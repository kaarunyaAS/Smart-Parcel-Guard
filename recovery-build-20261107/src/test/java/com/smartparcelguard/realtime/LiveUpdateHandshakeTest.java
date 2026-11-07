package com.smartparcelguard.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.config.SmartParcelGuardProperties;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.security.JwtService;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LiveUpdateHandshakeTest {

    @LocalServerPort
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private SmartParcelGuardProperties properties;
    @Autowired private LiveUpdateBroker broker;
    @Autowired private ObjectMapper objectMapper;

    private final List<User> createdUsers = new ArrayList<>();
    private final List<WebSocket> sockets = new ArrayList<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    void cleanUp() {
        sockets.forEach(socket -> socket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete"));
        userRepository.deleteAll(createdUsers);
    }

    @Test
    void rejectsInvalidExpiredAndDisabledCredentials() throws Exception {
        User user = createUser(UserRole.OPERATOR, true);
        assertHandshakeRejected("not-a-jwt");
        assertHandshakeRejected(expiredToken(user));

        User disabled = createUser(UserRole.OPERATOR, false);
        assertHandshakeRejected(jwtService.generateToken(disabled.getUsername(), disabled.getRole().name(), disabled.getId()));
    }

    @Test
    void operatorReceivesOnlyUpdatesForAssignedParcels() throws Exception {
        User assignedOperator = createUser(UserRole.OPERATOR, true);
        User otherOperator = createUser(UserRole.OPERATOR, true);
        WebSocketListener assignedListener = new WebSocketListener();
        WebSocketListener otherListener = new WebSocketListener();
        WebSocket assignedSocket = connect(jwtService.generateToken(
                assignedOperator.getUsername(), assignedOperator.getRole().name(), assignedOperator.getId()), assignedListener);
        WebSocket otherSocket = connect(jwtService.generateToken(
                otherOperator.getUsername(), otherOperator.getRole().name(), otherOperator.getId()), otherListener);
        sockets.add(assignedSocket);
        sockets.add(otherSocket);

        String subscribe = objectMapper.writeValueAsString(Map.of("action", "subscribe", "parcelId", 42));
        assignedSocket.sendText(subscribe, true).get(5, TimeUnit.SECONDS);
        otherSocket.sendText(subscribe, true).get(5, TimeUnit.SECONDS);

        Parcel parcel = new Parcel();
        parcel.setId(42L);
        parcel.setOperator(assignedOperator);

        String authorizedMessage = null;
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (authorizedMessage == null && System.nanoTime() < deadline) {
            broker.publish(parcel, "TEST", Map.of("value", "visible"));
            authorizedMessage = assignedListener.messages.poll(100, TimeUnit.MILLISECONDS);
        }

        assertTrue(authorizedMessage != null, "The assigned operator did not receive the parcel update");
        assertTrue(authorizedMessage.contains("\"value\":\"visible\""));
        assertFalse(otherListener.messages.poll(100, TimeUnit.MILLISECONDS) != null,
                "An operator received an update for a parcel assigned to another operator");
    }

    private WebSocket connect(String token, WebSocket.Listener listener) throws Exception {
        URI uri = URI.create("ws://localhost:" + port + "/ws?access_token=" + token);
        return httpClient.newWebSocketBuilder().buildAsync(uri, listener).get(5, TimeUnit.SECONDS);
    }

    private void assertHandshakeRejected(String token) {
        URI uri = URI.create("ws://localhost:" + port + "/ws?access_token=" + token);
        CompletableFuture<WebSocket> handshake = httpClient.newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() { });
        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> handshake.get(5, TimeUnit.SECONDS));
        assertTrue(failure.getCause() instanceof WebSocketHandshakeException
                        || failure.getCause() instanceof java.io.IOException,
                "Expected an HTTP/WebSocket handshake rejection");
    }

    private User createUser(UserRole role, boolean enabled) {
        String name = "ws-" + UUID.randomUUID().toString().substring(0, 12);
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.com");
        user.setFullName(name);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPasswordHash("not-used-by-handshake-test");
        createdUsers.add(userRepository.saveAndFlush(user));
        return user;
    }

    private String expiredToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(key)
                .compact();
    }

    private static final class WebSocketListener implements WebSocket.Listener {
        private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messages.add(data.toString());
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }
    }
}
