package at.technikum.api.controller;

import at.technikum.api.dto.AuthDTO;
import at.technikum.api.entity.User;
import at.technikum.api.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user.getUsername())));
    }
}