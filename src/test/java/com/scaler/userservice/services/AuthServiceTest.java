package com.scaler.userservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.userservice.Repositories.SessionRepository;
import com.scaler.userservice.Repositories.UserRepositories;
import com.scaler.userservice.models.Session;
import com.scaler.userservice.models.SessionStatus;
import com.scaler.userservice.models.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    // A 256-bit test key, Base64-encoded the way USERSERVICE_JWT_SECRET is.
    private static final byte[] KEY_BYTES = "test-key-that-is-32-bytes-long!!".getBytes(StandardCharsets.UTF_8);
    private static final String SECRET = Base64.getEncoder().encodeToString(KEY_BYTES);
    private static final SecretKey KEY = Keys.hmacShaKeyFor(KEY_BYTES);

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private UserRepositories userRepositories;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(sessionRepository, userRepositories, passwordEncoder, SECRET);

        user = new User();
        user.setId(7L);
        user.setEmail("piyush@example.com");
        user.setPassword(passwordEncoder.encode("correct-password"));
        when(userRepositories.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepositories.findById(user.getId())).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void login_token_does_not_contain_the_password_hash() throws Exception {
        String token = login();

        assertThat(payloadOf(token)).doesNotContainKey("password");
        assertThat(token).doesNotContain(user.getPassword());
    }

    @Test
    void login_token_is_signed_and_expires() throws Exception {
        String token = login();

        String[] parts = token.split("\\.", -1);
        assertThat(parts).hasSize(3);
        assertThat(parts[2]).as("signature").isNotEmpty();
        Map<String, Object> payload = payloadOf(token);
        long lifetimeSeconds = ((Number) payload.get("exp")).longValue() - ((Number) payload.get("iat")).longValue();
        assertThat(lifetimeSeconds).isEqualTo(AuthService.TOKEN_VALIDITY.toSeconds());
        assertThat(payload).containsEntry("sub", "7");
    }

    @Test
    void validate_accepts_a_token_from_login_while_its_session_is_active() throws Exception {
        String token = login();
        givenActiveSession(token);

        assertThat(authService.validate(token, 7L)).isPresent();
    }

    @Test
    void validate_rejects_a_token_signed_with_another_key() {
        SecretKey otherKey = Keys.hmacShaKeyFor("some-other-key-also-32-bytes-ok!".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder().subject("7").expiration(inMinutes(60)).signWith(otherKey).compact();
        givenActiveSession(forged);

        assertThat(authService.validate(forged, 7L)).isEmpty();
    }

    @Test
    void validate_rejects_an_expired_token() {
        String expired = Jwts.builder().subject("7").expiration(inMinutes(-1)).signWith(KEY).compact();
        givenActiveSession(expired);

        assertThat(authService.validate(expired, 7L)).isEmpty();
    }

    @Test
    void logout_ends_the_session_so_its_token_no_longer_validates() throws Exception {
        String token = login();
        Session session = givenActiveSession(token);

        assertThat(authService.logout(token, 7L)).isTrue();

        assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.ENDED);
        assertThat(authService.validate(token, 7L)).isEmpty();
    }

    @Test
    void logout_of_an_unknown_or_ended_session_changes_nothing() throws Exception {
        assertThat(authService.logout("no-such-token", 7L)).isFalse();

        String token = login();
        givenActiveSession(token).setSessionStatus(SessionStatus.ENDED);
        assertThat(authService.logout(token, 7L)).isFalse();

        verify(sessionRepository, times(1)).save(any(Session.class)); // only login's
    }

    private String login() throws Exception {
        return authService.login(user.getEmail(), "correct-password").getHeaders().getFirst("AUTH_TOKEN");
    }

    private Session givenActiveSession(String token) {
        Session session = new Session();
        session.setToken(token);
        session.setUser(user);
        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setExpiryAt(inMinutes(60));
        when(sessionRepository.findSessionByTokenAndUser_Id(token, user.getId())).thenReturn(Optional.of(session));
        return session;
    }

    private static Date inMinutes(long minutes) {
        return new Date(System.currentTimeMillis() + minutes * 60_000);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payloadOf(String token) throws Exception {
        byte[] json = Base64.getUrlDecoder().decode(token.split("\\.")[1]);
        return new ObjectMapper().readValue(json, Map.class);
    }
}
