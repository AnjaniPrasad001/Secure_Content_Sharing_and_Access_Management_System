package com.filevault.service;

import com.filevault.dto.chat.ChatMessageRequest;
import com.filevault.dto.chat.ChatMessageResponse;
import com.filevault.entity.*;
import com.filevault.repository.*;
import com.filevault.service.chat.AdminChatbotService;
import com.filevault.service.chat.PublicChatbotService;
import com.filevault.service.llm.LlmService;
import com.filevault.service.rag.DocumentChunk;
import com.filevault.service.rag.DocumentExtractorService;
import com.filevault.service.rag.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ChatbotServiceTest {

    @Autowired
    private PublicChatbotService publicChatbotService;

    @Autowired
    private AdminChatbotService adminChatbotService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FileLikeRepository fileLikeRepository;

    @Autowired
    private AdminSubscriptionRepository adminSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    private Admin testAdmin;
    private Category testCategory;
    private File testFile;

    @BeforeEach
    public void setUp() {
        testAdmin = Admin.builder()
                .firstName("Test")
                .lastName("Admin")
                .email("testadmin_" + System.currentTimeMillis() + "@example.com")
                .password("password123")
                .isActive(true)
                .build();
        testAdmin = adminRepository.save(testAdmin);

        testCategory = Category.builder()
                .name("Artificial Intelligence")
                .description("AI and Machine Learning Documents")
                .build();
        testCategory = categoryRepository.save(testCategory);

        testFile = File.builder()
                .fileName("test_ai_doc.txt")
                .originalFileName("AI_Introduction_Guide.txt")
                .filePath("uploads/test_ai_doc.txt")
                .fileSize(1024L)
                .fileType("text/plain")
                .accessType(FileAccessType.PUBLIC)
                .admin(testAdmin)
                .category(testCategory)
                .description("Introduction to Machine Learning, Deep Learning, and Neural Networks with Python and PyTorch.")
                .viewCount(25L)
                .uploadedAt(LocalDateTime.now())
                .build();
        testFile = fileRepository.save(testFile);

        vectorStoreService.indexFile(testFile);
    }

    @Test
    public void testPublicChatbotQuery() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Find recently uploaded machine learning content")
                .build();

        ChatMessageResponse response = publicChatbotService.processPublicQuery(request);

        assertNotNull(response);
        assertNotNull(response.getReply());
        assertFalse(response.getReply().isBlank());
        assertNotNull(response.getStructuredFiles());
        assertTrue(response.getStructuredFiles().stream()
                .anyMatch(f -> f.getOriginalFileName().contains("AI_Introduction_Guide")));
    }

    @Test
    public void testPublicChatbotRAGSearch() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Show documents related to Neural Networks")
                .build();

        ChatMessageResponse response = publicChatbotService.processPublicQuery(request);

        assertNotNull(response);
        assertNotNull(response.getReply());
        assertTrue(response.getReply().toLowerCase().contains("neural") || 
                   response.getStructuredFiles().size() > 0);
    }

    @Test
    public void testAdminChatbotAnalyticsQuery() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Give me analytics on my uploaded content")
                .build();

        ChatMessageResponse response = adminChatbotService.processAdminQuery(testAdmin.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("Total Files Uploaded") || 
                   response.getReply().contains("Analytics"));
    }

    @Test
    public void testAdminChatbotChartGeneration() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Plot daily views for the last 7 days")
                .build();

        ChatMessageResponse response = adminChatbotService.processAdminQuery(testAdmin.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
        assertEquals("Daily Engagement Trends (Last 7 Days)", response.getChartData().getTitle());
        assertEquals("line", response.getChartData().getChartType());
        assertNotNull(response.getChartData().getDataPoints());
    }

    @Test
    public void testAdminChatbotCategoryChart() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Show views by content category")
                .build();

        ChatMessageResponse response = adminChatbotService.processAdminQuery(testAdmin.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
        assertEquals("Views by Content Category", response.getChartData().getTitle());
        assertEquals("bar", response.getChartData().getChartType());
    }

    @Test
    public void testAdminChatbotUnauthorizedAccess() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .message("Show stats")
                .build();

        assertThrows(SecurityException.class, () -> {
            adminChatbotService.processAdminQuery(null, request);
        });
    }

    @Test
    public void testVectorStoreIndexingAndSearch() {
        List<DocumentChunk> chunks = vectorStoreService.searchPublicChunks("PyTorch", 5);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(c -> c.getFileId().equals(testFile.getId())));

        vectorStoreService.removeFileIndex(testFile.getId());
        List<DocumentChunk> remainingChunks = vectorStoreService.searchPublicChunks("PyTorch", 5);
        assertFalse(remainingChunks.stream().anyMatch(c -> c.getFileId().equals(testFile.getId())));
    }
}
