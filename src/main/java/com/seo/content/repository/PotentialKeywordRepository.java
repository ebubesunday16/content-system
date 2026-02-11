package com.seo.content.repository;

import com.seo.content.model.KeywordStatus;
import com.seo.content.model.Niche;
import com.seo.content.model.PotentialKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PotentialKeywordRepository extends JpaRepository<PotentialKeyword, Long> {
    
    Optional<PotentialKeyword> findByKeywordText(String keywordText);
    
    boolean existsByKeywordText(String keywordText);
    
    List<PotentialKeyword> findByNicheAndStatus(Niche niche, KeywordStatus status);
    
    List<PotentialKeyword> findByNiche(Niche niche);
    
    @Query("SELECT pk FROM PotentialKeyword pk WHERE pk.niche = :niche AND pk.status = :status " +
           "AND pk.qualificationScore IS NOT NULL ORDER BY pk.qualificationScore DESC")
    List<PotentialKeyword> findTopQualifiedKeywords(@Param("niche") Niche niche, 
                                                     @Param("status") KeywordStatus status);
    
    @Query("SELECT pk FROM PotentialKeyword pk WHERE pk.niche = :niche " +
           "ORDER BY pk.depthLevel DESC")
    List<PotentialKeyword> findByNicheOrderByDepthLevelDesc(@Param("niche") Niche niche);
    
    @Query("SELECT MAX(pk.depthLevel) FROM PotentialKeyword pk WHERE pk.niche = :niche")
    Integer findMaxDepthLevelByNiche(@Param("niche") Niche niche);
    
    @Query("SELECT pk FROM PotentialKeyword pk WHERE pk.niche = :niche AND pk.depthLevel = :depth")
    List<PotentialKeyword> findByNicheAndDepthLevel(@Param("niche") Niche niche, 
                                                     @Param("depth") Integer depth);
    
    @Query("SELECT pk FROM PotentialKeyword pk WHERE pk.niche = :niche AND pk.status = 'UNWRITTEN' " +
           "AND pk.qualificationScore >= 5.0 ORDER BY pk.qualificationScore DESC, pk.depthLevel ASC")
    List<PotentialKeyword> findUnwrittenQualifiedKeywords(@Param("niche") Niche niche);
    
    @Query("SELECT COUNT(pk) FROM PotentialKeyword pk WHERE pk.niche = :niche AND pk.status = :status")
    Long countByNicheAndStatus(@Param("niche") Niche niche, @Param("status") KeywordStatus status);
}
