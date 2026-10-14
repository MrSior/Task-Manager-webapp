package com.taskmanager.service;

import com.taskmanager.domain.User;
import com.taskmanager.repository.SessionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionLookupService {

    private final SessionRepository sessionRepository;

    public SessionLookupService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByToken(String token) {
        return sessionRepository.findByToken(token).map(session -> session.getUser());
    }
}
