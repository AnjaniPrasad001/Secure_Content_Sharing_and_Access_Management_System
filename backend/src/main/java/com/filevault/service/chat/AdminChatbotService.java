package com.filevault.service.chat;

import com.filevault.dto.chat.ChartDataDto;
import com.filevault.dto.chat.ChatMessageRequest;
import com.filevault.dto.chat.ChatMessageResponse;
import com.filevault.dto.chat.FileResultDto;
import com.filevault.entity.File;
import com.filevault.repository.*;
import com.filevault.service.llm.LlmService;
import com.filevault.service.rag.DocumentChunk;
import com.filevault.service.rag.VectorStoreService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminChatbotService {

    private final FileRepository fileRepository;
    private final FileViewEventRepository fileViewEventRepository;
    private final FileLikeRepository fileLikeRepository;
    private final AdminSubscriptionRepository adminSubscriptionRepository;
    private final VectorStoreService vectorStoreService;
    private final LlmService llmService;

    public AdminChatbotService(FileRepository fileRepository,
                               FileViewEventRepository fileViewEventRepository,
                               FileLikeRepository fileLikeRepository,
                               AdminSubscriptionRepository adminSubscriptionRepository,
                               VectorStoreService vectorStoreService,
                               LlmService llmService) {
        this.fileRepository = fileRepository;
        this.fileViewEventRepository = fileViewEventRepository;
        this.fileLikeRepository = fileLikeRepository;
        this.adminSubscriptionRepository = adminSubscriptionRepository;
        this.vectorStoreService = vectorStoreService;
        this.llmService = llmService;
    }

    public ChatMessageResponse processAdminQuery(Long adminId, ChatMessageRequest request) {
        if (adminId == null) {
            throw new SecurityException("Unauthorized: Admin identity is missing");
        }

        String query = request.getMessage() != null ? request.getMessage().trim() : "";
        String lowerQuery = query.toLowerCase();

        // Check if chart requested
        boolean isChartQuery = lowerQuery.contains("graph") || lowerQuery.contains("plot") || lowerQuery.contains("chart") || lowerQuery.contains("trend") || lowerQuery.contains("visual");
        boolean isCategoryQuery = lowerQuery.contains("category") || lowerQuery.contains("categories");
        boolean isLowestQuery = lowerQuery.contains("lowest") || lowerQuery.contains("least") || lowerQuery.contains("poor");

        // Aggregated Metrics
        Long totalFiles = fileRepository.countByAdminId(adminId);
        Long totalViews = fileRepository.sumViewCountByAdminId(adminId);
        Long totalLikes = fileLikeRepository.countTotalLikesByAdminId(adminId);
        Long totalSubscribers = adminSubscriptionRepository.countByAdminId(adminId);

        List<File> adminFiles = fileRepository.findByAdminId(adminId);
        List<File> topFiles = fileRepository.findTopAdminFilesByViews(adminId);
        List<File> lowestFiles = fileRepository.findLowestAdminFilesByViews(adminId);

        ChartDataDto chartData = null;
        if (isChartQuery || isCategoryQuery) {
            chartData = buildAdminChartData(adminId, isCategoryQuery);
        }

        // RAG for Admin's own documents
        List<DocumentChunk> adminChunks = vectorStoreService.searchAdminChunks(query, adminId, 5);

        List<FileResultDto> fileDtos = (isLowestQuery ? lowestFiles : topFiles).stream()
                .limit(5)
                .map(this::mapToFileResultDto)
                .collect(Collectors.toList());

        // System prompt for LLM
        StringBuilder adminContext = new StringBuilder();
        adminContext.append("Admin Dashboard Overview Metrics:\n")
                .append("- Total Uploaded Files: ").append(totalFiles).append("\n")
                .append("- Total File Views: ").append(totalViews).append("\n")
                .append("- Total Likes: ").append(totalLikes).append("\n")
                .append("- Total Subscribers: ").append(totalSubscribers).append("\n\n");

        if (!adminFiles.isEmpty()) {
            adminContext.append("Top Performing Content:\n");
            for (int i = 0; i < Math.min(3, topFiles.size()); i++) {
                File f = topFiles.get(i);
                adminContext.append("- ").append(f.getOriginalFileName()).append(" (Views: ").append(f.getViewCount()).append(", Category: ").append(f.getCategory().getName()).append(")\n");
            }
        }

        if (!adminChunks.isEmpty()) {
            adminContext.append("\nAdmin Document Search Results:\n");
            for (DocumentChunk chunk : adminChunks) {
                adminContext.append("- File ").append(chunk.getOriginalFileName()).append(": \"").append(chunk.getText()).append("\"\n");
            }
        }

        String systemPrompt = "You are the FileVault Admin AI Analytics & Content Assistant.\n" +
                "You provide verified metrics, historical trend analyses, engagement recommendations, and document summaries strictly for this authenticated Admin (ID: " + adminId + ").\n" +
                "Never expose passwords, tokens, or other admins' data. Distinguish data-backed facts from recommendations.\n\n" +
                adminContext.toString();

        String llmReply = llmService.generateResponse(systemPrompt, query);

        if (llmReply == null) {
            llmReply = generateFallbackAdminReply(query, totalFiles, totalViews, totalLikes, totalSubscribers, topFiles, lowestFiles, adminChunks);
        }

        return ChatMessageResponse.builder()
                .reply(llmReply)
                .structuredFiles(fileDtos)
                .chartData(chartData)
                .llmActive(llmService.isConfigured())
                .build();
    }

    private ChartDataDto buildAdminChartData(Long adminId, boolean categoryChart) {
        if (categoryChart) {
            List<Object[]> catCounts = fileViewEventRepository.countViewsByCategoryForAdmin(adminId);
            List<Map<String, Object>> dataPoints = new ArrayList<>();
            for (Object[] row : catCounts) {
                Map<String, Object> point = new HashMap<>();
                point.put("category", row[0] != null ? row[0].toString() : "Uncategorized");
                point.put("views", ((Number) row[1]).longValue());
                dataPoints.add(point);
            }
            if (dataPoints.isEmpty()) {
                // Generate sample fallback points from current files if events are fresh
                List<File> files = fileRepository.findByAdminId(adminId);
                Map<String, Long> catMap = files.stream().collect(Collectors.groupingBy(f -> f.getCategory().getName(), Collectors.summingLong(File::getViewCount)));
                catMap.forEach((cat, views) -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("category", cat);
                    point.put("views", views);
                    dataPoints.add(point);
                });
            }
            return ChartDataDto.builder()
                    .title("Views by Content Category")
                    .chartType("bar")
                    .xAxisKey("category")
                    .seriesKeys(Collections.singletonList("views"))
                    .dataPoints(dataPoints)
                    .build();
        } else {
            // Daily views time-series graph for last 7 days
            List<Map<String, Object>> dataPoints = new ArrayList<>();
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");

            for (int i = 6; i >= 0; i--) {
                LocalDate day = today.minusDays(i);
                LocalDateTime start = day.atStartOfDay();
                LocalDateTime end = day.atTime(LocalTime.MAX);

                Long dayViews = fileViewEventRepository.countByAdminIdAndViewedAtBetween(adminId, start, end);
                Long dayLikes = fileLikeRepository.countLikesByAdminIdAndLikedAtBetween(adminId, start, end);
                Long daySubs = adminSubscriptionRepository.countSubscriptionsByAdminIdAndSubscribedAtBetween(adminId, start, end);

                Map<String, Object> point = new HashMap<>();
                point.put("date", day.format(fmt));
                point.put("views", dayViews);
                point.put("likes", dayLikes);
                point.put("subscribers", daySubs);
                dataPoints.add(point);
            }

            return ChartDataDto.builder()
                    .title("Daily Engagement Trends (Last 7 Days)")
                    .chartType("line")
                    .xAxisKey("date")
                    .seriesKeys(Arrays.asList("views", "likes", "subscribers"))
                    .dataPoints(dataPoints)
                    .build();
        }
    }

    private String generateFallbackAdminReply(String query, Long totalFiles, Long totalViews, Long totalLikes, Long totalSubscribers, List<File> topFiles, List<File> lowestFiles, List<DocumentChunk> adminChunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Admin Analytics Overview\n")
                .append("- **Total Files Uploaded:** ").append(totalFiles).append("\n")
                .append("- **Total Content Views:** ").append(totalViews).append("\n")
                .append("- **Total Likes Received:** ").append(totalLikes).append("\n")
                .append("- **Subscribers:** ").append(totalSubscribers).append("\n\n");

        if (!topFiles.isEmpty()) {
            sb.append("**Top Performing Upload:** ").append(topFiles.get(0).getOriginalFileName())
                    .append(" (").append(topFiles.get(0).getViewCount()).append(" views)\n");
        }

        if (!adminChunks.isEmpty()) {
            sb.append("\n**Document Search Summary:**\n");
            for (DocumentChunk chunk : adminChunks) {
                sb.append("• ").append(chunk.getOriginalFileName()).append(": ").append(chunk.getText().substring(0, Math.min(100, chunk.getText().length()))).append("...\n");
            }
        }

        return sb.toString();
    }

    private FileResultDto mapToFileResultDto(File f) {
        Long likes = fileLikeRepository.countByFileId(f.getId());
        return FileResultDto.builder()
                .id(f.getId())
                .fileName(f.getFileName())
                .originalFileName(f.getOriginalFileName())
                .fileSize(f.getFileSize())
                .fileType(f.getFileType())
                .accessType(f.getAccessType().name())
                .categoryName(f.getCategory() != null ? f.getCategory().getName() : "")
                .adminId(f.getAdmin().getId())
                .adminName(f.getAdmin().getFirstName() + " " + f.getAdmin().getLastName())
                .price(f.getPrice())
                .description(f.getDescription())
                .viewCount(f.getViewCount())
                .likesCount(likes)
                .uploadedAt(f.getUploadedAt())
                .build();
    }
}
