package at.technikum.api;

import at.technikum.api.controller.SessionController;
import at.technikum.api.controller.UserController;
import at.technikum.api.entity.User;
import at.technikum.api.service.JwtService;
import at.technikum.api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {UserController.class, SessionController.class})
@WithMockUser
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void register_posts_to_users_collection() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content("""
                                {"username":"paperless-user","password":"secure-password"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string("User successfully registered!"));

        verify(userService).register("paperless-user", "secure-password");
    }

    @Test
    void login_posts_to_sessions_and_returns_token() throws Exception {
        User user = new User("paperless-user", "encoded-password");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("paperless-user")).thenReturn("signed-token");

        mockMvc.perform(post("/sessions")
                        .contentType("application/json")
                        .content("""
                                {"username":"paperless-user","password":"secure-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-token"));
    }
}
