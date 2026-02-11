#!/bin/bash

# SEO Content Generator - API Test Script
# This script demonstrates all major API endpoints

BASE_URL="http://localhost:8080"

echo "==================================="
echo "SEO Content Generator - API Tests"
echo "==================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test 1: Create Niche
echo -e "${BLUE}1. Creating a new niche...${NC}"
NICHE_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/niches \
  -H "Content-Type: application/json" \
  -d '{
    "nicheName": "Home Gardening",
    "description": "Everything about growing plants, vegetables, and maintaining a home garden",
    "seedKeywords": ["home gardening", "vegetable garden", "indoor plants", "garden tips"]
  }')

echo "$NICHE_RESPONSE" | jq .
NICHE_ID=$(echo "$NICHE_RESPONSE" | jq -r '.id')
echo -e "${GREEN}✓ Niche created with ID: $NICHE_ID${NC}"
echo ""

# Test 2: List all niches
echo -e "${BLUE}2. Listing all niches...${NC}"
curl -s ${BASE_URL}/api/niches | jq .
echo ""

# Test 3: Explore keywords manually
echo -e "${BLUE}3. Exploring keywords manually...${NC}"
curl -s -X POST ${BASE_URL}/api/workflow/explore-keywords/${NICHE_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "seedKeywords": ["home gardening", "vegetable garden"],
    "depth": 1
  }' | jq .
echo -e "${GREEN}✓ Keyword exploration triggered${NC}"
echo ""

# Wait for keyword discovery
echo "Waiting 10 seconds for keyword discovery..."
sleep 10

# Test 4: Get workflow statistics
echo -e "${BLUE}4. Getting workflow statistics...${NC}"
curl -s ${BASE_URL}/api/workflow/stats/${NICHE_ID} | jq .
echo ""

# Test 5: Get discovered keywords
echo -e "${BLUE}5. Getting discovered keywords...${NC}"
curl -s "${BASE_URL}/api/workflow/keywords/${NICHE_ID}?status=UNWRITTEN" | jq '.[0:5]'
echo ""

# Test 6: Get unwritten keywords
echo -e "${BLUE}6. Getting unwritten qualified keywords...${NC}"
KEYWORDS=$(curl -s ${BASE_URL}/api/workflow/unwritten-keywords/${NICHE_ID})
echo "$KEYWORDS" | jq '.[0:5]'
KEYWORD_ID=$(echo "$KEYWORDS" | jq -r '.[0].id')
echo -e "${GREEN}✓ Found keyword ID: $KEYWORD_ID${NC}"
echo ""

# Test 7: Generate article for specific keyword
if [ ! -z "$KEYWORD_ID" ] && [ "$KEYWORD_ID" != "null" ]; then
    echo -e "${BLUE}7. Generating article for keyword ID: $KEYWORD_ID...${NC}"
    curl -s -X POST ${BASE_URL}/api/workflow/generate-article/${KEYWORD_ID} | jq .
    echo -e "${GREEN}✓ Article generated${NC}"
    echo ""
else
    echo -e "${BLUE}7. No qualified keywords available for article generation${NC}"
    echo ""
fi

# Test 8: Get all articles
echo -e "${BLUE}8. Getting all articles...${NC}"
curl -s ${BASE_URL}/api/workflow/articles/${NICHE_ID} | jq .
echo ""

# Test 9: Execute full daily workflow
echo -e "${BLUE}9. Executing full daily workflow...${NC}"
curl -s -X POST ${BASE_URL}/api/workflow/execute/${NICHE_ID} | jq .
echo -e "${GREEN}✓ Daily workflow executed${NC}"
echo ""

# Wait for workflow completion
echo "Waiting 30 seconds for workflow completion..."
sleep 30

# Test 10: Get execution logs
echo -e "${BLUE}10. Getting exploration logs...${NC}"
curl -s ${BASE_URL}/api/workflow/logs/${NICHE_ID} | jq '.[0:3]'
echo ""

# Test 11: Final statistics
echo -e "${BLUE}11. Final statistics...${NC}"
curl -s ${BASE_URL}/api/workflow/stats/${NICHE_ID} | jq .
echo ""

# Test 12: Filter keywords by depth
echo -e "${BLUE}12. Getting depth-1 keywords...${NC}"
curl -s "${BASE_URL}/api/workflow/keywords/${NICHE_ID}?depth=1" | jq '.[0:5]'
echo ""

# Test 13: Update niche
echo -e "${BLUE}13. Updating niche description...${NC}"
curl -s -X PUT ${BASE_URL}/api/niches/${NICHE_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Updated: Complete guide to home gardening, from seed to harvest"
  }' | jq .
echo -e "${GREEN}✓ Niche updated${NC}"
echo ""

echo "==================================="
echo -e "${GREEN}All tests completed!${NC}"
echo "==================================="
echo ""
echo "Summary:"
echo "- Niche ID: $NICHE_ID"
echo "- View statistics: ${BASE_URL}/api/workflow/stats/${NICHE_ID}"
echo "- View articles: ${BASE_URL}/api/workflow/articles/${NICHE_ID}"
echo "- View logs: ${BASE_URL}/api/workflow/logs/${NICHE_ID}"
echo ""
