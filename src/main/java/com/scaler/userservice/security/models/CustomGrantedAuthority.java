package com.scaler.userservice.security.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.scaler.userservice.models.Role;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@JsonDeserialize
@NoArgsConstructor
public class CustomGrantedAuthority implements GrantedAuthority {
    //Making this change for the jackson to create an object of the methods starting with get.
    //private Role role;
    private String authority;

    public CustomGrantedAuthority(Role role) {
        this.authority = role.getRole();
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }
}
