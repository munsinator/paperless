package at.technikum.api.controller;

import at.technikum.api.dto.AuthDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthDTO dto) {
        // TODO implement logic
        return new ResponseEntity<>("User successfully registered!", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTO dto) {
        // TODO implement logic
        return new ResponseEntity<>("User successfully logged in!", HttpStatus.OK);
    }
}
