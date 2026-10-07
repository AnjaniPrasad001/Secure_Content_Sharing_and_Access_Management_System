package com.filevault.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    private Long fileId;
    private String fileName;
    private String originalFileName;
    private Long adminId;
    private String accessType; // PUBLIC, PRIVATE, RESTRICTED
    private String categoryName;
    private int chunkIndex;
    private String text;
    private float[] embedding;
}
