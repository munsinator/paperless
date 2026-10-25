package at.technikum.api.service;

import at.technikum.api.entity.User;
import at.technikum.api.exceptions.UsernameAlreadyExistsException;
import at.technikum.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        log.debug("Loading user: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User lookup failed, unknown username: {}", username);
                    return new UsernameNotFoundException("Invalid username or password");
                });
    }

    @Transactional
    public User register(String username, String rawPassword) {
        String normalizedUsername = username.trim();
        log.debug("Registering user: {}", normalizedUsername);
        if (userRepository.existsByUsername(normalizedUsername)) {
            log.warn("Registration rejected, username already registered: {}", normalizedUsername);
            throw new UsernameAlreadyExistsException("Username is already registered");
        }

        User savedUser = userRepository.save(new User(normalizedUsername, passwordEncoder.encode(rawPassword)));
        log.info("Registered user: {}", savedUser.getUsername());
        return savedUser;
    }
}
