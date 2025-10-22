package com.delight.account.repository;

import com.delight.account.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    
    @Query("SELECT s FROM Session s JOIN FETCH s.user JOIN FETCH s.account WHERE s.sessionId = :sessionId AND s.isActive = true")
    Optional<Session> findBySessionIdAndIsActiveTrue(@Param("sessionId") String sessionId);
    
    @Query("SELECT s FROM Session s JOIN FETCH s.user JOIN FETCH s.account WHERE s.jwtToken = :jwtToken AND s.isActive = true")
    Optional<Session> findByJwtTokenAndIsActiveTrue(@Param("jwtToken") String jwtToken);
    
    List<Session> findByUserIdAndIsActiveTrue(Long userId);
    
    List<Session> findByAccountIdAndIsActiveTrue(Long accountId);
    
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.sessionId = :sessionId")
    void deactivateBySessionId(@Param("sessionId") String sessionId);
    
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.user.id = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.expiresAt < :now")
    void deactivateExpiredSessions(@Param("now") OffsetDateTime now);
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId AND s.isActive = true")
    long countActiveSessionsByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Session s SET s.jwtToken = :jwtToken WHERE s.sessionId = :sessionId AND s.isActive = true")
    void updateJwtTokenBySessionId(@Param("sessionId") String sessionId, @Param("jwtToken") String jwtToken);
}