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
    
    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH s.account " +
           "LEFT JOIN FETCH s.role r " +
           "LEFT JOIN FETCH r.rolePrivileges rp " +
           "LEFT JOIN FETCH rp.privilege " +
           "WHERE s.sessionId = :sessionId AND s.isActive = true")
    Optional<Session> findBySessionIdAndIsActiveTrue(@Param("sessionId") String sessionId);
    
    @Query("SELECT s FROM Session s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH s.account " +
           "LEFT JOIN FETCH s.role r " +
           "LEFT JOIN FETCH r.rolePrivileges rp " +
           "LEFT JOIN FETCH rp.privilege " +
           "WHERE s.jwtToken = :jwtToken AND s.isActive = true")
    Optional<Session> findByJwtTokenAndIsActiveTrue(@Param("jwtToken") String jwtToken);
    
    List<Session> findByUserIdAndIsActiveTrue(Long userId);
    
    List<Session> findByAccountIdAndIsActiveTrue(Long accountId);
    
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.account.id = :accountId AND s.clientType = :clientType AND s.isActive = true")
    List<Session> findByUserIdAndAccountIdAndClientTypeAndIsActiveTrue(
        @Param("userId") Long userId, 
        @Param("accountId") Long accountId, 
        @Param("clientType") com.delight.account.model.ClientType clientType);
    
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.sessionId = :sessionId")
    void deactivateBySessionId(@Param("sessionId") String sessionId);
    
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.user.id = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.user.id = :userId AND s.account.id = :accountId AND s.clientType = :clientType AND s.isActive = true")
    void deactivateByUserIdAndAccountIdAndClientType(
        @Param("userId") Long userId, 
        @Param("accountId") Long accountId, 
        @Param("clientType") com.delight.account.model.ClientType clientType);
    
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.expiresAt < :now")
    void deactivateExpiredSessions(@Param("now") OffsetDateTime now);
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId AND s.isActive = true")
    long countActiveSessionsByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Session s SET s.jwtToken = :jwtToken WHERE s.sessionId = :sessionId AND s.isActive = true")
    void updateJwtTokenBySessionId(@Param("sessionId") String sessionId, @Param("jwtToken") String jwtToken);
    
    /**
     * Get the maximum last accessed time for each user/account combination.
     * Used for displaying last active time in Team Members list.
     */
    @Query("SELECT s.user.id as userId, s.account.id as accountId, MAX(s.lastAccessedAt) as lastActiveAt " +
           "FROM Session s " +
           "WHERE s.account.id = :accountId AND s.isActive = true " +
           "GROUP BY s.user.id, s.account.id")
    List<Object[]> findLastActiveTimesByAccountId(@Param("accountId") Long accountId);
}