package com.filevault.repository;

import com.filevault.entity.File;
import com.filevault.entity.FileAccessType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByAdminId(Long adminId);
    List<File> findByCategoryId(Long categoryId);
    List<File> findByAccessType(FileAccessType accessType);
    List<File> findByAdminIdAndAccessType(Long adminId, FileAccessType accessType);
    
    @Query("SELECT f FROM File f WHERE f.accessType = com.filevault.entity.FileAccessType.PUBLIC OR f.accessType = com.filevault.entity.FileAccessType.RESTRICTED")
    List<File> findAllPublicAndRestrictedFiles();
    
    @Query("SELECT f FROM File f WHERE f.admin.id = :adminId AND f.category.id = :categoryId")
    List<File> findByAdminAndCategory(@Param("adminId") Long adminId, @Param("categoryId") Long categoryId);
    
    @Query("SELECT f FROM File f ORDER BY f.id DESC")
    List<File> findAllFiles();

    @Query("SELECT f FROM File f WHERE (f.accessType = com.filevault.entity.FileAccessType.PUBLIC OR f.accessType = com.filevault.entity.FileAccessType.RESTRICTED) AND (LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.category.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<File> searchPublicFiles(@Param("query") String query);

    @Query("SELECT f FROM File f WHERE f.admin.id = :adminId AND (LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.category.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<File> searchAdminFiles(@Param("adminId") Long adminId, @Param("query") String query);

    @Query("SELECT f FROM File f WHERE f.accessType = com.filevault.entity.FileAccessType.PUBLIC ORDER BY f.uploadedAt DESC")
    List<File> findRecentPublicFiles();

    @Query("SELECT f FROM File f WHERE f.admin.id = :adminId ORDER BY f.uploadedAt DESC")
    List<File> findRecentAdminFiles(@Param("adminId") Long adminId);

    @Query("SELECT f FROM File f WHERE f.accessType = com.filevault.entity.FileAccessType.PUBLIC ORDER BY f.viewCount DESC")
    List<File> findTopPublicFilesByViews();

    @Query("SELECT f FROM File f WHERE f.admin.id = :adminId ORDER BY f.viewCount DESC")
    List<File> findTopAdminFilesByViews(@Param("adminId") Long adminId);

    @Query("SELECT f FROM File f WHERE f.admin.id = :adminId ORDER BY f.viewCount ASC")
    List<File> findLowestAdminFilesByViews(@Param("adminId") Long adminId);

    Long countByAdminId(Long adminId);

    @Query("SELECT COALESCE(SUM(f.viewCount), 0) FROM File f WHERE f.admin.id = :adminId")
    Long sumViewCountByAdminId(@Param("adminId") Long adminId);
}
