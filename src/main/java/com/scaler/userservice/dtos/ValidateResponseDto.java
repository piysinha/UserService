package com.scaler.userservice.dtos;

import com.scaler.userservice.models.SessionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateResponseDto {
    private UserDto userDto;
    private SessionStatus sessionStatus;
}
