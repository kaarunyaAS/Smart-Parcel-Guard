package com.smartparcelguard.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.security.JwtService;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveUpdateBrokerTest {

    @Test
    void authenticatesOnlyValidTokensForEnabledUsers() {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        LiveUpdateBroker broker = new LiveUpdateBroker(jwtService, userRepository, new ObjectMapper());
        var attributes = new HashMap<String, Object>();

        assertFalse(broker.authenticate(null, attributes));

        when(jwtService.isValid("expired")).thenReturn(false);
        assertFalse(broker.authenticate("expired", attributes));

        when(jwtService.isValid("valid")).thenReturn(true);
        when(jwtService.parse("valid")).thenReturn(Jwts.claims().subject("operator").build());
        User user = new User();
        user.setId(7L);
        user.setRole(UserRole.OPERATOR);
        user.setEnabled(false);
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(user));
        assertFalse(broker.authenticate("valid", attributes));

        user.setEnabled(true);
        assertTrue(broker.authenticate("valid", attributes));
        assertTrue(attributes.get(LiveUpdateBroker.USER_ID).equals(7L));
        assertTrue(attributes.get(LiveUpdateBroker.ROLE) == UserRole.OPERATOR);
    }

    @Test
    void operatorsReceiveOnlyAssignedParcels() {
        LiveUpdateBroker broker = new LiveUpdateBroker(null, null, null);
        User operator = new User();
        operator.setId(7L);
        Parcel assigned = new Parcel();
        assigned.setOperator(operator);
        Parcel other = new Parcel();
        User stranger = new User();
        stranger.setId(8L);
        other.setOperator(stranger);

        assertTrue(broker.mayReceive(UserRole.ADMIN, 1L, other));
        assertTrue(broker.mayReceive(UserRole.OPERATOR, 7L, assigned));
        assertFalse(broker.mayReceive(UserRole.OPERATOR, 7L, other));
        assertFalse(broker.mayReceive(UserRole.OPERATOR, 7L, new Parcel()));
    }
}
