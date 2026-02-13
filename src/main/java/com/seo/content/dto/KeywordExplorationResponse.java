package com.seo.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordExplorationResponse {
    private boolean success;
    private String message;
    private int keywordsDiscovered;
    private int keywordsQualified;
    private int keywordsSaved;
}