package com.seo.content.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "potential_keywords", 
       indexes = {
           @Index(name = "idx_keyword_text", columnList = "keyword_text"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_depth_level", columnList = "depth_level"),
           @Index(name = "idx_qualification_score", columnList = "qualification_score")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PotentialKeyword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "keyword_text", nullable = false, unique = true)
    private String keywordText;
    
    @Column(name = "depth_level", nullable = false)
    private Integer depthLevel;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_keyword_id")
    private PotentialKeyword parentKeyword;
    
    @OneToMany(mappedBy = "parentKeyword", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PotentialKeyword> childKeywords = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "niche_id", nullable = false)
    private Niche niche;
    
    @Column(name = "qualification_score")
    private Double qualificationScore;
    
    @Column(name = "qualification_reasoning", columnDefinition = "TEXT")
    private String qualificationReasoning;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private KeywordStatus status = KeywordStatus.UNWRITTEN;
    
    @Column(name = "discovered_date", nullable = false)
    private LocalDateTime discoveredDate;
    
    @Column(name = "written_date")
    private LocalDateTime writtenDate;
    
    @OneToOne(mappedBy = "keyword", cascade = CascadeType.ALL, orphanRemoval = true)
    private Article article;
    
    @PrePersist
    protected void onCreate() {
        if (discoveredDate == null) {
            discoveredDate = LocalDateTime.now();
        }
    }
    
    public boolean isQualified() {
        return qualificationScore != null && qualificationScore >= 5.0;
    }
    
    public String getParentKeywordText() {
        return parentKeyword != null ? parentKeyword.getKeywordText() : null;
    }
}
