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
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepositories userRepositories;
    @Autowired
    private SessionRepository sessionRepository;

    public AuthService(SessionRepository sessionRepository, UserRepositories userRepositories, PasswordEncoder passwordEncoder) {
        this.sessionRepository = sessionRepository;
        this.userRepositories = userRepositories;
        this.passwordEncoder = passwordEncoder;
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

        // String token = RandomStringUtils.randomAscii(10);

        // Implemented JWT Token
        MultiValueMap<String, String> claimsMap = new MultiValueMapAdapter<>(new HashMap<>());
        claimsMap.add("email", user.getEmail());
        claimsMap.add("password", user.getPassword());
        String jwsToken = Jwts.builder().claims(claimsMap).compact();

        MultiValueMapAdapter<String , String> map = new MultiValueMapAdapter<String, String>(new HashMap<>());
        map.add("AUTH_TOKEN", jwsToken);

        Session session = new Session();
        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setToken(jwsToken);
        session.setUser(user);
        sessionRepository.save(session);

        UserDto userDto = UserDto.from(user);
        ResponseEntity<UserDto> response = new ResponseEntity<>(
                userDto, map, HttpStatus.OK
        );
        return response;
    }

    public Optional<UserDto> validate(String token, Long UserId){
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
