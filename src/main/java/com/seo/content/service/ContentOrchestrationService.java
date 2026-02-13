package com.seo.content.service;

import com.seo.content.dto.KeywordExplorationResponse;
import com.seo.content.dto.LLMDto.*;
import com.seo.content.model.*;
import com.seo.content.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentOrchestrationService {
    
    private final LLMService llmService;
    private final KeywordDiscoveryService keywordDiscoveryService;
    private final NicheRepository nicheRepository;
    private final PotentialKeywordRepository keywordRepository;
    private final ArticleRepository articleRepository;
    private final ExplorationLogRepository explorationLogRepository;
    
    /**
     * Main daily workflow execution
     */
    @Transactional
    public void executeDailyWorkflow(Long nicheId) {
        long startTime = System.currentTimeMillis();
        
        log.info("=== Starting Daily SEO Content Workflow for Niche ID: {} ===", nicheId);
        
        try {
            // Load niche
            Niche niche = nicheRepository.findById(nicheId)
                    .orElseThrow(() -> new RuntimeException("Niche not found: " + nicheId));
            
            // Initialize metrics
            int keywordsDiscovered = 0;
            int keywordsQualified = 0;
            int articlesGenerated = 0;
            
            // Step 1: Decide exploration strategy
            log.info("Step 1: Deciding exploration strategy...");
            List<PotentialKeyword> existingKeywords = keywordRepository.findByNiche(niche);
            ExplorationStrategyResponse strategy = llmService.decideExplorationStrategy(niche, existingKeywords);
            log.info("Strategy: {} - Target depth: {}", strategy.getStrategy(), strategy.getTargetDepthLevel());
            
            // Step 2: Discover new keywords
            log.info("Step 2: Discovering keywords...");
            List<String> seedKeywords = strategy.getSeedKeywordsToExplore();
            if (seedKeywords == null || seedKeywords.isEmpty()) {
                seedKeywords = niche.getSeedKeywordsAsList();
            }
            
            List<String> discoveredSuggestions = keywordDiscoveryService.discoverKeywordsFromSeeds(
                    seedKeywords, 
                    strategy.getTargetDepthLevel()
            );
            
            // Filter out existing keywords
            List<String> newSuggestions = keywordDiscoveryService.filterNewKeywords(discoveredSuggestions, niche);
            keywordsDiscovered = newSuggestions.size();
            log.info("Discovered {} new keyword suggestions", keywordsDiscovered);
            
            // Step 3: Qualify keywords with LLM
            if (!newSuggestions.isEmpty()) {
                log.info("Step 3: Qualifying keywords with LLM...");
                
                // Process in batches of 20
                List<PotentialKeyword> qualifiedKeywords = new ArrayList<>();
                
                for (int i = 0; i < newSuggestions.size(); i += 20) {
                    int end = Math.min(i + 20, newSuggestions.size());
                    List<String> batch = newSuggestions.subList(i, end);
                    
                    List<KeywordQualification> qualifications = llmService.qualifyKeywords(
                            batch, niche, existingKeywords
                    );
                    
                    // Save qualified keywords
                    for (KeywordQualification qual : qualifications) {
                        if (qual.getRelevant() && !qual.getOverlapsExisting()) {
                            PotentialKeyword keyword = createPotentialKeyword(
                                    qual, niche, strategy.getTargetDepthLevel(), seedKeywords
                            );
                            qualifiedKeywords.add(keyword);
                            
                            if (keyword.isQualified()) {
                                keywordsQualified++;
                            }
                        }
                    }
                    
                    // Rate limiting
                    if (end < newSuggestions.size()) {
                        Thread.sleep(1000);
                    }
                }
                
                keywordRepository.saveAll(qualifiedKeywords);
                log.info("Saved {} qualified keywords", keywordsQualified);
            }
            
            // Step 4: Select and generate article
            log.info("Step 4: Selecting keyword for article generation...");
            List<PotentialKeyword> unwrittenKeywords = keywordRepository.findUnwrittenQualifiedKeywords(niche);
            
            if (!unwrittenKeywords.isEmpty()) {
                KeywordSelectionResponse selection = llmService.selectBestKeywordForArticle(
                        unwrittenKeywords.subList(0, Math.min(20, unwrittenKeywords.size())), 
                        niche
                );
                
                log.info("Selected keyword: {} - {}", selection.getSelectedKeyword(), selection.getReasoning());
                
                // Find the selected keyword
                Optional<PotentialKeyword> selectedKeywordOpt = unwrittenKeywords.stream()
                        .filter(kw -> kw.getKeywordText().equals(selection.getSelectedKeyword()))
                        .findFirst();
                
                if (selectedKeywordOpt.isPresent()) {
                    PotentialKeyword selectedKeyword = selectedKeywordOpt.get();
                    
                    // Step 5: Check similarity
                    log.info("Step 5: Checking content similarity...");
                    List<Article> existingArticles = articleRepository.findByNicheWithKeywords(niche);
                    SimilarityCheckResponse similarity = llmService.checkContentSimilarity(
                            selectedKeyword.getKeywordText(), 
                            existingArticles
                    );
                    
                    if (!similarity.getSimilar() || similarity.getSimilarityScore() < 0.7) {
                        // Step 6: Generate article
                        log.info("Step 6: Generating article...");
                        ArticleContent content = llmService.generateArticle(selectedKeyword, niche);
                        
                        // Save article
                        Article article = Article.builder()
                                .keyword(selectedKeyword)
                                .niche(niche)
                                .title(content.getTitle())
                                .metaDescription(content.getMetaDescription())
                                .content(content.getContent())
                                .build();
                        
                        articleRepository.save(article);
                        
                        // Update keyword status
                        selectedKeyword.setStatus(KeywordStatus.WRITTEN);
                        selectedKeyword.setWrittenDate(LocalDateTime.now());
                        keywordRepository.save(selectedKeyword);
                        
                        articlesGenerated = 1;
                        log.info("Article generated successfully: {}", content.getTitle());
                    } else {
                        log.info("Skipping article - too similar to existing content");
                    }
                } else {
                    log.warn("Selected keyword not found in unwritten list");
                }
            } else {
                log.info("No qualified unwritten keywords available for article generation");
            }
            
            // Step 7: Generate daily summary
            log.info("Step 7: Generating daily summary...");
            List<PotentialKeyword> recentKeywords = keywordRepository.findByNiche(niche).stream()
                    .sorted((a, b) -> b.getDiscoveredDate().compareTo(a.getDiscoveredDate()))
                    .limit(10)
                    .collect(Collectors.toList());
            
            DailySummary summary = llmService.generateDailySummary(
                    keywordsDiscovered, keywordsQualified, articlesGenerated, niche, recentKeywords
            );
            
            // Save exploration log
            Integer maxDepth = keywordRepository.findMaxDepthLevelByNiche(niche);
            long duration = System.currentTimeMillis() - startTime;
            
            ExplorationLog explorationLog = ExplorationLog.builder()
                    .niche(niche)
                    .executionDate(LocalDateTime.now())
                    .explorationStrategy(strategy.getStrategy() + " - " + strategy.getReasoning())
                    .currentMaxDepthLevel(maxDepth != null ? maxDepth : 0)
                    .keywordsDiscovered(keywordsDiscovered)
                    .keywordsQualified(keywordsQualified)
                    .articlesGenerated(articlesGenerated)
                    .llmNotes(summary.getSummary() + "\n\nNext steps: " + summary.getNextSteps())
                    .executionDurationMs(duration)
                    .success(true)
                    .build();
            
            explorationLogRepository.save(explorationLog);
            
            log.info("=== Daily Workflow Completed Successfully ===");
            log.info("Duration: {}ms", duration);
            log.info("Keywords discovered: {}", keywordsDiscovered);
            log.info("Keywords qualified: {}", keywordsQualified);
            log.info("Articles generated: {}", articlesGenerated);
            
        } catch (Exception e) {
            log.error("Error in daily workflow execution", e);
            
            // Save error log
            ExplorationLog errorLog = ExplorationLog.builder()
                    .niche(nicheRepository.findById(nicheId).orElse(null))
                    .executionDate(LocalDateTime.now())
                    .explorationStrategy("ERROR")
                    .keywordsDiscovered(0)
                    .keywordsQualified(0)
                    .articlesGenerated(0)
                    .llmNotes("Workflow failed")
                    .executionDurationMs(System.currentTimeMillis() - startTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            
            explorationLogRepository.save(errorLog);
            
            throw new RuntimeException("Daily workflow failed", e);
        }
    }
    
    /**
 * Manually trigger keyword exploration without article generation
 */
@Transactional
public KeywordExplorationResponse exploreKeywordsOnly(Long nicheId, List<String> seedKeywords, int depth) {
    Niche niche = nicheRepository.findById(nicheId)
            .orElseThrow(() -> new RuntimeException("Niche not found"));
    
    log.info("Manual keyword exploration for: {}", niche.getNicheName());
    
    List<String> suggestions = keywordDiscoveryService.discoverKeywordsFromSeeds(seedKeywords, depth);
    List<String> newSuggestions = keywordDiscoveryService.filterNewKeywords(suggestions, niche);
    
    List<PotentialKeyword> existingKeywords = keywordRepository.findByNiche(niche);
    
    // Qualify in batches
    List<PotentialKeyword> qualifiedKeywords = new ArrayList<>();
    int qualifiedCount = 0;
    
    for (int i = 0; i < newSuggestions.size(); i += 20) {
        int end = Math.min(i + 20, newSuggestions.size());
        List<String> batch = newSuggestions.subList(i, end);
        
        List<KeywordQualification> qualifications = llmService.qualifyKeywords(
                batch, niche, existingKeywords
        );
        
        for (KeywordQualification qual : qualifications) {
            if (qual.getRelevant() && !qual.getOverlapsExisting()) {
                PotentialKeyword keyword = createPotentialKeyword(qual, niche, depth, seedKeywords);
                qualifiedKeywords.add(keyword);
                
                if (keyword.isQualified()) {
                    qualifiedCount++;
                }
            }
        }
    }
    
    List<PotentialKeyword> saved = keywordRepository.saveAll(qualifiedKeywords);
    log.info("Saved {} qualified keywords from manual exploration", saved.size());
    
    return KeywordExplorationResponse.builder()
            .success(true)
            .message("Keywords explored successfully")
            .keywordsDiscovered(newSuggestions.size())
            .keywordsQualified(qualifiedCount)
            .keywordsSaved(saved.size())
            .build();
}
    /**
     * Generate article for a specific keyword
     */
    @Transactional
    public Article generateArticleForKeyword(Long keywordId) {
        PotentialKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("Keyword not found"));
        
        if (keyword.getStatus() == KeywordStatus.WRITTEN) {
            throw new RuntimeException("Article already exists for this keyword");
        }
        
        Niche niche = keyword.getNiche();
        
        // Check similarity
        List<Article> existingArticles = articleRepository.findByNicheWithKeywords(niche);
        SimilarityCheckResponse similarity = llmService.checkContentSimilarity(
                keyword.getKeywordText(), 
                existingArticles
        );
        
        if (similarity.getSimilar() && similarity.getSimilarityScore() >= 0.7) {
            throw new RuntimeException("Content too similar to existing articles: " + 
                    similarity.getOverlappingArticles());
        }
        
        // Generate article
        ArticleContent content = llmService.generateArticle(keyword, niche);
        
        Article article = Article.builder()
                .keyword(keyword)
                .niche(niche)
                .title(content.getTitle())
                .metaDescription(content.getMetaDescription())
                .content(content.getContent())
                .build();
        
        articleRepository.save(article);
        
        keyword.setStatus(KeywordStatus.WRITTEN);
        keyword.setWrittenDate(LocalDateTime.now());
        keywordRepository.save(keyword);
        
        log.info("Article generated for keyword: {}", keyword.getKeywordText());
        
        return article;
    }
    
    /**
     * Helper method to create PotentialKeyword from qualification
     */
    private PotentialKeyword createPotentialKeyword(
            KeywordQualification qual, 
            Niche niche, 
            int depth,
            List<String> seedKeywords) {
        
        // Try to find parent keyword
        PotentialKeyword parent = null;
        for (String seed : seedKeywords) {
            Optional<PotentialKeyword> parentOpt = keywordRepository.findByKeywordText(seed);
            if (parentOpt.isPresent()) {
                parent = parentOpt.get();
                break;
            }
        }
        
        return PotentialKeyword.builder()
                .keywordText(qual.getKeyword())
                .niche(niche)
                .depthLevel(depth)
                .parentKeyword(parent)
                .qualificationScore(qual.getScore())
                .qualificationReasoning(qual.getReasoning())
                .status(qual.getScore() >= 5.0 ? KeywordStatus.UNWRITTEN : KeywordStatus.REJECTED)
                .discoveredDate(LocalDateTime.now())
                .build();
    }
}
