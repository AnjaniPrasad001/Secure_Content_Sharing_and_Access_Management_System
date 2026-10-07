package com.filevault.repository;

import com.filevault.entity.AdminSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminSubscriptionRepository extends JpaRepository<AdminSubscription, Long> {

    Optional<AdminSubscription> findByAdminIdAndUserId(Long adminId, Long userId);

    boolean existsByAdminIdAndUserId(Long adminId, Long userId);

    Long countByAdminId(Long adminId);

    @Query("SELECT COUNT(s) FROM AdminSubscription s WHERE s.admin.id = :adminId AND s.subscribedAt BETWEEN :start AND :end")
    Long countSubscriptionsByAdminIdAndSubscribedAtBetween(@Param("adminId") Long adminId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<AdminSubscription> findByAdminIdAndSubscribedAtBetween(Long adminId, LocalDateTime start, LocalDateTime end);
}
