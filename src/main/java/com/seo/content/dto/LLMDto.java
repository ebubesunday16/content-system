package com.seo.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class LLMDto {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExplorationStrategyResponse {
        private String strategy;
        private List<String> seedKeywordsToExplore;
        private String reasoning;
        private Integer targetDepthLevel;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordQualification {
        private String keyword;
        private Boolean relevant;
        private Boolean overlapsExisting;
        private Double score;
        private String reasoning;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordSelectionResponse {
        private String selectedKeyword;
        private String reasoning;
        private String contentAngle;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarityCheckResponse {
        private Boolean similar;
        private String reasoning;
        private Double similarityScore;
        private List<String> overlappingArticles;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleContent {
        private String title;
        private String metaDescription;
        private String content;
        private Integer estimatedWordCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummary {
        private String summary;
        private Integer keywordsDiscovered;
        private Integer keywordsQualified;
        private Integer articlesGenerated;
        private String nextSteps;
    }
    
    // Anthropic/OpenAI API request/response structures
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LLMRequest {
        private String model;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private List<Message> messages;
        private Double temperature;
        private String system;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LLMResponse {
        private String id;
        private String type;
        private String role;
        private List<Content> content;
        private String model;
        @JsonProperty("stop_reason")
        private String stopReason;
        private Usage usage;
        
        public String getTextContent() {
            if (content != null && !content.isEmpty()) {
                return content.get(0).getText();
            }
            return "";
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String type;
        private String text;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;
        @JsonProperty("output_tokens")
        private Integer outputTokens;
    }
}
