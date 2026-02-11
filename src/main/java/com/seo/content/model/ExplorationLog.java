package com.seo.content.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exploration_logs",
       indexes = {
           @Index(name = "idx_execution_date", columnList = "execution_date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "niche_id", nullable = false)
    private Niche niche;
    
    @Column(name = "execution_date", nullable = false)
    private LocalDateTime executionDate;
    
    @Column(name = "exploration_strategy", columnDefinition = "TEXT")
    private String explorationStrategy;
    
    @Column(name = "current_max_depth_level")
    private Integer currentMaxDepthLevel;
    
    @Column(name = "keywords_discovered")
    private Integer keywordsDiscovered;
    
    @Column(name = "keywords_qualified")
    private Integer keywordsQualified;
    
    @Column(name = "articles_generated")
    private Integer articlesGenerated;
    
    @Column(name = "llm_notes", columnDefinition = "TEXT")
    private String llmNotes;
    
    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;
    
    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        if (executionDate == null) {
            executionDate = LocalDateTime.now();
        }
    }
}
