package com.scaler.userservice.security.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.scaler.userservice.models.Role;
import com.scaler.userservice.models.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonDeserialize
@NoArgsConstructor
@Getter
@Setter
public class CustomUserDetails implements UserDetails {
    private User user;

    private List<GrantedAuthority> authorities;
    private String password;
    private String username;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
    private long userId;


    public CustomUserDetails(User user){
        authorities = new ArrayList<>();
        for(Role role : user.getRoles()){
            authorities.add(new CustomGrantedAuthority(role));
        }
        this.password = user.getPassword();
        this.username = user.getEmail();
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
        //this.user = user;
        this.userId = user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        grantedAuthorities = new ArrayList<>();
//        for(Role role : user.getRoles()){
//            grantedAuthorities.add(new CustomGrantedAuthority(role));
//        }
        return this.authorities;
    }

    @Override
    public String getPassword() {
        //return user.getPassword();
        //Doing this change to get the jackson converting the json object without error
        return this.password;
    }

    @Override
    public String getUsername() {
        //return user.getEmail();
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        //return true;
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        //return true;
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        //return true;
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        //return true;
        return this.enabled;
    }
}
