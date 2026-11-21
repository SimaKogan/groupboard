package com.groupboard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration @EnableMethodSecurity
public class Security {
    @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }

    @Bean CorsConfigurationSource cors(@Value("${app.cors.origin}") String origin) {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(origin)); c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE")); c.setAllowedHeaders(List.of("*"));
        var s = new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/api/**", c); return s;
    }

    @Bean SecurityFilterChain chain(HttpSecurity http, Jwt jwt) throws Exception {
        var filter = new OncePerRequestFilter() {
            protected void doFilterInternal(HttpServletRequest rq, HttpServletResponse rs, FilterChain fc) throws IOException, jakarta.servlet.ServletException {
                var h = rq.getHeader("Authorization");
                if (h != null && h.startsWith("Bearer ")) {
                    try {
                        var c = jwt.read(h.substring(7));
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                            c.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + c.get("role")))));
                    } catch (Exception ignored) { /* invalid token: stay anonymous */ }
                }
                fc.doFilter(rq, rs);
            }
        };
        return http.csrf(c -> c.disable()).cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
