package com.scaler.userservice.Repositories;

import com.scaler.userservice.models.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findSessionByTokenAndUser_Id(String token, Long userId);
}
