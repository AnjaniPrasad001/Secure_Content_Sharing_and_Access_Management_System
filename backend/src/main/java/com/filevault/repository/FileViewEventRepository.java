package com.filevault.repository;

import com.filevault.entity.FileViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileViewEventRepository extends JpaRepository<FileViewEvent, Long> {

    List<FileViewEvent> findByFileAdminIdAndViewedAtBetween(Long adminId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(v) FROM FileViewEvent v WHERE v.file.admin.id = :adminId AND v.viewedAt BETWEEN :start AND :end")
    Long countByAdminIdAndViewedAtBetween(@Param("adminId") Long adminId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT v.userId) FROM FileViewEvent v WHERE v.file.admin.id = :adminId AND v.userId IS NOT NULL AND v.viewedAt BETWEEN :start AND :end")
    Long countUniqueViewersByAdminIdAndViewedAtBetween(@Param("adminId") Long adminId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT v.file.category.name, COUNT(v) FROM FileViewEvent v WHERE v.file.admin.id = :adminId GROUP BY v.file.category.name")
    List<Object[]> countViewsByCategoryForAdmin(@Param("adminId") Long adminId);
}
