package com.taskmanager.repository;

import com.taskmanager.domain.Session;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.token = :token")
    Optional<Session> findByToken(@Param("token") String token);

    void deleteByToken(String token);
}
