package uk.creswick.luumo.pireporting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private String filename;
    private String title;
    private String date;
    private LocalDateTime lastModified;
    private String type; // "html" or "md"
    private String description;
    
    public boolean isMarkdown() {
        return "md".equalsIgnoreCase(type);
    }
    
    public boolean isHtml() {
        return "html".equalsIgnoreCase(type);
    }
}
