package com.filevault.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResultDto {
    private Long id;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private String accessType;
    private String categoryName;
    private Long adminId;
    private String adminName;
    private Double price;
    private String description;
    private Long viewCount;
    private Long likesCount;
    private LocalDateTime uploadedAt;
}
