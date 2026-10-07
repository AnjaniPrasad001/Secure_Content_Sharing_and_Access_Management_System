package com.filevault.repository;

import com.filevault.entity.FileLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileLikeRepository extends JpaRepository<FileLike, Long> {

    Optional<FileLike> findByFileIdAndUserId(Long fileId, Long userId);

    boolean existsByFileIdAndUserId(Long fileId, Long userId);

    Long countByFileId(Long fileId);

    @Query("SELECT COUNT(l) FROM FileLike l WHERE l.file.admin.id = :adminId")
    Long countTotalLikesByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(l) FROM FileLike l WHERE l.file.admin.id = :adminId AND l.likedAt BETWEEN :start AND :end")
    Long countLikesByAdminIdAndLikedAtBetween(@Param("adminId") Long adminId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<FileLike> findByFileAdminIdAndLikedAtBetween(Long adminId, LocalDateTime start, LocalDateTime end);
}
