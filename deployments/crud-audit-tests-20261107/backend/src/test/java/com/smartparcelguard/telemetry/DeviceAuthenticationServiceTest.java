package com.smartparcelguard.telemetry;

import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.device.DeviceRepository;
import com.smartparcelguard.device.DeviceTokenHasher;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.parcel.Parcel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceAuthenticationServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceAssignmentRepository assignmentRepository;

    @InjectMocks
    private DeviceAuthenticationService service;

    @Test
    void acceptsAnEnabledAssignedDeviceWithoutPersistingTheRawToken() {
        Device device = device();
        device.setEnabled(true);
        Parcel parcel = new Parcel();
        parcel.setId(9L);
        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setParcel(parcel);
        when(deviceRepository.findByTokenHash(DeviceTokenHasher.sha256("raw-token"))).thenReturn(Optional.of(device));
        when(assignmentRepository.findByActiveDeviceKey(4L)).thenReturn(Optional.of(assignment));

        String storedHash = device.getTokenHash();
        AuthenticatedDevice authenticated = service.authenticate("raw-token");

        assertEquals(4L, authenticated.device().getId());
        assertEquals(9L, authenticated.parcel().getId());
        assertEquals(storedHash, device.getTokenHash());
        assertNotEquals("raw-token", device.getTokenHash());
    }

    @Test
    void rejectsMissingInvalidRevokedAndUnassignedTokens() {
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.authenticate("  "));

        when(deviceRepository.findByTokenHash(DeviceTokenHasher.sha256("missing"))).thenReturn(Optional.empty());
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.authenticate("missing"));

        Device revoked = device();
        revoked.setEnabled(false);
        when(deviceRepository.findByTokenHash(DeviceTokenHasher.sha256("revoked"))).thenReturn(Optional.of(revoked));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.authenticate("revoked"));

        Device unassigned = device();
        unassigned.setEnabled(true);
        when(deviceRepository.findByTokenHash(DeviceTokenHasher.sha256("unassigned"))).thenReturn(Optional.of(unassigned));
        when(assignmentRepository.findByActiveDeviceKey(4L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.CONFLICT, () -> service.authenticate("unassigned"));
    }

    private static Device device() {
        Device device = new Device();
        device.setId(4L);
        device.setTokenHash(DeviceTokenHasher.sha256("stored-only-as-hash"));
        return device;
    }

    private static void assertStatus(HttpStatus status, Runnable action) {
        ApiException exception = assertThrows(ApiException.class, action::run);
        assertEquals(status, exception.getStatus());
    }
}
