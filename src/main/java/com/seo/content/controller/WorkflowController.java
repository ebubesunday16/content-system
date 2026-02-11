package com.seo.content.controller;

import com.seo.content.model.Article;
import com.seo.content.model.ExplorationLog;
import com.seo.content.model.KeywordStatus;
import com.seo.content.model.PotentialKeyword;
import com.seo.content.repository.ArticleRepository;
import com.seo.content.repository.ExplorationLogRepository;
import com.seo.content.repository.NicheRepository;
import com.seo.content.repository.PotentialKeywordRepository;
import com.seo.content.service.ContentOrchestrationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {
    
    private final ContentOrchestrationService orchestrationService;
    private final NicheRepository nicheRepository;
    private final PotentialKeywordRepository keywordRepository;
    private final ArticleRepository articleRepository;
    private final ExplorationLogRepository explorationLogRepository;
    
    /**
     * Manually trigger daily workflow for a niche
     */
    @PostMapping("/execute/{nicheId}")
    public ResponseEntity<Map<String, Object>> executeDailyWorkflow(@PathVariable Long nicheId) {
        try {
            orchestrationService.executeDailyWorkflow(nicheId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Daily workflow executed successfully");
            response.put("nicheId", nicheId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Workflow execution failed: " + e.getMessage());
            response.put("nicheId", nicheId);
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Manually explore keywords without generating articles
     */
    @PostMapping("/explore-keywords/{nicheId}")
    public ResponseEntity<Map<String, Object>> exploreKeywords(
            @PathVariable Long nicheId,
            @RequestBody ExploreKeywordsRequest request) {
        
        try {
            orchestrationService.exploreKeywordsOnly(
                    nicheId, 
                    request.getSeedKeywords(), 
                    request.getDepth()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Keywords explored successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Keyword exploration failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Generate article for a specific keyword
     */
    @PostMapping("/generate-article/{keywordId}")
    public ResponseEntity<Article> generateArticle(@PathVariable Long keywordId) {
        try {
            Article article = orchestrationService.generateArticleForKeyword(keywordId);
            return ResponseEntity.ok(article);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Get workflow statistics for a niche
     */
    @GetMapping("/stats/{nicheId}")
    public ResponseEntity<Map<String, Object>> getWorkflowStats(@PathVariable Long nicheId) {
        return nicheRepository.findById(nicheId)
                .map(niche -> {
                    Map<String, Object> stats = new HashMap<>();
                    
                    List<PotentialKeyword> allKeywords = keywordRepository.findByNiche(niche);
                    Long unwrittenCount = keywordRepository.countByNicheAndStatus(niche, KeywordStatus.UNWRITTEN);
                    Long writtenCount = keywordRepository.countByNicheAndStatus(niche, KeywordStatus.WRITTEN);
                    Long rejectedCount = keywordRepository.countByNicheAndStatus(niche, KeywordStatus.REJECTED);
                    Integer maxDepth = keywordRepository.findMaxDepthLevelByNiche(niche);
                    Long articleCount = articleRepository.countByNiche(niche);
                    
                    stats.put("totalKeywords", allKeywords.size());
                    stats.put("unwrittenKeywords", unwrittenCount);
                    stats.put("writtenKeywords", writtenCount);
                    stats.put("rejectedKeywords", rejectedCount);
                    stats.put("maxDepthLevel", maxDepth != null ? maxDepth : 0);
                    stats.put("totalArticles", articleCount);
                    
                    // Average qualification score
                    double avgScore = allKeywords.stream()
                            .filter(kw -> kw.getQualificationScore() != null)
                            .mapToDouble(PotentialKeyword::getQualificationScore)
                            .average()
                            .orElse(0.0);
                    stats.put("averageQualificationScore", avgScore);
                    
                    // Recent logs
                    List<ExplorationLog> recentLogs = explorationLogRepository.findRecentByNiche(niche, 5);
                    stats.put("recentExecutions", recentLogs);
                    
                    return ResponseEntity.ok(stats);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get unwritten qualified keywords
     */
    @GetMapping("/unwritten-keywords/{nicheId}")
    public ResponseEntity<List<PotentialKeyword>> getUnwrittenKeywords(@PathVariable Long nicheId) {
        return nicheRepository.findById(nicheId)
                .map(niche -> {
                    List<PotentialKeyword> keywords = keywordRepository.findUnwrittenQualifiedKeywords(niche);
                    return ResponseEntity.ok(keywords);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all keywords for a niche with filtering
     */
    @GetMapping("/keywords/{nicheId}")
    public ResponseEntity<List<PotentialKeyword>> getKeywords(
            @PathVariable Long nicheId,
            @RequestParam(required = false) KeywordStatus status,
            @RequestParam(required = false) Integer depth) {
        
        return nicheRepository.findById(nicheId)
                .map(niche -> {
                    List<PotentialKeyword> keywords;
                    
                    if (status != null) {
                        keywords = keywordRepository.findByNicheAndStatus(niche, status);
                    } else if (depth != null) {
                        keywords = keywordRepository.findByNicheAndDepthLevel(niche, depth);
                    } else {
                        keywords = keywordRepository.findByNiche(niche);
                    }
                    
                    return ResponseEntity.ok(keywords);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all articles for a niche
     */
    @GetMapping("/articles/{nicheId}")
    public ResponseEntity<List<Article>> getArticles(@PathVariable Long nicheId) {
        return nicheRepository.findById(nicheId)
                .map(niche -> {
                    List<Article> articles = articleRepository.findByNicheOrderByCreatedDateDesc(niche);
                    return ResponseEntity.ok(articles);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get exploration logs for a niche
     */
    @GetMapping("/logs/{nicheId}")
    public ResponseEntity<List<ExplorationLog>> getExplorationLogs(@PathVariable Long nicheId) {
        return nicheRepository.findById(nicheId)
                .map(niche -> {
                    List<ExplorationLog> logs = explorationLogRepository.findByNicheOrderByExecutionDateDesc(niche);
                    return ResponseEntity.ok(logs);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @Data
    public static class ExploreKeywordsRequest {
        private List<String> seedKeywords;
        private Integer depth;
    }
}
