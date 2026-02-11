package com.seo.content.service;

import com.seo.content.dto.GoogleSuggestResponse;
import com.seo.content.model.Niche;
import com.seo.content.model.PotentialKeyword;
import com.seo.content.repository.PotentialKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordDiscoveryService {
    
    private final RestTemplate restTemplate;
    private final PotentialKeywordRepository keywordRepository;
    
    private static final String GOOGLE_SUGGEST_URL = 
            "http://suggestqueries.google.com/complete/search";
    
    private static final Pattern SUGGESTION_PATTERN = 
            Pattern.compile("\\[\"([^\"]+)\"");
    
    /**
     * Fetches keyword suggestions from Google's autocomplete API
     */
    public GoogleSuggestResponse fetchGoogleSuggestions(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            
            String url = UriComponentsBuilder.fromUriString(GOOGLE_SUGGEST_URL)
                    .queryParam("client", "firefox")
                    .queryParam("q", encodedKeyword)
                    .build(false) // Don't encode again
                    .toUriString();
            
            log.debug("Fetching suggestions for: {}", keyword);
            
            String response = restTemplate.getForObject(url, String.class);
            
            List<String> suggestions = parseSuggestions(response);
            
            log.info("Found {} suggestions for keyword: {}", suggestions.size(), keyword);
            
            return GoogleSuggestResponse.builder()
                    .query(keyword)
                    .suggestions(suggestions)
                    .build();
            
        } catch (Exception e) {
            log.error("Error fetching Google suggestions for: {}", keyword, e);
            return GoogleSuggestResponse.builder()
                    .query(keyword)
                    .suggestions(new ArrayList<>())
                    .build();
        }
    }
    
    /**
     * Expands keyword tree by exploring a seed keyword and its variations
     */
    public List<String> expandKeywordTree(String seedKeyword, int currentDepth) {
        List<String> allSuggestions = new ArrayList<>();
        
        // Get direct suggestions
        GoogleSuggestResponse directSuggestions = fetchGoogleSuggestions(seedKeyword);
        allSuggestions.addAll(directSuggestions.getSuggestions());
        
        // Add common modifiers for depth
        if (currentDepth <= 2) {
            List<String> modifiers = getModifiersForDepth(currentDepth);
            
            for (String modifier : modifiers) {
                String modifiedQuery = seedKeyword + " " + modifier;
                GoogleSuggestResponse modifiedSuggestions = fetchGoogleSuggestions(modifiedQuery);
                allSuggestions.addAll(modifiedSuggestions.getSuggestions());
                
                // Rate limiting to be respectful
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // Remove duplicates and filter
        return allSuggestions.stream()
                .distinct()
                .filter(s -> s != null && !s.trim().isEmpty())
                .filter(s -> !s.equalsIgnoreCase(seedKeyword))
                .collect(Collectors.toList());
    }
    
    /**
     * Discovers new keywords from multiple seed keywords
     */
    public List<String> discoverKeywordsFromSeeds(List<String> seedKeywords, int targetDepth) {
        List<String> allDiscoveredKeywords = new ArrayList<>();
        
        for (String seed : seedKeywords) {
            log.info("Exploring seed keyword: {} at depth {}", seed, targetDepth);
            
            List<String> suggestions = expandKeywordTree(seed, targetDepth);
            allDiscoveredKeywords.addAll(suggestions);
            
            // Rate limiting
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return allDiscoveredKeywords.stream()
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Filters out keywords that already exist in the database
     */
    public List<String> filterNewKeywords(List<String> suggestions, Niche niche) {
        return suggestions.stream()
                .filter(keyword -> !keywordRepository.existsByKeywordText(keyword))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets appropriate modifiers based on depth level
     */
    private List<String> getModifiersForDepth(int depth) {
        if (depth == 0) {
            return List.of("how to", "what is", "best", "guide");
        } else if (depth == 1) {
            return List.of("tips", "for beginners", "examples", "vs");
        } else {
            return List.of("benefits", "cost", "reviews", "comparison");
        }
    }
    
    /**
     * Parses Google's JSON-like response format
     */
    private List<String> parseSuggestions(String response) {
        List<String> suggestions = new ArrayList<>();
        
        if (response == null || response.isEmpty()) {
            return suggestions;
        }
        
        try {
            // Google returns: ["query",["suggestion1","suggestion2",...]]
            // We need to extract the suggestions array
            
            Matcher matcher = SUGGESTION_PATTERN.matcher(response);
            boolean firstMatch = true;
            
            while (matcher.find()) {
                if (firstMatch) {
                    // Skip the first match (it's the query itself)
                    firstMatch = false;
                    continue;
                }
                suggestions.add(matcher.group(1));
            }
            
        } catch (Exception e) {
            log.error("Error parsing suggestions", e);
        }
        
        return suggestions;
    }
    
    /**
     * Generates alphabet soup variations (keyword + a, keyword + b, etc.)
     * Useful for comprehensive keyword discovery
     */
    public List<String> generateAlphabetSoupSuggestions(String keyword) {
        List<String> allSuggestions = new ArrayList<>();
        
        // Only use common starting letters to avoid too many requests
        String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "t", "w"};
        
        for (String letter : letters) {
            String query = keyword + " " + letter;
            GoogleSuggestResponse response = fetchGoogleSuggestions(query);
            allSuggestions.addAll(response.getSuggestions());
            
            // Rate limiting
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return allSuggestions.stream()
                .distinct()
                .collect(Collectors.toList());
    }
}
