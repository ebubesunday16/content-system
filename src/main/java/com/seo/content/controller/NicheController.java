package com.seo.content.controller;

import com.seo.content.model.Niche;
import com.seo.content.repository.NicheRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/niches")
@RequiredArgsConstructor
public class NicheController {
    
    private final NicheRepository nicheRepository;
    
    @GetMapping
    public ResponseEntity<List<Niche>> getAllNiches() {
        return ResponseEntity.ok(nicheRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Niche> getNiche(@PathVariable Long id) {
        return nicheRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Niche> createNiche(@RequestBody CreateNicheRequest request) {
        if (nicheRepository.existsByNicheName(request.getNicheName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        Niche niche = Niche.builder()
                .nicheName(request.getNicheName())
                .description(request.getDescription())
                .seedKeywords(String.join(",", request.getSeedKeywords()))
                .createdDate(LocalDateTime.now())
                .build();
        
        Niche saved = nicheRepository.save(niche);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Niche> updateNiche(
            @PathVariable Long id, 
            @RequestBody UpdateNicheRequest request) {
        
        return nicheRepository.findById(id)
                .map(niche -> {
                    if (request.getDescription() != null) {
                        niche.setDescription(request.getDescription());
                    }
                    if (request.getSeedKeywords() != null) {
                        niche.setSeedKeywords(String.join(",", request.getSeedKeywords()));
                    }
                    return ResponseEntity.ok(nicheRepository.save(niche));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNiche(@PathVariable Long id) {
        if (!nicheRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        nicheRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @Data
    public static class CreateNicheRequest {
        private String nicheName;
        private String description;
        private List<String> seedKeywords;
    }
    
    @Data
    public static class UpdateNicheRequest {
        private String description;
        private List<String> seedKeywords;
    }
}
