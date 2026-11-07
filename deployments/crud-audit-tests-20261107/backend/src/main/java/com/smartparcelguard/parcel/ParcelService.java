package com.smartparcelguard.parcel;

import com.smartparcelguard.common.PageResponse;
import com.smartparcelguard.common.TextSanitizer;
import com.smartparcelguard.device.DeviceService;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.parcel.dto.ParcelRequest;
import com.smartparcelguard.parcel.dto.ParcelResponse;
import com.smartparcelguard.parcel.dto.ParcelStatusHistoryResponse;
import com.smartparcelguard.parcel.dto.ParcelStatusRequest;
import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class ParcelService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "parcelCode", "name", "status", "priority", "expectedDeliveryDate");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParcelRepository parcelRepository;
    private final ParcelStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final DeviceAssignmentRepository assignmentRepository;
    private final DeviceService deviceService;

    public ParcelService(ParcelRepository parcelRepository,
                         ParcelStatusHistoryRepository historyRepository,
                         UserRepository userRepository,
                         DeviceAssignmentRepository assignmentRepository,
                         DeviceService deviceService) {
        this.parcelRepository = parcelRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.deviceService = deviceService;
    }

    @Transactional
    public ParcelResponse create(ParcelRequest request, User actor) {
        Parcel parcel = new Parcel();
        parcel.setParcelCode(nextCode());
        parcel.setStatus(ParcelStatus.REGISTERED);
        parcel.setMonitoringState(MonitoringState.INACTIVE);
        parcel.setCreatedBy(actor);
        applyDetails(parcel, request);
        parcelRepository.save(parcel);
        recordStatus(parcel, null, ParcelStatus.REGISTERED, actor, "Parcel registered");
        if (request.deviceId() != null) {
            deviceService.assign(request.deviceId(), parcel.getId());
        }
        return toResponse(parcel);
    }

    @Transactional(readOnly = true)
    public PageResponse<ParcelResponse> list(User actor, ParcelStatus status, String query, int page, int size, String sort) {
        Long operatorId = actor.getRole() == UserRole.ADMIN ? null : actor.getId();
        Page<ParcelResponse> parcels = parcelRepository.search(status, operatorId, likeQuery(query), pageRequest(page, size, sort))
                .map(this::toResponse);
        return PageResponse.from(parcels);
    }

    @Transactional(readOnly = true)
    public ParcelResponse get(Long id, User actor) {
        return toResponse(requireVisible(id, actor));
    }

    @Transactional(readOnly = true)
    public Parcel requireVisibleParcel(Long id, User actor) {
        return requireVisible(id, actor);
    }

    @Transactional
    public ParcelResponse update(Long id, ParcelRequest request) {
        Parcel parcel = requireParcel(id);
        applyDetails(parcel, request);
        if (request.deviceId() != null) {
            deviceService.assign(request.deviceId(), parcel.getId());
        }
        return toResponse(parcel);
    }

    @Transactional
    public ParcelResponse updateStatus(Long id, ParcelStatusRequest request, User actor) {
        Parcel parcel = requireVisible(id, actor);
        if (parcel.getStatus() == request.status()) {
            throw new ApiException(HttpStatus.CONFLICT, "Parcel is already " + request.status());
        }
        if (actor.getRole() != UserRole.ADMIN && !ParcelStatusRules.operatorCanChange(parcel.getStatus(), request.status())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Operators cannot change status from " + parcel.getStatus() + " to " + request.status());
        }
        ParcelStatus previous = parcel.getStatus();
        parcel.setStatus(request.status());
        recordStatus(parcel, previous, request.status(), actor, TextSanitizer.clean(request.note()));
        return toResponse(parcel);
    }

    @Transactional(readOnly = true)
    public List<ParcelStatusHistoryResponse> history(Long id, User actor) {
        requireVisible(id, actor);
        return historyRepository.findByParcelIdOrderByChangedAtAsc(id).stream()
                .map(entry -> new ParcelStatusHistoryResponse(
                        entry.getId(),
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        entry.getChangedBy() == null ? null : entry.getChangedBy().getUsername(),
                        entry.getNote(),
                        entry.getChangedAt()
                ))
                .toList();
    }

    private void applyDetails(Parcel parcel, ParcelRequest request) {
        requireCoordinatePair(request.pickupLatitude(), request.pickupLongitude(), "Pickup");
        requireCoordinatePair(request.destinationLatitude(), request.destinationLongitude(), "Destination");
        parcel.setName(TextSanitizer.required(request.name(), "Name"));
        parcel.setDescription(TextSanitizer.clean(request.description()));
        parcel.setCategory(TextSanitizer.required(request.category(), "Category"));
        parcel.setDeclaredValue(request.declaredValue());
        parcel.setSenderName(TextSanitizer.required(request.senderName(), "Sender name"));
        parcel.setSenderPhone(TextSanitizer.clean(request.senderPhone()));
        parcel.setSenderAddress(TextSanitizer.clean(request.senderAddress()));
        parcel.setReceiverName(TextSanitizer.required(request.receiverName(), "Receiver name"));
        parcel.setReceiverPhone(TextSanitizer.clean(request.receiverPhone()));
        parcel.setReceiverAddress(TextSanitizer.clean(request.receiverAddress()));
        parcel.setPickupAddress(TextSanitizer.required(request.pickupAddress(), "Pickup address"));
        parcel.setPickupLatitude(request.pickupLatitude());
        parcel.setPickupLongitude(request.pickupLongitude());
        parcel.setDestinationAddress(TextSanitizer.required(request.destinationAddress(), "Destination address"));
        parcel.setDestinationLatitude(request.destinationLatitude());
        parcel.setDestinationLongitude(request.destinationLongitude());
        parcel.setExpectedDeliveryDate(request.expectedDeliveryDate());
        parcel.setPriority(request.priority());
        parcel.setOperator(resolveOperator(request.operatorId()));
    }

    private User resolveOperator(Long operatorId) {
        if (operatorId == null) {
            return null;
        }
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operator not found"));
        if (operator.getRole() != UserRole.OPERATOR || !operator.isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "Parcels can only be assigned to an enabled operator");
        }
        return operator;
    }

    private void requireCoordinatePair(BigDecimal latitude, BigDecimal longitude, String label) {
        if ((latitude == null) != (longitude == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, label + " latitude and longitude must both be provided");
        }
    }

    private void recordStatus(Parcel parcel, ParcelStatus from, ParcelStatus to, User actor, String note) {
        ParcelStatusHistory history = new ParcelStatusHistory();
        history.setParcel(parcel);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actor);
        history.setNote(note);
        history.setChangedAt(Instant.now());
        historyRepository.save(history);
    }

    private Parcel requireVisible(Long id, User actor) {
        Parcel parcel = requireParcel(id);
        if (actor.getRole() == UserRole.ADMIN) {
            return parcel;
        }
        if (parcel.getOperator() == null || !parcel.getOperator().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Parcel not found");
        }
        return parcel;
    }

    private Parcel requireParcel(Long id) {
        return parcelRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parcel not found"));
    }

    private ParcelResponse toResponse(Parcel parcel) {
        DeviceAssignment assignment = assignmentRepository.findByParcelIdAndActiveDeviceKeyIsNotNull(parcel.getId())
                .stream()
                .findFirst()
                .orElse(null);
        return new ParcelResponse(
                parcel.getId(),
                parcel.getParcelCode(),
                parcel.getName(),
                parcel.getDescription(),
                parcel.getCategory(),
                parcel.getDeclaredValue(),
                parcel.getSenderName(),
                parcel.getSenderPhone(),
                parcel.getSenderAddress(),
                parcel.getReceiverName(),
                parcel.getReceiverPhone(),
                parcel.getReceiverAddress(),
                parcel.getPickupAddress(),
                parcel.getPickupLatitude(),
                parcel.getPickupLongitude(),
                parcel.getDestinationAddress(),
                parcel.getDestinationLatitude(),
                parcel.getDestinationLongitude(),
                parcel.getExpectedDeliveryDate(),
                parcel.getPriority(),
                parcel.getStatus(),
                parcel.getMonitoringState(),
                parcel.getOperator() == null ? null : parcel.getOperator().getId(),
                parcel.getOperator() == null ? null : parcel.getOperator().getUsername(),
                assignment == null ? null : assignment.getDevice().getId(),
                assignment == null ? null : assignment.getDevice().getDeviceCode(),
                parcel.getLatestRiskScore(),
                parcel.getLatestRiskLevel(),
                parcel.getCreatedAt()
        );
    }

    private String nextCode() {
        String date = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "SPG-P-" + date + "-" + random(6);
            if (parcelRepository.findByParcelCode(code).isEmpty()) {
                return code;
            }
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Could not generate a unique parcel code");
    }

    private String random(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return value.toString();
    }

    private String likeQuery(String query) {
        String cleaned = TextSanitizer.clean(query);
        if (cleaned == null) {
            return null;
        }
        return cleaned.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private PageRequest pageRequest(int page, int size, String sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        String[] parts = sort == null ? new String[]{"createdAt", "desc"} : sort.split(",");
        if (!SORT_FIELDS.contains(parts[0])) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(direction, parts[0]));
    }
}
