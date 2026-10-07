package com.filevault.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String reply;
    private List<FileResultDto> structuredFiles;
    private ChartDataDto chartData;
    private boolean llmActive;
}
