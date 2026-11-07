package com.smartparcelguard.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.security.JwtService;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveUpdateBroker extends TextWebSocketHandler {

    static final String USER_ID = "userId";
    static final String ROLE = "role";
    static final String PARCELS = "parcels";
    static final String DASHBOARD = "dashboard";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public LiveUpdateBroker(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public boolean authenticate(String token, Map<String, Object> attributes) {
        if (token == null || token.isBlank() || !jwtService.isValid(token)) {
            return false;
        }
        String username = jwtService.parse(token).getSubject();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.isEnabled()) {
            return false;
        }
        attributes.put(USER_ID, user.getId());
        attributes.put(ROLE, user.getRole());
        attributes.put(PARCELS, ConcurrentHashMap.newKeySet());
        attributes.put(DASHBOARD, false);
        return true;
    }

    public boolean mayReceive(UserRole role, Long userId, Parcel parcel) {
        if (role == UserRole.ADMIN) {
            return true;
        }
        return parcel.getOperator() != null && parcel.getOperator().getId().equals(userId);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ClientCommand command = objectMapper.readValue(message.getPayload(), ClientCommand.class);
        if ("dashboard".equals(command.action())) {
            session.getAttributes().put(DASHBOARD, true);
            return;
        }
        if (command.parcelId() == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Set<Long> parcels = (Set<Long>) session.getAttributes().get(PARCELS);
        if ("subscribe".equals(command.action())) {
            parcels.add(command.parcelId());
        } else if ("unsubscribe".equals(command.action())) {
            parcels.remove(command.parcelId());
        }
    }

    public void publish(Parcel parcel, String type, Object data) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "parcelId", parcel.getId(),
                    "data", data == null ? Map.of() : data
            ));
        } catch (IOException exception) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen() || !visibleTo(session, parcel)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Set<Long> parcels = (Set<Long>) session.getAttributes().get(PARCELS);
            boolean dashboard = Boolean.TRUE.equals(session.getAttributes().get(DASHBOARD));
            boolean subscribed = parcels != null && parcels.contains(parcel.getId());
            if (!dashboard && !subscribed) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException exception) {
                sessions.remove(session);
            }
        }
    }

    private boolean visibleTo(WebSocketSession session, Parcel parcel) {
        Object role = session.getAttributes().get(ROLE);
        Object userId = session.getAttributes().get(USER_ID);
        if (!(role instanceof UserRole userRole) || !(userId instanceof Long id)) {
            return false;
        }
        return mayReceive(userRole, id, parcel);
    }

    public record ClientCommand(String action, Long parcelId) {
    }
}
