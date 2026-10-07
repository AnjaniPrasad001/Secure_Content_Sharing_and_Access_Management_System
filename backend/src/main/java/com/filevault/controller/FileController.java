package com.filevault.controller;

import com.filevault.dto.FileResponse;
import com.filevault.dto.FileUploadRequest;
import com.filevault.entity.Admin;
import com.filevault.entity.File;
import com.filevault.entity.FileAccessType;
import com.filevault.repository.AdminRepository;
import com.filevault.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam("accessType") String accessType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) Double price) {
        
        try {
            // Get admin from authentication
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            
            // Find admin by email
            Admin admin = adminRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new Exception("Admin not found: " + adminEmail));
            
            Long adminId = admin.getId();
            
            FileAccessType accessTypeEnum = FileAccessType.valueOf(accessType.toUpperCase());
            
            // Use categoryId if provided, otherwise use categoryName
            File uploadedFile;
            if (categoryId != null) {
                uploadedFile = fileService.uploadFile(file, adminId, categoryId, accessTypeEnum, description, price);
            } else if (categoryName != null && !categoryName.isEmpty()) {
                uploadedFile = fileService.uploadFileWithCategoryName(file, adminId, categoryName, accessTypeEnum, description, price);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Either categoryId or categoryName must be provided");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("fileId", uploadedFile.getId());
            response.put("fileName", uploadedFile.getOriginalFileName());
            response.put("fileSize", uploadedFile.getFileSize());
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            log.error("File upload error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "File upload failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("File upload error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "File upload failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{fileId}")
    public ResponseEntity<?> getFileById(
            @PathVariable Long fileId,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            FileResponse fileResponse = fileService.getFileById(fileId, userId);
            return new ResponseEntity<>(fileResponse, HttpStatus.OK);
        } catch (Exception e) {
            log.error("File retrieval error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "File not found");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/public")
    public ResponseEntity<?> getPublicFiles() {
        try {
            List<FileResponse> files = fileService.getPublicFiles();
            return new ResponseEntity<>(files, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get public files error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve public files");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getFilesByCategory(@PathVariable Long categoryId) {
        try {
            List<File> files = fileService.getFilesByCategory(categoryId);
            return new ResponseEntity<>(files, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get files by category error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve files");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            Admin admin = adminRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new Exception("Admin not found: " + adminEmail));
            
            fileService.deleteFile(fileId, admin.getId());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "File deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            log.error("File deletion error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "File deletion failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("File deletion error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "File deletion failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{fileId}")
    public ResponseEntity<?> updateFile(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> updates) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth.getName();
            Admin admin = adminRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new Exception("Admin not found: " + adminEmail));

            File updatedFile = fileService.updateFile(fileId, admin.getId(), updates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File updated successfully");
            response.put("file", fileService.convertToFileResponse(updatedFile));
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("File update error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "File update failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long fileId,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            File file = fileService.getFileByIdForDownload(fileId, userId);
            Resource resource = new FileSystemResource(file.getFilePath());
            
            if (!resource.exists()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File not found on disk");
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("File download error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "File download failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/read/{fileId}")
    public ResponseEntity<?> readFile(
            @PathVariable Long fileId,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            File file = fileService.getFileByIdForDownload(fileId, userId);
            log.info("Reading file: {} at path: {}", file.getOriginalFileName(), file.getFilePath());
            
            Resource resource = new FileSystemResource(file.getFilePath());
            
            if (!resource.exists()) {
                log.error("File not found at path: {}", file.getFilePath());
                Map<String, String> error = new HashMap<>();
                error.put("error", "File not found on disk at path: " + file.getFilePath());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            // Increment view count for reading
            fileService.incrementViewCount(fileId);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + file.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("File read error: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "File read failed");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/public/sorted")
    public ResponseEntity<?> getPublicFilesSortedByViews() {
        try {
            var files = fileService.getPublicFilesSortedByViews();
            return new ResponseEntity<>(files, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get public files error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve public files");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{fileId}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long fileId) {
        try {
            fileService.incrementViewCount(fileId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "View count incremented");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("View count increment error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to increment view count");
            error.put("message", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/debug/{fileId}")
    public ResponseEntity<?> debugFile(@PathVariable Long fileId) {
        try {
            log.info("Debug requested for file ID: {}", fileId);
            
            // Get the file from repository directly
            java.util.Optional<File> fileOpt = fileService.getFileFromRepository(fileId);
            if (!fileOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File not found in database");
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            File file = fileOpt.get();
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("fileId", file.getId());
            debugInfo.put("fileName", file.getFileName());
            debugInfo.put("originalFileName", file.getOriginalFileName());
            debugInfo.put("fileType", file.getFileType());
            debugInfo.put("storedPath", file.getFilePath());
            
            java.io.File diskFile = new java.io.File(file.getFilePath());
            debugInfo.put("fileExists", diskFile.exists());
            debugInfo.put("absolutePath", diskFile.getAbsolutePath());
            debugInfo.put("isFile", diskFile.isFile());
            if (diskFile.exists()) {
                debugInfo.put("fileSize", diskFile.length());
                debugInfo.put("canRead", diskFile.canRead());
            }
            
            return new ResponseEntity<>(debugInfo, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Debug error: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
}

