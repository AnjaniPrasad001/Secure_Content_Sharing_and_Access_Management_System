package com.filevault.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataDto {
    private String title;
    private String chartType; // "line", "bar", "pie"
    private String xAxisKey;
    private List<String> seriesKeys;
    private List<Map<String, Object>> dataPoints;
}
