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
@Table(name = "niches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Niche {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "niche_name", nullable = false, unique = true)
    private String nicheName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "seed_keywords", columnDefinition = "TEXT")
    private String seedKeywords;
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @OneToMany(mappedBy = "niche", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PotentialKeyword> potentialKeywords = new ArrayList<>();
    
    @OneToMany(mappedBy = "niche", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExplorationLog> explorationLogs = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
    
    public List<String> getSeedKeywordsAsList() {
        if (seedKeywords == null || seedKeywords.trim().isEmpty()) {
            return List.of();
        }
        return List.of(seedKeywords.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
