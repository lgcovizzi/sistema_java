package com.example.sistemajava.token;

import com.example.sistemajava.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByUser(User user);
    long deleteByExpiresAtBefore(Instant instant);
}


