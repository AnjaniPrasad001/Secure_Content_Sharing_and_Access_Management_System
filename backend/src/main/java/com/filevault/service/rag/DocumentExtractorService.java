package com.filevault.service.rag;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Service
public class DocumentExtractorService {

    public String extractText(File file, String fileType) {
        if (file == null || !file.exists() || !file.canRead()) {
            return "";
        }
        try {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv") ||
                name.endsWith(".json") || name.endsWith(".java") || name.endsWith(".py") ||
                name.endsWith(".xml") || name.endsWith(".html") || name.endsWith(".js") ||
                name.endsWith(".ts") || name.endsWith(".css") || name.endsWith(".sql") ||
                (fileType != null && (fileType.contains("text") || fileType.contains("json")))) {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            
            // For general text or fallback files
            byte[] bytes = Files.readAllBytes(file.toPath());
            String text = new String(bytes, StandardCharsets.UTF_8);
            // Quick check if printable text
            long nonPrintable = text.chars().filter(c -> c < 9 || (c > 13 && c < 32)).count();
            if (nonPrintable < text.length() * 0.1) {
                return text;
            }
        } catch (Exception e) {
            System.err.println("Could not extract text from " + file.getName() + ": " + e.getMessage());
        }
        return "";
    }
}
