package com.seo.content.repository;

import com.seo.content.model.Article;
import com.seo.content.model.Niche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    List<Article> findByNiche(Niche niche);
    
    @Query("SELECT a FROM Article a WHERE a.niche = :niche ORDER BY a.createdDate DESC")
    List<Article> findByNicheOrderByCreatedDateDesc(@Param("niche") Niche niche);
    
    @Query("SELECT COUNT(a) FROM Article a WHERE a.niche = :niche")
    Long countByNiche(@Param("niche") Niche niche);
    
    @Query("SELECT a FROM Article a JOIN FETCH a.keyword WHERE a.niche = :niche")
    List<Article> findByNicheWithKeywords(@Param("niche") Niche niche);
}
