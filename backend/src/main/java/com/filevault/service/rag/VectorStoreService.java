package com.filevault.service.rag;

import com.filevault.entity.File;
import com.filevault.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorStoreService {

    private final FileRepository fileRepository;
    private final DocumentExtractorService extractorService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    // In-memory vector index mapped by File ID
    private final Map<Long, List<DocumentChunk>> vectorStore = new ConcurrentHashMap<>();

    public VectorStoreService(FileRepository fileRepository, DocumentExtractorService extractorService) {
        this.fileRepository = fileRepository;
        this.extractorService = extractorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            List<File> allFiles = fileRepository.findAll();
            for (File fileEntity : allFiles) {
                indexFile(fileEntity);
            }
            System.out.println("VectorStoreService initialized. Total files indexed: " + vectorStore.size());
        } catch (Exception e) {
            System.err.println("Error initializing VectorStoreService: " + e.getMessage());
        }
    }

    public synchronized void indexFile(File fileEntity) {
        if (fileEntity == null || fileEntity.getFilePath() == null) return;
        try {
            String text = "";
            java.io.File diskFile = Paths.get(fileEntity.getFilePath()).toFile();
            if (!diskFile.exists()) {
                diskFile = Paths.get(uploadDir, "admin_" + fileEntity.getAdmin().getId(), fileEntity.getFileName()).toFile();
            }
            if (diskFile.exists()) {
                text = extractorService.extractText(diskFile, fileEntity.getFileType());
            }
            if (text == null || text.isBlank()) {
                text = "File Name: " + fileEntity.getOriginalFileName() + 
                       ". Category: " + (fileEntity.getCategory() != null ? fileEntity.getCategory().getName() : "") + 
                       ". Description: " + (fileEntity.getDescription() != null ? fileEntity.getDescription() : "");
            }

            List<String> chunksText = chunkText(text, 500, 100);
            List<DocumentChunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunksText.size(); i++) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .fileId(fileEntity.getId())
                        .fileName(fileEntity.getFileName())
                        .originalFileName(fileEntity.getOriginalFileName())
                        .adminId(fileEntity.getAdmin().getId())
                        .accessType(fileEntity.getAccessType().name())
                        .categoryName(fileEntity.getCategory() != null ? fileEntity.getCategory().getName() : "")
                        .chunkIndex(i)
                        .text(chunksText.get(i))
                        .build();
                chunks.add(chunk);
            }
            vectorStore.put(fileEntity.getId(), chunks);
        } catch (Exception e) {
            System.err.println("Failed to index file " + fileEntity.getId() + ": " + e.getMessage());
        }
    }

    public synchronized void removeFileIndex(Long fileId) {
        vectorStore.remove(fileId);
    }

    public List<DocumentChunk> searchPublicChunks(String query, int maxResults) {
        return searchChunks(query, "PUBLIC", null, maxResults);
    }

    public List<DocumentChunk> searchAdminChunks(String query, Long adminId, int maxResults) {
        return searchChunks(query, null, adminId, maxResults);
    }

    private List<DocumentChunk> searchChunks(String query, String accessTypeRequired, Long adminIdRequired, int maxResults) {
        if (query == null || query.isBlank()) return Collections.emptyList();

        String[] queryTerms = query.toLowerCase().split("\\W+");
        Set<String> termSet = new HashSet<>(Arrays.asList(queryTerms));

        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (List<DocumentChunk> fileChunks : vectorStore.values()) {
            for (DocumentChunk chunk : fileChunks) {
                // Scope & Access Filter
                if (accessTypeRequired != null && !accessTypeRequired.equalsIgnoreCase(chunk.getAccessType())) {
                    if (!"RESTRICTED".equalsIgnoreCase(chunk.getAccessType()) && !"PUBLIC".equalsIgnoreCase(chunk.getAccessType())) {
                        continue;
                    }
                }
                if (adminIdRequired != null && !adminIdRequired.equals(chunk.getAdminId())) {
                    continue;
                }

                double score = computeMatchScore(chunk.getText().toLowerCase(), termSet);
                if (score > 0) {
                    scoredChunks.add(new ScoredChunk(chunk, score));
                }
            }
        }

        scoredChunks.sort((a, b) -> Double.compare(b.score, a.score));

        List<DocumentChunk> results = new ArrayList<>();
        for (int i = 0; i < Math.min(maxResults, scoredChunks.size()); i++) {
            results.add(scoredChunks.get(i).chunk);
        }
        return results;
    }

    private double computeMatchScore(String text, Set<String> queryTerms) {
        double matches = 0;
        for (String term : queryTerms) {
            if (term.length() > 2 && text.contains(term)) {
                matches += 1.0;
            }
        }
        return matches;
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(text.substring(start, end));
            if (end == length) break;
            start += (chunkSize - overlap);
        }
        return chunks;
    }

    private static class ScoredChunk {
        DocumentChunk chunk;
        double score;
        ScoredChunk(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
