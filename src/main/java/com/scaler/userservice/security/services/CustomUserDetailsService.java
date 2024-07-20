package com.scaler.userservice.security.services;

import com.scaler.userservice.Repositories.UserRepositories;
import com.scaler.userservice.models.User;
import com.scaler.userservice.security.models.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private UserRepositories userRepositories;

    public CustomUserDetailsService(UserRepositories userRepositories) {
        this.userRepositories = userRepositories;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepositories.findByEmail(username);
        if(user.isEmpty()){
            throw new UsernameNotFoundException(username+" Username does not exist");
        }
        return new CustomUserDetails(user.get());
    }
}
