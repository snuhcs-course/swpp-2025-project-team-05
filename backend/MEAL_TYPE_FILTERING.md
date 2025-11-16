# Meal Type Filtering - Complete Implementation

## Overview
Updated the occasion-based filtering system to support all 10 meal types from the database.

## Database Meal Types (290 Total Items)

| Meal Type       | Count | Status |
|----------------|-------|--------|
| rice-based     | 64    | ✅ WORKING |
| soup-based     | 31    | ✅ WORKING |
| meat-based     | 44    | ✅ WORKING |
| noodle-based   | 46    | ✅ WORKING |
| seafood-based  | 23    | ✅ WORKING |
| bread-based    | 37    | ✅ WORKING |
| salad-based    | 5     | ✅ WORKING |
| snack          | 15    | ✅ WORKING |
| dessert        | 16    | ✅ WORKING |
| beverage       | 9     | ✅ WORKING |

## What Was Fixed

### Before
The `filter_by_meal_characteristics` function only recognized 6 meal types:
- ✅ rice-based, soup-based, noodle-based, bread-based
- ❌ grilled, fried (not actual meal types in database)
- ❌ Missing: meat-based, seafood-based, salad-based, snack, dessert, beverage

### After
Now supports all 10 meal types with comprehensive keyword mapping:

```python
meal_type_keywords = {
    'rice-based': ['rice', 'bibimbap', 'rice bowl', 'fried rice', 'risotto', 'pilaf'],
    'soup-based': ['soup', 'stew', 'jjigae', 'hot pot', 'broth', 'chowder'],
    'meat-based': ['meat', 'beef', 'pork', 'chicken', 'steak', 'bbq', 'barbecue', 'grilled', 'gui'],
    'noodle-based': ['noodle', 'pasta', 'ramen', 'udon', 'soba', 'spaghetti', 'linguine'],
    'seafood-based': ['seafood', 'fish', 'shrimp', 'crab', 'lobster', 'salmon', 'tuna', 'sushi'],
    'bread-based': ['bread', 'sandwich', 'wrap', 'burger', 'toast', 'baguette'],
    'salad-based': ['salad', 'greens', 'lettuce', 'fresh'],
    'snack': ['snack', 'appetizer', 'side dish', 'banchan', 'finger food'],
    'dessert': ['dessert', 'sweet', 'cake', 'ice cream', 'pastry', 'pudding'],
    'beverage': ['beverage', 'drink', 'juice', 'smoothie', 'tea', 'coffee']
}
```

## How It Works

When a user creates a poll with an occasion/title containing meal type keywords, the system:

1. **Detects Keywords**: Scans the occasion text for meal type keywords
2. **Filters Items**: Only shows food items matching the detected meal type
3. **Ranks Results**: Uses LLM to rank the filtered items based on soft preferences

### Example Usage

| Poll Title/Occasion | Detected Type | Result |
|---------------------|---------------|--------|
| "Let's get some soup for lunch" | `soup-based` | Shows 31 soup items only |
| "I want seafood tonight" | `seafood-based` | Shows 23 seafood items only |
| "Looking for a quick snack" | `snack` | Shows 15 snack items only |
| "What dessert should we get?" | `dessert` | Shows 16 dessert items only |
| "Need something with rice" | `rice-based` | Shows 64 rice items only |
| "Want some meat BBQ" | `meat-based` | Shows 44 meat items only |
| "Let's grab some noodles" | `noodle-based` | Shows 46 noodle items only |
| "Sandwich or burger?" | `bread-based` | Shows 37 bread items only |
| "Fresh salad for lunch" | `salad-based` | Shows 5 salad items only |
| "Get some drinks?" | `beverage` | Shows 9 beverage items only |

## Combined Filtering

The system can also combine multiple filters:

### Meal Type + Heaviness
- **"Light soup for dinner"**
  - Filters: `meal_type=soup-based` + `heaviness=light`
  - Shows only light soup items

### Meal Type + Nutrition
- **"High protein meat"**
  - Filters: `meal_type=meat-based`
  - Ranks by: protein content (highest first)

### All Three Combined
- **"Light low calorie seafood"**
  - Filters: `meal_type=seafood-based` + `heaviness=light`
  - Ranks by: calories (lowest first)

## Code Location

**File**: `/Users/a1234/Desktop/swpp-2025-project-team-05/backend/server.py`

**Function**: `filter_by_meal_characteristics` (lines 309-400)

**How Filtering Works**:
```python
def filter_by_meal_characteristics(foods: List[Dict], occasion: str) -> List[Dict]:
    # Detect meal type from occasion text
    for meal_type, keywords in meal_type_keywords.items():
        if any(keyword in occasion_lower for keyword in keywords):
            matched_meal_type = meal_type
            break

    # Filter foods by meal type
    for food in foods:
        food_meal_type = food.get('meal_type', '')
        if food_meal_type != matched_meal_type:
            continue  # Skip foods that don't match
        filtered.append(food)

    return filtered
```

## Testing

To test the meal type filtering:

1. **Create a poll** with an occasion containing a meal type keyword
2. **Check the candidates** returned - should only include items of that type
3. **Try combinations** like "light soup" or "high protein seafood"

### Test Cases

```bash
# Test 1: Soup filtering
curl -X POST http://localhost:5001/teams/{teamId}/create-poll \
  -H "Content-Type: application/json" \
  -d '{
    "pollTitle": "Let'\''s get some soup",
    "duration": 5,
    "selectedMembers": ["user1", "user2"]
  }'

# Expected: Only soup-based items (31 total)

# Test 2: Seafood filtering
curl -X POST http://localhost:5001/teams/{teamId}/create-poll \
  -H "Content-Type: application/json" \
  -d '{
    "pollTitle": "Sushi or fish tonight?",
    "duration": 5,
    "selectedMembers": ["user1", "user2"]
  }'

# Expected: Only seafood-based items (23 total)

# Test 3: Dessert filtering
curl -X POST http://localhost:5001/teams/{teamId}/create-poll \
  -H "Content-Type: application/json" \
  -d '{
    "pollTitle": "What dessert should we get?",
    "duration": 5,
    "selectedMembers": ["user1", "user2"]
  }'

# Expected: Only dessert items (16 total)
```

## Server Status

✅ Server running on http://127.0.0.1:5001
✅ All 10 meal types supported
✅ 290 food items loaded from database
✅ Meal type filtering active

## Related Systems

This filtering works together with:
- **Hard Constraints**: Filters out dietary restrictions, allergies, avoid ingredients
- **Nutrition Filters**: Ranks by calories, protein, carbs, fat
- **Heaviness Filters**: Filters by light/medium/heavy
- **LLM Ranking**: Ranks filtered results based on soft preferences (cuisines, spice)

All filters are applied in order:
1. Hard constraints (remove incompatible foods)
2. Meal type filtering (this system)
3. Heaviness filtering
4. Nutrition ranking
5. LLM ranking (soft preferences)
