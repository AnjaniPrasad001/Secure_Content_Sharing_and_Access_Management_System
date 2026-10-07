package com.filevault.service.chat;

import com.filevault.dto.chat.ChatMessageRequest;
import com.filevault.dto.chat.ChatMessageResponse;
import com.filevault.dto.chat.FileResultDto;
import com.filevault.entity.File;
import com.filevault.entity.FileAccessType;
import com.filevault.repository.FileLikeRepository;
import com.filevault.repository.FileRepository;
import com.filevault.service.llm.LlmService;
import com.filevault.service.rag.DocumentChunk;
import com.filevault.service.rag.VectorStoreService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PublicChatbotService {

    private final FileRepository fileRepository;
    private final FileLikeRepository fileLikeRepository;
    private final VectorStoreService vectorStoreService;
    private final LlmService llmService;

    public PublicChatbotService(FileRepository fileRepository,
                                FileLikeRepository fileLikeRepository,
                                VectorStoreService vectorStoreService,
                                LlmService llmService) {
        this.fileRepository = fileRepository;
        this.fileLikeRepository = fileLikeRepository;
        this.vectorStoreService = vectorStoreService;
        this.llmService = llmService;
    }

    public ChatMessageResponse processPublicQuery(ChatMessageRequest request) {
        String query = request.getMessage() != null ? request.getMessage().trim() : "";
        if (query.isEmpty()) {
            return ChatMessageResponse.builder()
                    .reply("Please enter a question or search request.")
                    .structuredFiles(Collections.emptyList())
                    .llmActive(llmService.isConfigured())
                    .build();
        }

        String lowerQuery = query.toLowerCase();
        List<File> matchedFiles = new ArrayList<>();

        // Intent detection: Recent uploads vs Search vs Recommendations vs Document RAG
        boolean isRecentQuery = lowerQuery.contains("recent") || lowerQuery.contains("latest") || lowerQuery.contains("newly uploaded") || lowerQuery.contains("today") || lowerQuery.contains("this week");
        boolean isTopQuery = lowerQuery.contains("popular") || lowerQuery.contains("top") || lowerQuery.contains("most viewed") || lowerQuery.contains("best");
        boolean isRecommendQuery = lowerQuery.contains("recommend") || lowerQuery.contains("suggest") || lowerQuery.contains("what should i learn");

        if (isRecentQuery) {
            matchedFiles = fileRepository.findRecentPublicFiles();
        } else if (isTopQuery) {
            matchedFiles = fileRepository.findTopPublicFilesByViews();
        } else if (isRecommendQuery) {
            matchedFiles = fileRepository.findAllPublicAndRestrictedFiles();
        } else {
            // Natural language keyword search
            matchedFiles = fileRepository.searchPublicFiles(query);
            if (matchedFiles.isEmpty()) {
                matchedFiles = fileRepository.findAllPublicAndRestrictedFiles();
            }
        }

        // RAG Context Retrieval
        List<DocumentChunk> ragChunks = vectorStoreService.searchPublicChunks(query, 5);

        // Convert files to DTO
        List<FileResultDto> fileDtos = matchedFiles.stream()
                .limit(6)
                .map(this::mapToFileResultDto)
                .collect(Collectors.toList());

        // System prompt for LLM
        StringBuilder ragContext = new StringBuilder();
        if (!ragChunks.isEmpty()) {
            ragContext.append("Relevant Document Excerpts:\n");
            for (DocumentChunk chunk : ragChunks) {
                ragContext.append("- File: ").append(chunk.getOriginalFileName())
                        .append(" (Category: ").append(chunk.getCategoryName()).append("):\n\"")
                        .append(chunk.getText()).append("\"\n\n");
            }
        }

        StringBuilder availableCatalog = new StringBuilder("Available Public Content Files:\n");
        for (FileResultDto file : fileDtos) {
            availableCatalog.append("- ").append(file.getOriginalFileName())
                    .append(" | Category: ").append(file.getCategoryName())
                    .append(" | Views: ").append(file.getViewCount())
                    .append(" | Description: ").append(file.getDescription() != null ? file.getDescription() : "N/A")
                    .append("\n");
        }

        String systemPrompt = "You are the FileVault Public Discovery Assistant. Help users discover, understand, and get recommendations for public content.\n" +
                "Ground all your answers strictly in the provided document excerpts and catalog files.\n" +
                "Never invent non-existent files or dates. Be helpful, concise, and professional.\n\n" +
                availableCatalog.toString() + "\n" + ragContext.toString();

        String llmReply = llmService.generateResponse(systemPrompt, query);

        if (llmReply == null) {
            // Fallback response generator if LLM key is not present or failed
            llmReply = generateFallbackReply(query, fileDtos, ragChunks, isRecentQuery, isRecommendQuery);
        }

        return ChatMessageResponse.builder()
                .reply(llmReply)
                .structuredFiles(fileDtos)
                .llmActive(llmService.isConfigured())
                .build();
    }

    private String generateFallbackReply(String query, List<FileResultDto> files, List<DocumentChunk> ragChunks, boolean isRecent, boolean isRecommend) {
        StringBuilder sb = new StringBuilder();
        if (isRecent) {
            sb.append("Here are the most recently uploaded public documents in FileVault:\n");
        } else if (isRecommend) {
            sb.append("Based on your request, here are recommended resources from our collection:\n");
        } else if (!ragChunks.isEmpty()) {
            sb.append("Found relevant details in document contents:\n");
            for (DocumentChunk chunk : ragChunks) {
                sb.append("• From **").append(chunk.getOriginalFileName()).append("**: \"")
                        .append(chunk.getText().substring(0, Math.min(120, chunk.getText().length()))).append("...\"\n");
            }
            sb.append("\nBelow are matching public files:\n");
        } else {
            sb.append("Found ").append(files.size()).append(" public file(s) matching your query:\n");
        }

        if (files.isEmpty()) {
            sb.append("No matching public files found. Try searching for topics like Java, CyberSecurity, or Machine Learning.");
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
