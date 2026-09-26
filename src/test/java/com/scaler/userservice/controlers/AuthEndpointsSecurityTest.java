package com.scaler.userservice.controlers;

import com.scaler.userservice.dtos.UserDto;
import com.scaler.userservice.security.SecurityConfig;
import com.scaler.userservice.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Other services and clients call these endpoints with JSON, without a browser session or CSRF token.
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthEndpointsSecurityTest {

    private static final String CREDENTIALS = "{\"email\":\"piyush@example.com\",\"password\":\"correct-password\"}";
    private static final String TOKEN = "{\"userId\":7,\"token\":\"a-token\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private RegisteredClientRepository registeredClientRepository;

    @Test
    void a_client_can_sign_up() throws Exception {
        when(authService.signUp(any(), any())).thenReturn(new UserDto());

        mockMvc.perform(post("/auth/SignUp").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk());
    }

    @Test
    void a_client_can_log_in_without_already_being_logged_in() throws Exception {
        when(authService.login(any(), any())).thenReturn(new ResponseEntity<>(new UserDto(), HttpStatus.OK));

        mockMvc.perform(post("/auth/Login").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk());
    }

    @Test
    void another_service_can_validate_a_token() throws Exception {
        when(authService.validate(any(), any())).thenReturn(Optional.of(new UserDto()));

        mockMvc.perform(post("/auth/Validate").contentType(MediaType.APPLICATION_JSON).content(TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void a_client_can_log_out_with_its_token() throws Exception {
        when(authService.logout(any(), any())).thenReturn(true);

        mockMvc.perform(post("/auth/Logout").contentType(MediaType.APPLICATION_JSON).content(TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void logging_out_an_unknown_session_is_not_found() throws Exception {
        when(authService.logout(any(), any())).thenReturn(false);

        mockMvc.perform(post("/auth/Logout").contentType(MediaType.APPLICATION_JSON).content(TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void everything_else_still_needs_a_login() throws Exception {
        mockMvc.perform(get("/somewhere-else"))
                .andExpect(status().is3xxRedirection());
    }
}
