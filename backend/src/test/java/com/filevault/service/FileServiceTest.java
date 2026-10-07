package com.filevault.service;

import com.filevault.dto.FileResponse;
import com.filevault.dto.RegisterRequest;
import com.filevault.entity.Admin;
import com.filevault.entity.Category;
import com.filevault.entity.File;
import com.filevault.entity.FileAccessType;
import com.filevault.repository.AdminRepository;
import com.filevault.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FileServiceTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testUploadAndRetrievePublicFile() throws Exception {
        // Register admin
        RegisterRequest adminReq = RegisterRequest.builder()
                .email("fileadmin@example.com")
                .password("Password123!")
                .firstName("File")
                .lastName("Admin")
                .phoneNumber("5551234567")
                .build();
        authService.registerAdmin(adminReq);
        Admin admin = adminRepository.findByEmail("fileadmin@example.com").orElseThrow();

        // Ensure category exists
        Category category = categoryRepository.save(Category.builder().name("TestCat").description("Test").build());

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World Content".getBytes()
        );

        File uploaded = fileService.uploadFile(
                mockFile,
                admin.getId(),
                category.getId(),
                FileAccessType.PUBLIC,
                "Test Public File",
                null
        );

        assertNotNull(uploaded.getId());
        assertEquals("test.txt", uploaded.getOriginalFileName());
        assertEquals(FileAccessType.PUBLIC, uploaded.getAccessType());

        List<FileResponse> publicFiles = fileService.getPublicFiles();
        assertTrue(publicFiles.stream().anyMatch(f -> f.getId().equals(uploaded.getId())));
    }
}
