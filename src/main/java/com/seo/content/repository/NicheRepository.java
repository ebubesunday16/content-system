package com.seo.content.repository;

import com.seo.content.model.Niche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NicheRepository extends JpaRepository<Niche, Long> {
    
    Optional<Niche> findByNicheName(String nicheName);
    
    boolean existsByNicheName(String nicheName);
}
