package com.filevault.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_view_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileViewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "viewer_ip", nullable = true)
    private String viewerIp;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime viewedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }
}
