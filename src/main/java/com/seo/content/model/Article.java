package com.seo.content.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles",
       indexes = {
           @Index(name = "idx_created_date", columnList = "created_date"),
           @Index(name = "idx_published_date", columnList = "published_date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false, unique = true)
    private PotentialKeyword keyword;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "niche_id", nullable = false)
    private Niche niche;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "meta_description", length = 500)
    private String metaDescription;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "word_count")
    private Integer wordCount;
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "published_date")
    private LocalDateTime publishedDate;
    
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (content != null) {
            wordCount = content.split("\\s+").length;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        if (content != null) {
            wordCount = content.split("\\s+").length;
        }
    }
}
