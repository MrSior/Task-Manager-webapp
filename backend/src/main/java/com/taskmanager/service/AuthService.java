package com.taskmanager.service;

import com.taskmanager.domain.Session;
import com.taskmanager.domain.User;
import com.taskmanager.dto.AuthRequest;
import com.taskmanager.dto.AuthResponse;
import com.taskmanager.metrics.AppMetrics;
import com.taskmanager.repository.SessionRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.PasswordEncoder;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppMetrics appMetrics;

    public AuthService(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            AppMetrics appMetrics) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.appMetrics = appMetrics;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String login = request.getLogin().trim();
        if (userRepository.existsByLogin(login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login already taken");
        }

        User user = new User();
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        appMetrics.incAuthRegistrations();
        return createSession(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByLogin(request.getLogin().trim()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            appMetrics.incAuthFailedLogins();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        appMetrics.incAuthLogins();
        return createSession(user);
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessionRepository.deleteByToken(token);
        }
    }

    private AuthResponse createSession(User user) {
        Session session = new Session();
        session.setToken(UUID.randomUUID().toString().replace("-", ""));
        session.setUser(user);
        sessionRepository.save(session);
        return new AuthResponse(session.getToken(), user.getLogin());
    }
}
