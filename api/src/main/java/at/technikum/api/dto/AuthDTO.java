package at.technikum.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthDTO {

    @NotBlank(message = "Username cannot be empty")
    @jakarta.validation.constraints.Pattern(regexp = "(?U)\\S+", message = "Username must not contain whitespace")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters long")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
