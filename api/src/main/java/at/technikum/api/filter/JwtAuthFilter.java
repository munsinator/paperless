package at.technikum.api.filter;

import at.technikum.api.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT aus HTTP Anfrage lesen und Benutzer als authentifiziert markieren
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Token aus Header lesen
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        // Token ist leer oder Benutzer ist bereits authentifiziert
        if (token.isEmpty() || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Username aus Token extrahieren
            String username = jwtService.extractUsername(token);
            UserDetails user = userDetailsService.loadUserByUsername(username);
            // JWT Service prüft, ob Token gültig ist und zum geladenen Benutzer gehört
            if (jwtService.isTokenValid(token, user)) {
                // Authentifizierter User
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Authentifizierung im SecurityContext speichern
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("Rejected JWT for {} {}: token does not match user", request.getMethod(), request.getRequestURI());
            }
        } catch (UsernameNotFoundException exception) {
            log.warn("Rejected JWT for {} {}: user no longer exists", request.getMethod(), request.getRequestURI());
            SecurityContextHolder.clearContext();
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            log.warn("Rejected JWT for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
