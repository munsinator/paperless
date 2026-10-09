package at.technikum.api.controller;

import at.technikum.api.dto.AuthDTO;
import at.technikum.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<String> register(@Valid @RequestBody AuthDTO dto) {
        userService.register(dto.getUsername(), dto.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body("User successfully registered!");
    }
}
