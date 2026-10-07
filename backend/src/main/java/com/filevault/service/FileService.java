package com.filevault.service;

import com.filevault.dto.FileResponse;
import com.filevault.dto.FileDetailResponse;
import com.filevault.entity.*;
import com.filevault.exception.ResourceNotFoundException;
import com.filevault.exception.UnauthorizedAccessException;
import com.filevault.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@Transactional
public class FileService {
    
    @Autowired
    private FileRepository fileRepository;
    
    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private AccessControlRepository accessControlRepository;

    @Autowired(required = false)
    private com.filevault.service.rag.VectorStoreService vectorStoreService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @PostConstruct
    public void init() {
        Path uploadPath = Paths.get(uploadDir);
        log.info("FileService initialized with upload directory: {}", uploadPath.toAbsolutePath());
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory confirmed/created at: {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", e.getMessage());
        }
    }
    
    public File uploadFile(MultipartFile multipartFile, Long adminId, Long categoryId, 
                          FileAccessType accessType, String description, Double price) throws IOException {
        
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, "admin_" + adminId);
        Files.createDirectories(uploadPath);
        
        log.info("Upload directory path: {}", uploadPath.toAbsolutePath());
        
        // Generate unique file name
        String fileName = generateUniqueFileName(multipartFile.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);
        
        // Save file to disk
        Files.copy(multipartFile.getInputStream(), filePath);
        
        log.info("File saved to: {}", filePath.toAbsolutePath());
        
        // Create file entity - store absolute path
        File fileEntity = File.builder()
                .fileName(fileName)
                .originalFileName(multipartFile.getOriginalFilename())
                .filePath(filePath.toAbsolutePath().toString())
                .fileSize(multipartFile.getSize())
                .fileType(multipartFile.getContentType())
                .accessType(accessType)
                .admin(admin)
                .category(category)
                .description(description)
                .price(accessType == FileAccessType.RESTRICTED ? price : null)
                .build();
        
