package com.scaler.userservice.services;

import com.scaler.userservice.Repositories.SessionRepository;
import com.scaler.userservice.Repositories.UserRepositories;
import com.scaler.userservice.dtos.UserDto;
import com.scaler.userservice.exceptions.PasswordDoesNotMatchException;
import com.scaler.userservice.exceptions.UserAlreadyExistsException;
import com.scaler.userservice.exceptions.UserDoesNotExistException;
import com.scaler.userservice.models.Session;
import com.scaler.userservice.models.SessionStatus;
import com.scaler.userservice.models.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMapAdapter;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@Service
public class AuthService {
    // How long a login lasts: both the token's expiry and its session's.
    static final Duration TOKEN_VALIDITY = Duration.ofHours(24);

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepositories userRepositories;
    @Autowired
    private SessionRepository sessionRepository;

    private final SecretKey jwtKey;

    public AuthService(SessionRepository sessionRepository, UserRepositories userRepositories, PasswordEncoder passwordEncoder,
                       @Value("${userservice.jwt.secret}") String jwtSecret) {
        this.sessionRepository = sessionRepository;
        this.userRepositories = userRepositories;
        this.passwordEncoder = passwordEncoder;
        // A Base64-encoded key; hmacShaKeyFor refuses one shorter than 256 bits, so a weak key fails at startup.
        this.jwtKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public UserDto signUp(String email, String password) throws UserAlreadyExistsException {
        Optional<User> userOptional =userRepositories.findByEmail(email);
        if (userOptional.isPresent()) {
            throw new UserAlreadyExistsException("User with email : "+ email +" Already Exist Please use Login");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        User savedUser = userRepositories.save(user);
        return UserDto.from(savedUser);
    }

    public ResponseEntity<UserDto> login(String email, String password) throws UserDoesNotExistException, PasswordDoesNotMatchException {
        Optional<User> userOptional = userRepositories.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new UserDoesNotExistException("Email: "+email+" does not exist please use signup method");
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordDoesNotMatchException("Password does not match");
        }

        // A signed JWT that expires. It carries who the user is, never their password hash.
        Date issuedAt = new Date();
        Date expiresAt = Date.from(issuedAt.toInstant().plus(TOKEN_VALIDITY));
        String jwsToken = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(jwtKey)
                .compact();

        MultiValueMapAdapter<String , String> map = new MultiValueMapAdapter<String, String>(new HashMap<>());
        map.add("AUTH_TOKEN", jwsToken);

        Session session = new Session();
        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setToken(jwsToken);
        session.setExpiryAt(expiresAt);
        session.setUser(user);
        sessionRepository.save(session);

        UserDto userDto = UserDto.from(user);
        ResponseEntity<UserDto> response = new ResponseEntity<>(
                userDto, map, HttpStatus.OK
        );
        return response;
    }

    public Optional<UserDto> validate(String token, Long UserId){
        // Only tokens this service signed, and that haven't expired, can match a session.
        try {
            Jwts.parser().verifyWith(jwtKey).build().parseSignedClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }

        Optional<Session> sessionOptional = sessionRepository.findSessionByTokenAndUser_Id(token , UserId);
        if (sessionOptional.isEmpty()) {
            return Optional.empty();
        }
        Session session = sessionOptional.get();
        if(session.getSessionStatus() != SessionStatus.ACTIVE){
            return Optional.empty();
        }

        User user = userRepositories.findById(UserId).get();
        UserDto userDto = UserDto.from(user);

        return Optional.of(userDto);
    }
}
