package com.scaler.userservice.controlers;

import com.scaler.userservice.dtos.*;
import com.scaler.userservice.exceptions.PasswordDoesNotMatchException;
import com.scaler.userservice.exceptions.UserAlreadyExistsException;
import com.scaler.userservice.exceptions.UserDoesNotExistException;
import com.scaler.userservice.models.SessionStatus;
import com.scaler.userservice.models.User;
import com.scaler.userservice.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    public AuthController (AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/Login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequestDto requestDto) throws PasswordDoesNotMatchException, UserDoesNotExistException {
        return authService.login(requestDto.getEmail(), requestDto.getPassword());
    }

//    @PostMapping("/Logout")
//    public ResponseEntity<Void> logout (@RequestBody LogoutRequestDto requestDto){
//        return authService.logout(requestDto.getToken(),requestDto.getUserId());
//    }

    @PostMapping("/SignUp")
    public ResponseEntity<UserDto> signUp(@RequestBody SignUpRequestDto requestDto) throws UserAlreadyExistsException {
        UserDto userDto = authService.signUp(requestDto.getEmail(), requestDto.getPassword());
        return new ResponseEntity<>(userDto, HttpStatus.OK);
    }

    @PostMapping("/Validate")
    public ResponseEntity<ValidateResponseDto> validateToken (@RequestBody ValidateTokenRequestDto requestDto){
        Optional<UserDto> userDto = authService.validate(requestDto.getToken(),requestDto.getUserId());
        if(userDto.isEmpty()){
            ValidateResponseDto response = new ValidateResponseDto();
            response.setSessionStatus(SessionStatus.INVALID);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        ValidateResponseDto response = new ValidateResponseDto();
        response.setSessionStatus(SessionStatus.ACTIVE);
        response.setUserDto(userDto.get());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