        File savedFile = fileRepository.save(fileEntity);
        if (vectorStoreService != null) {
            try { vectorStoreService.indexFile(savedFile); } catch (Exception ignored) {}
        }
        log.info("File entity saved with ID: {} and path: {}", savedFile.getId(), savedFile.getFilePath());
        return savedFile;
    }
    
    public File uploadFileWithCategoryName(MultipartFile multipartFile, Long adminId, String categoryName, 
                                          FileAccessType accessType, String description, Double price) throws IOException {
        
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        // Find or create category
        Optional<Category> existingCategory = categoryRepository.findByNameIgnoreCase(categoryName);
        Category category;
        
        if (existingCategory.isPresent()) {
            category = existingCategory.get();
        } else {
            // Create new category
            category = Category.builder()
                    .name(categoryName)
                    .description("Auto-created category: " + categoryName)
                    .build();
            category = categoryRepository.save(category);
        }
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, "admin_" + adminId);
        Files.createDirectories(uploadPath);
        
        log.info("Upload directory path: {}", uploadPath.toAbsolutePath());
        
        // Generate unique file name
        String fileName = generateUniqueFileName(multipartFile.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);
        
        // Save file to disk
        Files.copy(multipartFile.getInputStream(), filePath);
        
        log.info("File saved to: {}", filePath.toAbsolutePath());
        
        // Create file entity - store absolute path
        File fileEntity = File.builder()
                .fileName(fileName)
                .originalFileName(multipartFile.getOriginalFilename())
                .filePath(filePath.toAbsolutePath().toString())
                .fileSize(multipartFile.getSize())
                .fileType(multipartFile.getContentType())
                .accessType(accessType)
                .admin(admin)
                .category(category)
                .description(description)
                .price(accessType == FileAccessType.RESTRICTED ? price : null)
                .build();
        
        File savedFile = fileRepository.save(fileEntity);
        if (vectorStoreService != null) {
            try { vectorStoreService.indexFile(savedFile); } catch (Exception ignored) {}
        }
        log.info("File entity saved with ID: {} and path: {}", savedFile.getId(), savedFile.getFilePath());
        return savedFile;
    }
    
    public List<FileResponse> getFilesForAdmin(Long adminId) {
        List<File> files = fileRepository.findByAdminId(adminId);
        return files.stream()
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    public List<FileResponse> getPublicFiles() {
        List<File> files = fileRepository.findByAccessType(FileAccessType.PUBLIC);
        return files.stream()
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    public List<FileResponse> getAvailableFilesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        log.info("Fetching available files for user ID: {}", userId);
        List<File> allFiles = fileRepository.findAllPublicAndRestrictedFiles();
        log.info("Found {} public/restricted files for user {}", allFiles.size(), userId);
        
        return allFiles.stream()
                .map(file -> {
                    FileResponse response = convertToFileResponse(file);
                    log.info("Processing file: {} with access type: {}", file.getOriginalFileName(), file.getAccessType());
                    // Check if user has access to restricted file
                    if (file.getAccessType() == FileAccessType.RESTRICTED) {
                        Optional<AccessControl> access = accessControlRepository.findActiveAccess(file.getId(), userId);
                        response.setHasAccess(access.isPresent());
                    } else if (file.getAccessType() == FileAccessType.PUBLIC) {
                        response.setHasAccess(true);
                    } else {
                        response.setHasAccess(false);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    public FileResponse getFileById(Long fileId, Long userId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        
        FileResponse response = convertToFileResponse(file);
        
        // Check access for restricted files
        if (file.getAccessType() == FileAccessType.RESTRICTED && userId != null) {
            Optional<AccessControl> access = accessControlRepository.findActiveAccess(fileId, userId);
            response.setHasAccess(access.isPresent());
        } else if (file.getAccessType() == FileAccessType.PUBLIC) {
            response.setHasAccess(true);
        }
        
        return response;
    }
    
    public File deleteFile(Long fileId, Long adminId) throws IOException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        
        // Verify admin owns the file
        if (!file.getAdmin().getId().equals(adminId)) {
            throw new UnauthorizedAccessException("You don't have permission to delete this file");
        }
        
        // Delete from disk
        try {
            Files.deleteIfExists(Paths.get(file.getFilePath()));
        } catch (IOException e) {
            log.error("Error deleting file from disk: {}", e.getMessage());
        }
        
        // Delete from database
        fileRepository.delete(file);
        if (vectorStoreService != null) {
            try { vectorStoreService.removeFileIndex(fileId); } catch (Exception ignored) {}
        }
        return file;
    }
    
    public File getFileByIdForDownload(Long fileId, Long userId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        
        log.info("Retrieved file: {}, stored path: {}", file.getOriginalFileName(), file.getFilePath());
        
        // Verify access
        if (file.getAccessType() == FileAccessType.PRIVATE) {
            throw new UnauthorizedAccessException("This file is private");
        } else if (file.getAccessType() == FileAccessType.RESTRICTED) {
            if (userId == null) {
                throw new UnauthorizedAccessException("You need to be logged in to access this file");
            }
            Optional<AccessControl> access = accessControlRepository.findActiveAccess(fileId, userId);
            if (access.isEmpty()) {
                throw new UnauthorizedAccessException("You don't have access to this file");
            }
        }
        
        return file;
    }
    
    public List<File> getFilesByCategory(Long categoryId) {
        return fileRepository.findByCategoryId(categoryId);
    }
    
    public FileResponse convertToFileResponse(File file) {
        return FileResponse.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .originalFileName(file.getOriginalFileName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .accessType(file.getAccessType().toString())
                .categoryName(file.getCategory().getName())
                .adminId(file.getAdmin().getId())
                .adminName(file.getAdmin().getFirstName() + " " + file.getAdmin().getLastName())
                .price(file.getPrice())
                .description(file.getDescription())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
    
    private String generateUniqueFileName(String originalFileName) {
        long timestamp = System.currentTimeMillis();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }
    
    public List<FileDetailResponse> getPublicFilesSortedByViews() {
        List<File> files = fileRepository.findByAccessType(FileAccessType.PUBLIC);
        return files.stream()
                .sorted((f1, f2) -> f2.getViewCount().compareTo(f1.getViewCount()))
                .map(this::convertToFileDetailResponse)
                .collect(Collectors.toList());
    }
    
    public void incrementViewCount(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        file.setViewCount(file.getViewCount() + 1);
        fileRepository.save(file);
    }
    
    public FileDetailResponse convertToFileDetailResponse(File file) {
        return FileDetailResponse.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .originalFileName(file.getOriginalFileName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .accessType(file.getAccessType())
                .categoryName(file.getCategory().getName())
                .adminEmail(file.getAdmin().getEmail())
                .price(file.getPrice())
                .description(file.getDescription())
                .viewCount(file.getViewCount())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
    
    public Optional<File> getFileFromRepository(Long fileId) {
        return fileRepository.findById(fileId);
    }
    
    public List<File> getAllFilesDebug() {
        List<File> allFiles = fileRepository.findAll();
        log.info("DEBUG: Total files in database: {}", allFiles.size());
        for (File file : allFiles) {
            log.info("DEBUG: File ID: {}, Name: {}, Type: {}, AccessType: {}", 
                file.getId(), file.getOriginalFileName(), file.getFileType(), file.getAccessType());
        }
        return allFiles;
    }

    public File updateFile(Long fileId, Long adminId, Map<String, Object> updates) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (adminId != null && !file.getAdmin().getId().equals(adminId)) {
            throw new UnauthorizedAccessException("You don't have permission to update this file");
        }

        // Update description
        if (updates.containsKey("description")) {
            file.setDescription((String) updates.get("description"));
        }

        // Update access type
        if (updates.containsKey("accessType")) {
            String accessTypeStr = (String) updates.get("accessType");
            file.setAccessType(FileAccessType.valueOf(accessTypeStr.toUpperCase()));
        }

        // Update price (only for RESTRICTED files)
        if (updates.containsKey("price") && file.getAccessType() == FileAccessType.RESTRICTED) {
            Object priceObj = updates.get("price");
            if (priceObj != null) {
                if (priceObj instanceof Number) {
                    file.setPrice(((Number) priceObj).doubleValue());
                } else {
                    file.setPrice(Double.parseDouble(priceObj.toString()));
                }
            }
        }

        // Update category if provided
        if (updates.containsKey("categoryId")) {
            Object categoryIdObj = updates.get("categoryId");
            if (categoryIdObj != null) {
                Long categoryId = categoryIdObj instanceof Number 
                    ? ((Number) categoryIdObj).longValue() 
                    : Long.parseLong(categoryIdObj.toString());
                
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
                file.setCategory(category);
            }
        }

        file.setUpdatedAt(LocalDateTime.now());
        File updatedFile = fileRepository.save(file);
        if (vectorStoreService != null) {
            try { vectorStoreService.indexFile(updatedFile); } catch (Exception ignored) {}
        }
        return updatedFile;
    }
}
