package com.seo.content.repository;

import com.seo.content.model.ExplorationLog;
import com.seo.content.model.Niche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExplorationLogRepository extends JpaRepository<ExplorationLog, Long> {
    
    List<ExplorationLog> findByNicheOrderByExecutionDateDesc(Niche niche);
    
    @Query("SELECT el FROM ExplorationLog el WHERE el.niche = :niche " +
           "ORDER BY el.executionDate DESC LIMIT 1")
    Optional<ExplorationLog> findLatestByNiche(@Param("niche") Niche niche);
    
    @Query("SELECT el FROM ExplorationLog el WHERE el.niche = :niche " +
           "ORDER BY el.executionDate DESC LIMIT :limit")
    List<ExplorationLog> findRecentByNiche(@Param("niche") Niche niche, @Param("limit") int limit);
}
