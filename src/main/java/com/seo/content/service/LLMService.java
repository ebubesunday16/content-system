package com.seo.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seo.content.dto.LLMDto.*;
import com.seo.content.model.Article;
import com.seo.content.model.Niche;
import com.seo.content.model.PotentialKeyword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LLMService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${llm.api.key}")
    private String apiKey;
    
    @Value("${llm.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;
    
    @Value("${llm.model:claude-sonnet-4-20250514}")
    private String model;
    
    @Value("${llm.max.tokens:4096}")
    private Integer maxTokens;
    
    @Value("${llm.temperature:0.7}")
    private Double temperature;
    
    public LLMService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Decides exploration strategy for the day
     */
    public ExplorationStrategyResponse decideExplorationStrategy(
            Niche niche, 
            List<PotentialKeyword> existingKeywords) {
        
        String prompt = buildExplorationStrategyPrompt(niche, existingKeywords);
        
        String response = callLLM(prompt, "You are an SEO content strategist. " +
                "Respond with valid JSON only, no markdown formatting.");
        
        try {
            return objectMapper.readValue(response, ExplorationStrategyResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse exploration strategy response", e);
            // Fallback strategy
            return ExplorationStrategyResponse.builder()
                    .strategy("explore_seed_keywords")
                    .seedKeywordsToExplore(niche.getSeedKeywordsAsList())
                    .reasoning("Using default strategy due to parsing error")
                    .targetDepthLevel(1)
                    .build();
        }
    }
    
    /**
     * Qualifies a batch of keyword suggestions
     */
    public List<KeywordQualification> qualifyKeywords(
            List<String> suggestions, 
            Niche niche, 
            List<PotentialKeyword> existingKeywords) {
        
        String prompt = buildKeywordQualificationPrompt(suggestions, niche, existingKeywords);
        
        String response = callLLM(prompt, "You are an SEO keyword analyst. " +
                "Respond with a valid JSON array only, no markdown formatting.");
        
        try {
            return objectMapper.readValue(response, new TypeReference<List<KeywordQualification>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse keyword qualification response", e);
            // Return conservative qualifications
            return suggestions.stream()
                    .map(keyword -> KeywordQualification.builder()
                            .keyword(keyword)
                            .relevant(false)
                            .overlapsExisting(true)
                            .score(3.0)
                            .reasoning("Default low score due to parsing error")
                            .build())
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Selects the best keyword to write about
     */
    public KeywordSelectionResponse selectBestKeywordForArticle(
            List<PotentialKeyword> unwrittenKeywords, 
            Niche niche) {
        
        String prompt = buildKeywordSelectionPrompt(unwrittenKeywords, niche);
        
        String response = callLLM(prompt, "You are an SEO content strategist. " +
                "Respond with valid JSON only, no markdown formatting.");
        
        try {
            return objectMapper.readValue(response, KeywordSelectionResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse keyword selection response", e);
            // Fallback to highest scored keyword
            PotentialKeyword best = unwrittenKeywords.stream()
                    .max((a, b) -> Double.compare(
                            a.getQualificationScore() != null ? a.getQualificationScore() : 0.0,
                            b.getQualificationScore() != null ? b.getQualificationScore() : 0.0))
                    .orElse(unwrittenKeywords.get(0));
            
            return KeywordSelectionResponse.builder()
                    .selectedKeyword(best.getKeywordText())
                    .reasoning("Selected highest scoring keyword due to parsing error")
                    .contentAngle("Comprehensive guide")
                    .build();
        }
    }
    
    /**
     * Checks if new keyword is similar to existing content
     */
    public SimilarityCheckResponse checkContentSimilarity(
            String newKeyword, 
            List<Article> existingArticles) {
        
        String prompt = buildSimilarityCheckPrompt(newKeyword, existingArticles);
        
        String response = callLLM(prompt, "You are an SEO content analyst. " +
                "Respond with valid JSON only, no markdown formatting.");
        
        try {
            return objectMapper.readValue(response, SimilarityCheckResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse similarity check response", e);
            return SimilarityCheckResponse.builder()
                    .similar(false)
                    .reasoning("Unable to determine similarity")
                    .similarityScore(0.5)
                    .overlappingArticles(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Generates article content
     */
    public ArticleContent generateArticle(PotentialKeyword keyword, Niche niche) {
        
        String prompt = buildArticleGenerationPrompt(keyword, niche);
        
        String response = callLLM(prompt, "You are an expert SEO content writer. " +
                "Respond with valid JSON only, no markdown formatting.", 8000);
        
        try {
            return objectMapper.readValue(response, ArticleContent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse article generation response", e);
            // Return minimal article
            return ArticleContent.builder()
                    .title("Guide to " + keyword.getKeywordText())
                    .metaDescription("Learn everything about " + keyword.getKeywordText())
                    .content("Article generation failed. Please try again.")
                    .estimatedWordCount(0)
                    .build();
        }
    }
    
    /**
     * Generates daily summary
     */
    public DailySummary generateDailySummary(
            int keywordsDiscovered,
            int keywordsQualified,
            int articlesGenerated,
            Niche niche,
            List<PotentialKeyword> recentKeywords) {
        
        String prompt = buildDailySummaryPrompt(
                keywordsDiscovered, keywordsQualified, articlesGenerated, niche, recentKeywords);
        
        String response = callLLM(prompt, "You are an SEO strategist summarizing daily progress. " +
                "Respond with valid JSON only, no markdown formatting.");
        
        try {
            return objectMapper.readValue(response, DailySummary.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse daily summary response", e);
            return DailySummary.builder()
                    .summary("Daily workflow completed")
                    .keywordsDiscovered(keywordsDiscovered)
                    .keywordsQualified(keywordsQualified)
                    .articlesGenerated(articlesGenerated)
                    .nextSteps("Continue keyword exploration")
                    .build();
        }
    }
    
    // ==================== PROMPT BUILDERS ====================
    
    private String buildExplorationStrategyPrompt(Niche niche, List<PotentialKeyword> existingKeywords) {
        StringBuilder sb = new StringBuilder();
        sb.append("Niche: ").append(niche.getNicheName()).append("\n");
        sb.append("Description: ").append(niche.getDescription()).append("\n\n");
        
        if (!existingKeywords.isEmpty()) {
            sb.append("Existing Keyword Tree:\n");
            existingKeywords.stream()
                    .limit(50)
                    .forEach(kw -> sb.append("- ").append(kw.getKeywordText())
                            .append(" (depth: ").append(kw.getDepthLevel())
                            .append(", score: ").append(kw.getQualificationScore())
                            .append(")\n"));
        } else {
            sb.append("No existing keywords yet. This is the initial exploration.\n");
        }
        
        sb.append("\nBased on this niche and keyword tree, decide today's exploration strategy.\n");
        sb.append("Should we:\n");
        sb.append("1. Go deeper on promising branches (explore child keywords of high-scoring keywords)\n");
        sb.append("2. Explore new angles (use seed keywords or unexplored areas)\n");
        sb.append("3. Fill gaps in existing coverage\n\n");
        sb.append("Respond with JSON in this format:\n");
        sb.append("{\n");
        sb.append("  \"strategy\": \"explore_deeper|explore_new|fill_gaps\",\n");
        sb.append("  \"seedKeywordsToExplore\": [\"keyword1\", \"keyword2\"],\n");
        sb.append("  \"reasoning\": \"explanation of why this strategy\",\n");
        sb.append("  \"targetDepthLevel\": 2\n");
        sb.append("}");
        
        return sb.toString();
    }
    
    private String buildKeywordQualificationPrompt(
            List<String> suggestions, Niche niche, List<PotentialKeyword> existingKeywords) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Niche: ").append(niche.getNicheName()).append("\n");
        sb.append("Description: ").append(niche.getDescription()).append("\n\n");
        
        sb.append("Existing keywords: ");
        sb.append(existingKeywords.stream()
                .map(PotentialKeyword::getKeywordText)
                .limit(30)
                .collect(Collectors.joining(", ")));
        sb.append("\n\n");
        
        sb.append("New keyword suggestions to evaluate:\n");
        suggestions.forEach(s -> sb.append("- ").append(s).append("\n"));
        
        sb.append("\nFor each suggestion, evaluate:\n");
        sb.append("1. Is it relevant to our niche?\n");
        sb.append("2. Does it overlap significantly with existing keywords?\n");
        sb.append("3. Score the SEO opportunity (0-10, where 10 is excellent)\n\n");
        sb.append("Respond with a JSON array in this format:\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"keyword\": \"suggestion text\",\n");
        sb.append("    \"relevant\": true,\n");
        sb.append("    \"overlapsExisting\": false,\n");
        sb.append("    \"score\": 7.5,\n");
        sb.append("    \"reasoning\": \"why this score\"\n");
        sb.append("  }\n");
        sb.append("]");
        
        return sb.toString();
    }
    
    private String buildKeywordSelectionPrompt(List<PotentialKeyword> unwrittenKeywords, Niche niche) {
        StringBuilder sb = new StringBuilder();
        sb.append("Niche: ").append(niche.getNicheName()).append("\n");
        sb.append("Description: ").append(niche.getDescription()).append("\n\n");
        
        sb.append("Qualified unwritten keywords:\n");
        unwrittenKeywords.stream()
                .limit(20)
                .forEach(kw -> sb.append("- ").append(kw.getKeywordText())
                        .append(" (score: ").append(kw.getQualificationScore())
                        .append(", depth: ").append(kw.getDepthLevel())
                        .append(")\n"));
        
        sb.append("\nWhich keyword should we write about today?\n");
        sb.append("Consider:\n");
        sb.append("- Strategic value for building a content cluster\n");
        sb.append("- Filling content gaps\n");
        sb.append("- Creating foundational vs. supporting content\n\n");
        sb.append("Respond with JSON in this format:\n");
        sb.append("{\n");
        sb.append("  \"selectedKeyword\": \"exact keyword text\",\n");
        sb.append("  \"reasoning\": \"why this keyword\",\n");
        sb.append("  \"contentAngle\": \"the unique angle for this article\"\n");
        sb.append("}");
        
        return sb.toString();
    }
    
    private String buildSimilarityCheckPrompt(String newKeyword, List<Article> existingArticles) {
        StringBuilder sb = new StringBuilder();
        sb.append("New keyword: ").append(newKeyword).append("\n\n");
        
        sb.append("Existing articles:\n");
        existingArticles.forEach(article -> 
                sb.append("- Title: ").append(article.getTitle())
                        .append(" | Keyword: ").append(article.getKeyword().getKeywordText())
                        .append("\n"));
        
        sb.append("\nDoes the new keyword '").append(newKeyword).append("' ");
        sb.append("represent substantially the same topic as any existing article?\n");
        sb.append("We want to avoid duplicate content but allow complementary topics.\n\n");
        sb.append("Respond with JSON in this format:\n");
        sb.append("{\n");
        sb.append("  \"similar\": false,\n");
        sb.append("  \"reasoning\": \"explanation\",\n");
        sb.append("  \"similarityScore\": 0.0,\n");
        sb.append("  \"overlappingArticles\": []\n");
        sb.append("}");
        
        return sb.toString();
    }
    
    private String buildArticleGenerationPrompt(PotentialKeyword keyword, Niche niche) {
        StringBuilder sb = new StringBuilder();
        sb.append("Write a comprehensive, SEO-optimized article for:\n\n");
        sb.append("Niche: ").append(niche.getNicheName()).append("\n");
        sb.append("Target Keyword: ").append(keyword.getKeywordText()).append("\n");
        sb.append("Keyword Context: ").append(keyword.getQualificationReasoning()).append("\n\n");
        
        sb.append("Requirements:\n");
        sb.append("- 1500-2500 words\n");
        sb.append("- Include the keyword naturally throughout\n");
        sb.append("- Use proper heading structure (H2, H3)\n");
        sb.append("- Provide actionable, valuable information\n");
        sb.append("- Write in a clear, engaging style\n");
        sb.append("- Include an introduction and conclusion\n\n");
        
        sb.append("Respond with JSON in this format:\n");
        sb.append("{\n");
        sb.append("  \"title\": \"SEO-optimized title\",\n");
        sb.append("  \"metaDescription\": \"compelling 150-160 char meta description\",\n");
        sb.append("  \"content\": \"full article content in HTML format\",\n");
        sb.append("  \"estimatedWordCount\": 2000\n");
        sb.append("}");
        
        return sb.toString();
    }
    
    private String buildDailySummaryPrompt(
            int keywordsDiscovered, int keywordsQualified, int articlesGenerated,
            Niche niche, List<PotentialKeyword> recentKeywords) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Today's SEO content generation summary:\n\n");
        sb.append("Niche: ").append(niche.getNicheName()).append("\n");
        sb.append("Keywords discovered: ").append(keywordsDiscovered).append("\n");
        sb.append("Keywords qualified: ").append(keywordsQualified).append("\n");
        sb.append("Articles generated: ").append(articlesGenerated).append("\n\n");
        
        if (!recentKeywords.isEmpty()) {
            sb.append("Sample keywords discovered:\n");
            recentKeywords.stream()
                    .limit(10)
                    .forEach(kw -> sb.append("- ").append(kw.getKeywordText()).append("\n"));
        }
        
        sb.append("\nProvide a strategic summary and recommendations for tomorrow.\n\n");
        sb.append("Respond with JSON in this format:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"brief overview of today's progress\",\n");
        sb.append("  \"keywordsDiscovered\": ").append(keywordsDiscovered).append(",\n");
        sb.append("  \"keywordsQualified\": ").append(keywordsQualified).append(",\n");
        sb.append("  \"articlesGenerated\": ").append(articlesGenerated).append(",\n");
        sb.append("  \"nextSteps\": \"strategic recommendations\"\n");
        sb.append("}");
        
        return sb.toString();
    }
    
    // ==================== LLM API CALL ====================
    
    private String callLLM(String prompt, String systemPrompt) {
        return callLLM(prompt, systemPrompt, maxTokens);
    }
    
    private String callLLM(String prompt, String systemPrompt, Integer tokens) {
        try {
            LLMRequest request = LLMRequest.builder()
                    .model(model)
                    .maxTokens(tokens)
                    .temperature(temperature)
                    .system(systemPrompt)
                    .messages(List.of(
                            Message.builder()
                                    .role("user")
                                    .content(prompt)
                                    .build()
                    ))
                    .build();
            
            LLMResponse response = webClient.post()
                    .uri(apiUrl)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .body(Mono.just(request), LLMRequest.class)
                    .retrieve()
                    .bodyToMono(LLMResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            
            if (response != null) {
                return response.getTextContent();
            }
            
            throw new RuntimeException("Empty response from LLM");
            
        } catch (Exception e) {
            log.error("Error calling LLM API", e);
            throw new RuntimeException("Failed to call LLM: " + e.getMessage(), e);
        }
    }
}
