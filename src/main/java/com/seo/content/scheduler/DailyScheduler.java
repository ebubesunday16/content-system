package com.seo.content.scheduler;

import com.seo.content.model.Niche;
import com.seo.content.repository.NicheRepository;
import com.seo.content.service.ContentOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DailyScheduler {
    
    private final ContentOrchestrationService orchestrationService;
    private final NicheRepository nicheRepository;
    
    /**
     * Daily workflow execution - runs at 2 AM every day
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "${scheduler.cron:0 0 2 * * *}")
    public void executeDailyWorkflow() {
        log.info("=== STARTING SCHEDULED DAILY WORKFLOW ===");
        
        try {
            List<Niche> allNiches = nicheRepository.findAll();
            
            if (allNiches.isEmpty()) {
                log.warn("No niches found in database. Skipping daily workflow.");
                return;
            }
            
            for (Niche niche : allNiches) {
                try {
                    log.info("Processing niche: {} (ID: {})", niche.getNicheName(), niche.getId());
                    orchestrationService.executeDailyWorkflow(niche.getId());
                    log.info("Successfully completed workflow for niche: {}", niche.getNicheName());
                    
                    // Add delay between niches to avoid rate limiting
                    if (allNiches.size() > 1) {
                        Thread.sleep(5000); // 5 second delay
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing niche: {}", niche.getNicheName(), e);
                    // Continue with next niche even if one fails
                }
            }
            
            log.info("=== DAILY WORKFLOW COMPLETED FOR ALL NICHES ===");
            
        } catch (Exception e) {
            log.error("Critical error in scheduled daily workflow", e);
        }
    }
    
    /**
     * Weekly deep exploration - runs every Sunday at 3 AM
     * Goes deeper into high-performing keyword branches
     */
    @Scheduled(cron = "${scheduler.weekly.cron:0 0 3 * * SUN}")
    public void executeWeeklyDeepExploration() {
        log.info("=== STARTING WEEKLY DEEP EXPLORATION ===");
        
        try {
            List<Niche> allNiches = nicheRepository.findAll();
            
            for (Niche niche : allNiches) {
                try {
                    log.info("Deep exploration for niche: {}", niche.getNicheName());
                    // This would trigger a more aggressive exploration strategy
                    orchestrationService.executeDailyWorkflow(niche.getId());
                    
                } catch (Exception e) {
                    log.error("Error in weekly exploration for niche: {}", niche.getNicheName(), e);
                }
            }
            
            log.info("=== WEEKLY DEEP EXPLORATION COMPLETED ===");
            
        } catch (Exception e) {
            log.error("Critical error in weekly deep exploration", e);
        }
    }
    
    /**
     * Hourly health check - logs system status
     */
    @Scheduled(cron = "${scheduler.health.cron:0 0 * * * *}")
    public void healthCheck() {
        try {
            long nicheCount = nicheRepository.count();
            log.debug("Health check - Active niches: {}", nicheCount);
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
}
