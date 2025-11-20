from flask import Flask, request, jsonify
import firebase_admin
from firebase_admin import credentials, auth, firestore
import json
import os
from datetime import datetime, timedelta
from typing import List, Dict, Any
import random
import openai
from dotenv import load_dotenv

# Load environment variables from .env if present
load_dotenv()

app = Flask(__name__)

# Try to load Firebase credentials from environment variable or file
cred_json = os.environ.get("FIREBASE_CREDENTIALS")

if cred_json:
    # Load from environment variable
    cred_dict = json.loads(cred_json)
    cred = credentials.Certificate(cred_dict)
    firebase_admin.initialize_app(cred)
else:
    # Try to load from file
    # Resolve credentials file relative to this file location
    cred_path = os.path.join(os.path.dirname(__file__), "firebase-credentials.json")
    if os.path.exists(cred_path):
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
    else:
        print("WARNING: No Firebase credentials found. Poll functionality will be limited.")
        # Don't initialize Firebase Admin if credentials don't exist

try:
    db = firestore.client()
except Exception as e:
    print(f"WARNING: Could not connect to Firestore: {e}")
    db = None

def validate_env_on_startup() -> None:
    """Emit helpful logs about environment readiness."""
    # Firebase presence
    has_env_creds = bool(os.environ.get("FIREBASE_CREDENTIALS"))
    cred_path_chk = os.path.join(os.path.dirname(__file__), "firebase-credentials.json")
    has_file_creds = os.path.exists(cred_path_chk)
    if has_env_creds or has_file_creds:
        print("✅ Firebase credentials detected.")
    else:
        print("⚠️  Firebase credentials missing. Set FIREBASE_CREDENTIALS or add backend/firebase-credentials.json")

    # OpenAI (optional)
    if os.environ.get("OPENAI_API_KEY"):
        print("✅ OPENAI_API_KEY set: LLM recommendations enabled.")
    else:
        print("ℹ️  OPENAI_API_KEY not set: using dummy candidates.")

# ============================================================================
# VOCABULARY MAPPING: Android App Enums → Food Database Values
# ============================================================================

# Normalize Android enum values (UPPERCASE) to database values (lowercase)
DIETARY_RESTRICTION_MAP = {
    "VEGAN": "vegan",
    "VEGETARIAN": "vegetarian",
    "HALAL": "halal",
    "KOSHER": "kosher",
    "PESCATARIAN": "pescatarian",
    "GLUTEN_FREE": "gluten-free",
    "LACTOSE_FREE": "lactose-free"
}

ALLERGEN_MAP = {
    "EGGS": "eggs",
    "DAIRY": "dairy",
    "MILK": "dairy",  # Legacy support
    "FISH": "fish",
    "SHELLFISH": "shellfish",
    "PEANUTS": "nuts",  # Food DB doesn't distinguish peanuts from tree nuts
    "TREE_NUTS": "nuts",
    "SOY": "soy",
    "WHEAT": "wheat",
    "SESAME": "sesame"
}

# Global variable to hold loaded food database
FOOD_DATABASE = []

# ============================================================================
# FOOD DATABASE LOADING
# ============================================================================

def load_food_database():
    """Load food_dataset.json at server startup"""
    global FOOD_DATABASE
    food_db_path = os.path.join(os.path.dirname(__file__), "food_dataset.json")
    try:
        with open(food_db_path, 'r', encoding='utf-8') as f:
            FOOD_DATABASE = json.load(f)
        print(f"✅ Loaded {len(FOOD_DATABASE)} food items from database")
    except FileNotFoundError:
        print(f"⚠️  food_dataset.json not found at {food_db_path}")
        FOOD_DATABASE = []
    except Exception as e:
        print(f"❌ Error loading food database: {e}")
        FOOD_DATABASE = []

# ============================================================================
# GROUP CONSTRAINT BUILDING
# ============================================================================

def build_group_constraints(members_constraints: List[Dict]) -> Dict:
    """
    Build union of hard constraints across all team members.
    Returns normalized constraints for database filtering.

    Args:
        members_constraints: List of dicts with 'userId' and 'constraints' keys

    Returns:
        Dict with 'hard' (for filtering) and 'soft' (for LLM ranking) constraints
    """
    group_dietary_disallows = set()
    group_allergies = set()
    group_avoid_ingredients = set()

    # Collect soft preferences for LLM
    all_favorite_cuisines = []
    all_spice_tolerances = []

    for member in members_constraints:
        user_constraints = member.get('constraints', {})

        # Hard constraints - normalize to lowercase database format
        for restriction in user_constraints.get('dietaryRestrictions', []):
            normalized = DIETARY_RESTRICTION_MAP.get(restriction, restriction.lower())
            group_dietary_disallows.add(normalized)

        for allergy in user_constraints.get('allergies', []):
            normalized = ALLERGEN_MAP.get(allergy, allergy.lower())
            group_allergies.add(normalized)

        for ingredient in user_constraints.get('avoidIngredients', []):
            # Ingredients already lowercase in profiles
            group_avoid_ingredients.add(ingredient.lower())

        # Soft preferences
        for cuisine in user_constraints.get('favoriteCuisines', []):
            all_favorite_cuisines.append(cuisine.lower())

        spice = user_constraints.get('spiceTolerance', 'MEDIUM')
        all_spice_tolerances.append(spice)

    return {
        'hard': {
            'dietary_violations': list(group_dietary_disallows),
            'allergens': list(group_allergies),
            'ingredients': list(group_avoid_ingredients)
        },
        'soft': {
            'favorite_cuisines': all_favorite_cuisines,
            'spice_tolerances': all_spice_tolerances
        }
    }

# ============================================================================
# DATABASE FILTERING (HARD CONSTRAINTS)
# ============================================================================

def filter_foods_by_constraints(group_constraints: Dict, max_candidates: int = 200) -> List[Dict]:
    """
    Hard filter: Remove foods that violate ANY member's constraints.
    Uses set intersection logic: food must have ZERO overlap with group disallows.

    Args:
        group_constraints: Output from build_group_constraints()
        max_candidates: Maximum number of foods to return

    Returns:
        List of food items that pass all hard constraint filters
    """
    hard = group_constraints['hard']
    group_dietary_disallows = set(hard['dietary_violations'])
    group_allergies = set(hard['allergens'])
    group_avoid_ingredients = set(hard['ingredients'])

    filtered_foods = []

    for food in FOOD_DATABASE:
        # Check dietary violations (vegan, halal, etc.)
        food_violations = set(food.get('dietary_violations', []))
        if food_violations & group_dietary_disallows:  # Intersection - any overlap means exclude
            continue

        # Check allergens
        food_allergens = set(food.get('allergens', []))
        if food_allergens & group_allergies:
            continue

        # Check ingredients (more expensive, do last)
        food_ingredients = set(ing.lower() for ing in food.get('ingredients', []))
        if food_ingredients & group_avoid_ingredients:
            continue

        # Passed all filters
        filtered_foods.append(food)

        if len(filtered_foods) >= max_candidates:
            break

    return filtered_foods


def analyze_cuisine_compatibility(group_constraints: Dict) -> Dict[str, int]:
    """
    Analyze which cuisines have foods compatible with the group's constraints.

    Returns dict mapping cuisine name -> count of compatible foods
    """
    compatible_foods = filter_foods_by_constraints(group_constraints, max_candidates=1000)

    cuisine_counts = {}
    for food in compatible_foods:
        cuisine = food.get('cuisine', 'unknown')
        cuisine_counts[cuisine] = cuisine_counts.get(cuisine, 0) + 1

    return cuisine_counts


# ============================================================================
# LLM RANKING (SOFT PREFERENCES ONLY)
# ============================================================================

def filter_by_nutrition(foods: List[Dict], occasion: str) -> List[Dict]:
    """
    Filter/rank foods by nutrition keywords mentioned in occasion.
    E.g., "high protein" → rank foods by protein content (highest first)
          "low calorie" → rank foods by calories (lowest first)

    Args:
        foods: List of food items
        occasion: Occasion/poll title text

    Returns:
        List of foods sorted by nutrition criteria (or original if no nutrition request)
    """
    if not occasion:
        return foods

    occasion_lower = occasion.lower()

    # Detect nutrition-based requests
    nutrition_filters = {
        'high protein': ('protein', 'desc'),
        'low protein': ('protein', 'asc'),
        'high calorie': ('calories', 'desc'),
        'low calorie': ('calories', 'asc'),
        'low cal': ('calories', 'asc'),
        'diet': ('calories', 'asc'),
        'healthy': ('calories', 'asc'),
        'light': ('calories', 'asc'),
        'high carb': ('carbs', 'desc'),
        'low carb': ('carbs', 'asc'),
        'keto': ('carbs', 'asc'),
        'high fat': ('fat', 'desc'),
        'low fat': ('fat', 'asc')
    }

    # Find matching nutrition criteria
    nutrient = None
    direction = None
    matched_keyword = None

    for keyword, (nut, dir) in nutrition_filters.items():
        if keyword in occasion_lower:
            nutrient = nut
            direction = dir
            matched_keyword = keyword
            break

    # If no nutrition request found, return original list
    if not nutrient:
        return foods

    # Filter out foods without nutrition data
    foods_with_nutrition = [f for f in foods if f.get('nutrition') and f['nutrition'].get(nutrient) is not None]

    if not foods_with_nutrition:
        print(f"   ⚠️  No foods have {nutrient} data, returning all foods", flush=True)
        return foods

    # Sort by the nutrient
    reverse = (direction == 'desc')
    sorted_foods = sorted(foods_with_nutrition, key=lambda f: f['nutrition'].get(nutrient, 0), reverse=reverse)

    print(f"   🥗 Nutrition filter: '{matched_keyword}' detected", flush=True)
    print(f"   → Sorted {len(sorted_foods)} foods by {nutrient} ({'highest' if reverse else 'lowest'} first)", flush=True)

    # Show top 3 examples
    if len(sorted_foods) >= 3:
        for i, food in enumerate(sorted_foods[:3], 1):
            value = food['nutrition'].get(nutrient, 'N/A')
            print(f"      {i}. {food['name']}: {value}g {nutrient}", flush=True)

    return sorted_foods


def filter_by_meal_characteristics(foods: List[Dict], occasion: str) -> List[Dict]:
    """
    Filter foods by meal characteristics mentioned in occasion.
    E.g., "something heavy" → only heavy meals
          "soup-based food" → only soup-based meals

    Args:
        foods: List of food items
        occasion: Occasion/poll title text

    Returns:
        Filtered list of foods (or original list if no match)
    """
    if not occasion:
        return foods

    occasion_lower = occasion.lower()

    # Detect heaviness preferences
    heaviness_keywords = {
        'light': ['light', 'not too heavy', 'something light'],
        'medium': ['medium', 'moderate'],
        'heavy': ['heavy', 'filling', 'hearty', 'substantial']
    }

    # Detect meal-type preferences (matching all 10 database types)
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

    # Check for heaviness match
    matched_heaviness = None
    for heaviness, keywords in heaviness_keywords.items():
        if any(keyword in occasion_lower for keyword in keywords):
            matched_heaviness = heaviness
            break

    # Check for meal-type match
    matched_meal_type = None
    for meal_type, keywords in meal_type_keywords.items():
        if any(keyword in occasion_lower for keyword in keywords):
            matched_meal_type = meal_type
            break

    # If no match found, return original list
    if not matched_heaviness and not matched_meal_type:
        return foods

    # Filter by matched characteristics
    filtered = []
    for food in foods:
        # Check heaviness match
        if matched_heaviness:
            food_heaviness = food.get('heaviness', 'medium')
            if food_heaviness != matched_heaviness:
                continue

        # Check meal-type match
        if matched_meal_type:
            food_meal_type = food.get('meal_type', '')
            if food_meal_type != matched_meal_type:
                continue

        filtered.append(food)

    if filtered:
        characteristics = []
        if matched_heaviness:
            characteristics.append(f"heaviness={matched_heaviness}")
        if matched_meal_type:
            characteristics.append(f"meal_type={matched_meal_type}")

        print(f"   🍱 Meal characteristic filter: {', '.join(characteristics)}", flush=True)
        print(f"   → Filtered from {len(foods)} to {len(filtered)} foods", flush=True)
        return filtered
    else:
        print(f"   ⚠️  No foods match the requested characteristics, returning all foods", flush=True)
        return foods


def filter_by_occasion_ingredient(foods: List[Dict], occasion: str) -> List[Dict]:
    """
    Filter foods by ingredient keywords mentioned in occasion.
    E.g., "recommend tofu dishes" → only return foods with tofu in ingredients

    Args:
        foods: List of food items
        occasion: Occasion/poll title text

    Returns:
        Filtered list of foods (or original list if no ingredient match)
    """
    if not occasion:
        return foods

    occasion_lower = occasion.lower()

    # Common ingredient keywords to detect
    ingredient_keywords = [
        'tofu', 'chicken', 'beef', 'pork', 'fish', 'shrimp', 'salmon',
        'egg', 'cheese', 'mushroom', 'noodle', 'rice', 'pasta', 'kimchi',
        'seaweed', 'avocado', 'tomato', 'potato', 'spinach', 'broccoli'
    ]

    # Check if occasion mentions any specific ingredient
    mentioned_ingredients = []
    for keyword in ingredient_keywords:
        if keyword in occasion_lower:
            mentioned_ingredients.append(keyword)

    # If no specific ingredient mentioned, return all foods
    if not mentioned_ingredients:
        return foods

    # Filter foods to only include those with the mentioned ingredients
    filtered = []
    for food in foods:
        food_ingredients_lower = [ing.lower() for ing in food.get('ingredients', [])]

        # Check if food contains ANY of the mentioned ingredients
        if any(ingredient in ' '.join(food_ingredients_lower) for ingredient in mentioned_ingredients):
            filtered.append(food)

    print(f"   🔍 Occasion ingredient filter: Found '{', '.join(mentioned_ingredients)}' in occasion", flush=True)
    print(f"   → Filtered from {len(foods)} to {len(filtered)} foods containing those ingredients", flush=True)

    # If no foods match, return original list (LLM will do its best)
    if not filtered:
        print(f"   ⚠️  No foods contain {mentioned_ingredients}, returning all foods", flush=True)
        return foods

    return filtered


def rank_foods_with_llm(
    filtered_foods: List[Dict],
    group_constraints: Dict,
    occasion: str = None,
    top_k: int = 15
) -> List[Dict]:
    """
    LLM ranks pre-filtered foods by soft preferences ONLY.
    Hard constraints have already been applied by filter_foods_by_constraints().

    Args:
        filtered_foods: List of foods that passed hard constraint filtering
        group_constraints: Output from build_group_constraints()
        occasion: Optional occasion/poll title for context
        top_k: Number of top-ranked foods to return

    Returns:
        List of food dicts with added 'ranking' field (0-indexed, lower is better)
    """
    if not filtered_foods:
        return []

    # Apply nutrition-based sorting FIRST (if applicable)
    filtered_foods = filter_by_nutrition(filtered_foods, occasion)

    # Apply meal characteristic filtering (heaviness, meal-type)
    filtered_foods = filter_by_meal_characteristics(filtered_foods, occasion)

    # Apply occasion-based ingredient filtering (if applicable)
    filtered_foods = filter_by_occasion_ingredient(filtered_foods, occasion)

    soft = group_constraints['soft']

    # Build compact food list for LLM (reduce token usage)
    food_summaries = []
    for food in filtered_foods[:50]:  # Limit to 50 foods for token budget
        summary = {
            'food_id': food['food_id'],
            'name': food['name'],
            'cuisine': food['cuisine'],
            'spice_level': food.get('spice_level', 0),
            'heaviness': food.get('heaviness', 'medium'),
            'meal_type': food.get('meal_type', 'unknown'),
            'description': food.get('description', ''),
            'ingredients': food.get('ingredients', []),
            'allergens': food.get('allergens', []),
            'dietary_violations': food.get('dietary_violations', []),
            'nutrition': food.get('nutrition', {})
        }
        food_summaries.append(summary)

    # Build soft preference summary
    cuisine_counts = {}
    for cuisine in soft['favorite_cuisines']:
        cuisine_counts[cuisine] = cuisine_counts.get(cuisine, 0) + 1

    # Calculate average spice tolerance
    avg_spice_map = {'MILD': 1, 'MEDIUM': 2, 'SPICY': 3}
    spice_scores = [avg_spice_map.get(s.upper(), 2) for s in soft['spice_tolerances']]
    avg_spice_tolerance = sum(spice_scores) / len(spice_scores) if spice_scores else 2

    # Get hard constraints for LLM context
    hard = group_constraints['hard']

    # Build LLM prompt
    prompt = f"""You are a meal recommendation assistant. Rank the following {len(food_summaries)} meals based on how well they match the group's preferences.

IMPORTANT: All meals have ALREADY been filtered to satisfy hard constraints. However, the constraint information is provided for your awareness.

## Group Hard Constraints (ALREADY FILTERED):
- Dietary restrictions to avoid: {hard['dietary_violations'] or 'None'}
- Allergens to avoid: {hard['allergens'] or 'None'}
- Ingredients to avoid: {hard['ingredients'] or 'None'}

## Group Soft Preferences:
- Favorite cuisines: {dict(cuisine_counts)} (higher count = more members prefer it)
- Average spice tolerance: {avg_spice_tolerance:.1f}/3 (1=mild, 2=medium, 3=spicy)
"""

    if occasion:
        prompt += f"- Occasion note: \"{occasion}\"\n"

    prompt += f"\n## Candidate Meals (ALREADY HARD-FILTERED - ALL ARE SAFE):\n"
    for i, food in enumerate(food_summaries, 1):
        ingredients_str = ', '.join(food['ingredients'][:6]) if food['ingredients'] else 'N/A'
        allergens_str = ', '.join(food['allergens']) if food['allergens'] else 'none'
        violations_str = ', '.join(food['dietary_violations']) if food['dietary_violations'] else 'none'

        # Format nutrition info
        nutrition = food['nutrition']
        nutrition_str = f"{nutrition.get('calories', 'N/A')}cal" if nutrition else 'N/A'

        prompt += f"{i}. {food['name']} ({food['food_id']}) - {food['cuisine']}, spice {food['spice_level']}/4, {food['heaviness']}, {nutrition_str}\n"
        prompt += f"   Ingredients: [{ingredients_str}] | Allergens: [{allergens_str}] | Dietary: [{violations_str}]\n"

    prompt += f"""
## Instructions:
**NOTE**: If occasion mentions specific ingredients (e.g., "tofu") or nutrition goals (e.g., "high protein"), foods have ALREADY been filtered/sorted accordingly. You just need to rank these pre-filtered results.

1. Match favorite cuisines (weighted by member count) - this is your PRIMARY ranking factor
2. Consider spice level compatibility (don't recommend spice 4/4 if tolerance is 1/3)
3. Use other occasion context if provided (e.g., "kids" → prefer milder options, "quick" → prefer lighter meals)
4. Consider meal balance and variety in your ranking
5. ALL foods are safe to recommend (already filtered for dietary restrictions, allergies, and specific ingredients/nutrition)
6. Return top {top_k} meal IDs in ranked order (best match first)

Return ONLY valid JSON with this exact format:
{{"ranked_food_ids": ["F001", "F023", "F117"]}}
"""

    # Call OpenAI API
    try:
        api_key = os.environ.get("OPENAI_API_KEY")
        if not api_key:
            print("⚠️  No OpenAI API key - using fallback ranking")
            raise Exception("No API key")

        client = openai.OpenAI(api_key=api_key)
        response = client.chat.completions.create(
            model="gpt-5.1",
            messages=[
                {"role": "system", "content": "You rank meals by soft preferences. Return only valid JSON."},
                {"role": "user", "content": prompt}
            ],
            max_tokens=300,
            temperature=0.7
        )

        result_text = response.choices[0].message.content.strip()
        result = json.loads(result_text)
        ranked_ids = result.get('ranked_food_ids', [])

        print(f"✅ LLM ranked {len(ranked_ids)} foods")

        # Map back to full food objects with ranking
        ranked_foods = []
        id_to_food = {f['food_id']: f for f in filtered_foods}

        for rank, food_id in enumerate(ranked_ids[:top_k]):
            if food_id in id_to_food:
                food = id_to_food[food_id].copy()
                food['ranking'] = rank
                ranked_foods.append(food)

        # If LLM didn't return enough, pad with remaining filtered foods
        if len(ranked_foods) < top_k:
            existing_ids = {f['food_id'] for f in ranked_foods}
            for food in filtered_foods:
                if food['food_id'] not in existing_ids:
                    ranked_foods.append({**food, 'ranking': len(ranked_foods)})
                    if len(ranked_foods) >= top_k:
                        break

        return ranked_foods

    except Exception as e:
        print(f"⚠️  LLM ranking failed ({e}), using fallback ranking")
        # Fallback: return first N filtered foods with sequential ranking
        return [
            {**food, 'ranking': i}
            for i, food in enumerate(filtered_foods[:top_k])
        ]

# ============================================================================
# LEGACY FUNCTION (KEPT FOR BACKWARDS COMPATIBILITY - NOW USES NEW FLOW)
# ============================================================================

# Helper function to generate candidates using DATABASE-FIRST flow
def generate_candidates_for_team(team_name: str, members_constraints: List[Dict[str, Any]], num_candidates: int = 15, occasion: str = None) -> List[Dict[str, Any]]:
    """
    Generate meal candidates using DATABASE-FIRST filtering + LLM ranking.

    New Flow (v2.0):
    1. Build group constraints (union of all members' hard constraints)
    2. Filter food database by hard constraints (dietary violations, allergens, ingredients)
    3. LLM ranks filtered foods by soft preferences (cuisines, spice tolerance, occasion)
    4. Return top N ranked foods with names and rankings

    Args:
        team_name: Name of the team
        members_constraints: List of dicts with 'userId' and 'constraints' keys
        num_candidates: Number of candidates to generate (default 15 for two-phase voting)
        occasion: Optional poll title/occasion (e.g., "Korean food", "Italian restaurant")

    Returns:
        List of dicts with 'name' and 'ranking' keys (ranking is 0-indexed, lower is better)
    """
    print(f"\n{'='*60}", flush=True)
    print(f"🍽️  GENERATING CANDIDATES - DATABASE-FIRST FLOW", flush=True)
    print(f"   Team: '{team_name}' ({len(members_constraints)} members)", flush=True)
    if occasion:
        print(f"   Occasion: '{occasion}'", flush=True)
    print(f"{'='*60}", flush=True)

    try:
        # STEP 1: Build group constraints (union of all members)
        print(f"📊 Step 1: Building group constraints...", flush=True)
        group_constraints = build_group_constraints(members_constraints)

        hard = group_constraints['hard']
        soft = group_constraints['soft']

        print(f"   Hard constraints (MUST avoid):", flush=True)
        print(f"     - Dietary violations: {hard['dietary_violations'] or 'None'}", flush=True)
        print(f"     - Allergens: {hard['allergens'] or 'None'}", flush=True)
        print(f"     - Ingredients: {hard['ingredients'] or 'None'}", flush=True)
        print(f"   Soft preferences (for ranking):", flush=True)
        print(f"     - Favorite cuisines: {soft['favorite_cuisines'] or 'None'}", flush=True)
        print(f"     - Spice tolerances: {soft['spice_tolerances']}", flush=True)

        # STEP 2: Filter food database by hard constraints
        print(f"\n🔍 Step 2: Filtering food database by hard constraints...", flush=True)
        print(f"   Total foods in database: {len(FOOD_DATABASE)}", flush=True)

        filtered_foods = filter_foods_by_constraints(group_constraints, max_candidates=200)

        print(f"   ✅ Filtered to {len(filtered_foods)} foods that satisfy all hard constraints", flush=True)

        # Handle empty filter result
        if not filtered_foods:
            print(f"   ❌ NO FOODS match all constraints!", flush=True)
            print(f"   This means the combination of constraints is too restrictive.", flush=True)
            # Return error info for caller to handle
            return []

        # STEP 2.5: Check if requested cuisine is compatible (if occasion mentions a cuisine)
        requested_cuisine = None
        if occasion:
            occasion_lower = occasion.lower()
            # Check for common cuisine keywords
            cuisine_keywords = {
                'korean': 'korean',
                'japanese': 'japanese',
                'chinese': 'chinese',
                'italian': 'western',
                'mexican': 'mexican',
                'thai': 'southeast asian',
                'vietnamese': 'southeast asian'
            }
            for keyword, cuisine in cuisine_keywords.items():
                if keyword in occasion_lower:
                    requested_cuisine = cuisine
                    break

        # Analyze cuisine compatibility
        cuisine_counts = analyze_cuisine_compatibility(group_constraints)
        print(f"\n📊 Cuisine compatibility analysis:", flush=True)
        for cuisine, count in sorted(cuisine_counts.items(), key=lambda x: x[1], reverse=True):
            print(f"   - {cuisine}: {count} compatible foods", flush=True)

        # Warn if requested cuisine has no matches
        if requested_cuisine and cuisine_counts.get(requested_cuisine, 0) == 0:
            print(f"\n⚠️  WARNING: Requested cuisine '{requested_cuisine}' has NO compatible foods!", flush=True)
            print(f"   Your constraints (vegan + allergens) exclude all {requested_cuisine} foods in database.", flush=True)
            compatible_cuisines = [c for c, count in cuisine_counts.items() if count > 0]
            if compatible_cuisines:
                print(f"   ✅ Compatible cuisines: {', '.join(compatible_cuisines)}", flush=True)
            print(f"   → Proceeding with all {len(filtered_foods)} compatible foods from any cuisine...", flush=True)

        # STEP 3: LLM ranks filtered foods by soft preferences
        print(f"\n🤖 Step 3: LLM ranking filtered foods by soft preferences...", flush=True)

        ranked_foods = rank_foods_with_llm(
            filtered_foods=filtered_foods,
            group_constraints=group_constraints,
            occasion=occasion,
            top_k=num_candidates
        )

        # Convert to expected format (name + ranking)
        candidates = [
            {
                "name": food['name'],
                "ranking": food['ranking'],
                "food_id": food.get('food_id', ''),  # Include food_id for reference
                "cuisine": food.get('cuisine', ''),
                "spice_level": food.get('spice_level', 0)
            }
            for food in ranked_foods
        ]

        print(f"\n🎯 Final {len(candidates)} candidates:", flush=True)
        for i, c in enumerate(candidates[:5], 1):
            print(f"   {i}. {c['name']} ({c['cuisine']}, spice {c['spice_level']}/4)", flush=True)
        if len(candidates) > 5:
            print(f"   ... and {len(candidates) - 5} more", flush=True)

        print(f"{'='*60}\n", flush=True)

        return candidates

    except Exception as e:
        print(f"\n❌ Error in generate_candidates_for_team: {e}", flush=True)
        import traceback
        traceback.print_exc()

        # Fallback: return some foods from database without filtering
        print(f"📦 Using fallback: returning first {num_candidates} foods from database", flush=True)
        if FOOD_DATABASE:
            fallback = [
                {
                    "name": food['name'],
                    "ranking": i,
                    "food_id": food.get('food_id', ''),
                    "cuisine": food.get('cuisine', ''),
                    "spice_level": food.get('spice_level', 0)
                }
                for i, food in enumerate(FOOD_DATABASE[:num_candidates])
            ]
            return fallback
        else:
            # Ultimate fallback if database not loaded
            print(f"⚠️  Food database not loaded! Returning empty list.", flush=True)
            return []



@app.route("/test-llm", methods=["GET"])
def test_llm():
    """Test endpoint to verify OpenAI is working"""
    occasion = request.args.get("occasion", "Korean food")

    print(f"\n{'='*60}", flush=True)
    print(f"🧪 LLM TEST ENDPOINT", flush=True)
    print(f"{'='*60}", flush=True)

    try:
        candidates = generate_candidates_for_team(
            team_name="Test Team",
            members_constraints=[],
            num_candidates=10,
            occasion=occasion
        )

        print(f"{'='*60}", flush=True)
        print(f"✅ TEST COMPLETED SUCCESSFULLY", flush=True)
        print(f"{'='*60}\n", flush=True)

        return jsonify({
            "success": True,
            "occasion": occasion,
            "candidates": [c["name"] for c in candidates],
            "count": len(candidates),
            "message": "OpenAI is working! Check backend logs for details."
        }), 200
    except Exception as e:
        print(f"{'='*60}", flush=True)
        print(f"❌ TEST FAILED: {e}", flush=True)
        print(f"{'='*60}\n", flush=True)

        return jsonify({
            "success": False,
            "error": str(e),
            "message": "OpenAI test failed. Check backend logs for details."
        }), 500


@app.route("/check-email", methods=["POST"])
def check_email():
    data = request.get_json(force=True)
    email = data.get("email", "").strip().lower()
    if not email:
        return jsonify({"error": "Email required"}), 400
    try:
        user = auth.get_user_by_email(email)
        return jsonify({"exists": True, "uid": user.uid}), 200
    except firebase_admin._auth_utils.UserNotFoundError:
        return jsonify({"exists": False}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/polls/start", methods=["POST"])
def start_poll():
    """
    Create and start a new poll for a team.
    
    Request body:
    {
        "teamId": "swpp5",
        "pollTitle": "10/25 team dinner",
        "durationMinutes": 3
    }
    
    Returns:
    {
        "pollId": "abc123",
        "pollTitle": "10/25 team dinner",
        "teamName": "swpp 5",
        "duration": 3,
        "startedTime": "2025-10-26T11:00:00Z",
        "candidates": ["Bibimbap", "Vegan Burger", ...]
    }
    """
    data = request.get_json(force=True)
    team_id = data.get("teamId")
    poll_title = data.get("pollTitle")
    occasion_note = data.get("occasionNote")  # Additional field from Android app
    duration_minutes = data.get("durationMinutes")

    if not team_id or not poll_title or not duration_minutes:
        return jsonify({"error": "Missing required fields"}), 400

    # Use occasionNote if provided, otherwise fall back to pollTitle
    occasion_for_llm = occasion_note if occasion_note else poll_title
    print(f"📝 Poll request - Title: '{poll_title}', Occasion: '{occasion_for_llm}'", flush=True)
    
    try:
        # Check if there's already an active poll for this team
        team_ref = db.collection("teams").document(team_id)
        team_doc = team_ref.get()
        
        if not team_doc.exists:
            return jsonify({"error": "Team not found"}), 404
        
        team_data = team_doc.to_dict()
        currently_open_poll = team_data.get("currentlyOpenPoll")
        
        if currently_open_poll:
            # Check if the poll is still active; if expired, auto-close here too
            poll_ref = db.collection("polls").document(currently_open_poll)
            poll_doc = poll_ref.get()
            
            if poll_doc.exists:
                existing_poll = poll_doc.to_dict()
                if existing_poll.get("status") == "active":
                    started_time = existing_poll.get("startedTime")
                    duration_minutes = existing_poll.get("duration", 0)
                    if hasattr(started_time, 'timestamp'):
                        started_dt = datetime.utcfromtimestamp(started_time.timestamp())
                    else:
                        started_dt = started_time
                    elapsed_seconds = (datetime.utcnow() - started_dt).total_seconds()
                    if elapsed_seconds >= duration_minutes * 60:
                        # Expired: close now and continue starting a new poll
                        close_poll_internal(currently_open_poll)
                        team_ref.update({"currentlyOpenPoll": None})
                    else:
                        return jsonify({"error": "Poll already active for this team"}), 400
        
        # Get team members and their constraints
        members = team_data.get("members", [])
        members_constraints = []
        
        for user_id in members:
            user_ref = db.collection("users").document(user_id)
            user_doc = user_ref.get()

            if user_doc.exists:
                user_data = user_doc.to_dict()

                # Map Firestore fields (from Android app) to backend constraint format
                # Android app stores: dietaryRestrictions, allergies, avoidIngredients, favoriteCuisines, spiceTolerance
                constraints = {
                    # Hard constraints (MUST avoid)
                    "dietaryRestrictions": user_data.get("dietaryRestrictions", []),  # e.g., ["VEGAN", "VEGETARIAN"]
                    "allergies": user_data.get("allergies", []),  # e.g., ["PEANUTS", "SHELLFISH"]
                    "avoidIngredients": user_data.get("avoidIngredients", []),  # e.g., ["beef", "pork"]

                    # Soft preferences (nice to have)
                    "favoriteCuisines": user_data.get("favoriteCuisines", []),  # e.g., ["KOREAN", "ITALIAN"]
                    "spiceTolerance": user_data.get("spiceTolerance", "MEDIUM"),  # e.g., "MILD", "MEDIUM", "SPICY"
                }

                members_constraints.append({
                    "userId": user_id,
                    "constraints": constraints
                })
        
        # Generate candidates (10-15 for two-phase voting)
        print(f"📋 About to generate candidates for poll: '{poll_title}' with occasion: '{occasion_for_llm}'", flush=True)
        all_candidates_data = generate_candidates_for_team(
            team_name=team_data.get("teamName", ""),
            members_constraints=members_constraints,
            num_candidates=15,
            occasion=occasion_for_llm  # Use occasionNote if provided, else pollTitle
        )
        print(f"✅ Generated {len(all_candidates_data)} candidates", flush=True)

        # Extract just the names for initial display (first 5)
        visible_candidates = [c["name"] for c in all_candidates_data[:5]]

        # Create poll document with phase support
        started_time = datetime.utcnow()
        poll_data = {
            "pollTitle": poll_title,
            "startedTime": started_time,
            "duration": duration_minutes,
            "teamId": team_id,
            "teamName": team_data.get("teamName", ""),
            # Two-phase voting fields
            "phase": "phase1",
            "allCandidates": all_candidates_data,  # Full list with rankings
            "visibleCandidates": visible_candidates,  # First 5 shown
            "removedCandidates": [],  # Track globally rejected candidates
            "phase1Votes": {},  # {userId: {"approved": [...], "rejected": str|None}}
            "phase2Votes": {},  # {userId: selectedCandidate}
            "phase2Candidates": [],  # Top 3 from Phase 1
            "lockedInUsers": [],  # Users who locked in votes
            # Legacy fields for backward compatibility
            "candidates": visible_candidates,
            "votes": {},
            "status": "active",
            "resultRanking": []
        }
        
        poll_ref = db.collection("polls").document()
        poll_id = poll_ref.id
        poll_ref.set(poll_data)
        
        # Update team's currentlyOpenPoll
        team_ref.update({"currentlyOpenPoll": poll_id})
        
        return jsonify({
            "pollId": poll_id,
            "pollTitle": poll_title,
            "teamName": team_data.get("teamName", ""),
            "duration": duration_minutes,
            "startedTime": started_time.isoformat() + "Z",
            "candidates": visible_candidates  # Just the first 5 for initial display
        }), 200
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/polls/<poll_id>", methods=["GET"])
def get_poll(poll_id):
    """
    Get poll state (for voting session screen or closed screen).
    Handles both two-phase and legacy single-phase polls.

    Returns phase-specific poll state with remaining time and current votes,
    or closed poll state with results.
    """
    # Get current user ID from request
    user_id = request.headers.get("X-User-Id") or "demo_user"

    try:
        poll_ref = db.collection("polls").document(poll_id)
        poll_doc = poll_ref.get()

        if not poll_doc.exists:
            return jsonify({"error": "Poll not found"}), 404

        poll_data = poll_doc.to_dict()

        # Get team data for member count
        team_id = poll_data["teamId"]
        team_ref = db.collection("teams").document(team_id)
        team_doc = team_ref.get()
        team_data = team_doc.to_dict() if team_doc.exists else {}
        members = team_data.get("members", [])

        # Calculate remaining time
        started_time = poll_data["startedTime"]
        duration_minutes = poll_data["duration"]

        # Convert Firestore Timestamp to UTC datetime
        if hasattr(started_time, 'timestamp'):
            started_dt = datetime.utcfromtimestamp(started_time.timestamp())
        else:
            started_dt = started_time

        elapsed_seconds = (datetime.utcnow() - started_dt).total_seconds()
        seconds_left = (duration_minutes * 60) - elapsed_seconds
        remaining_seconds = max(0, seconds_left)

        # Check if this is a two-phase poll
        is_two_phase = "phase" in poll_data
        current_phase = poll_data.get("phase", "active")

        # Auto-close if expired (but not during active two-phase voting)
        # For two-phase polls, only close on timeout if we're past a grace period
        # to allow members to complete Phase 2 voting after Phase 1 ends
        should_auto_close = False
        if seconds_left <= 0 and poll_data["status"] == "active" and current_phase != "closed":
            if is_two_phase and current_phase == "phase2":
                # In Phase 2: only close if all members locked in or significant timeout
                # This prevents premature closure during Phase 1 → Phase 2 transition
                locked_in_users = poll_data.get("lockedInUsers", [])
                if len(locked_in_users) >= len(members):
                    # All members voted in Phase 2, safe to close
                    should_auto_close = True
                elif seconds_left < -30:
                    # 30 second grace period expired, force close
                    should_auto_close = True
            else:
                # Not in Phase 2 or not two-phase: normal auto-close
                should_auto_close = True

        if should_auto_close:
            poll_data = close_poll_internal(poll_id)
            current_phase = "closed"

        # Prepare response based on phase
        if current_phase == "closed" or poll_data["status"] == "closed":
            # Closed poll: return results
            result_ranking = poll_data.get("resultRanking", [])

            # Build results with vote counts
            phase2_votes = poll_data.get("phase2Votes", {})
            vote_counts = {}
            for candidate in phase2_votes.values():
                vote_counts[candidate] = vote_counts.get(candidate, 0) + 1

            results = []
            for candidate_name in result_ranking:
                results.append({
                    "name": candidate_name,
                    "voteCount": vote_counts.get(candidate_name, 0)
                })

            return jsonify({
                "pollId": poll_id,
                "pollTitle": poll_data.get("pollTitle", ""),
                "teamId": team_id,
                "teamName": poll_data.get("teamName", ""),
                "phase": "closed",
                "status": "closed",
                "results": results,
                "winner": result_ranking[0] if result_ranking else None
            }), 200

        elif is_two_phase and current_phase == "phase1":
            # Phase 1: Approval voting
            visible_candidates = poll_data.get("visibleCandidates", [])
            phase1_votes = poll_data.get("phase1Votes", {})
            locked_in_users = poll_data.get("lockedInUsers", [])

            user_vote = phase1_votes.get(user_id, {})
            approved = user_vote.get("approved", [])
            rejected = user_vote.get("rejected")

            return jsonify({
                "pollId": poll_id,
                "pollTitle": poll_data["pollTitle"],
                "teamId": team_id,
                "teamName": poll_data["teamName"],
                "phase": "phase1",
                "status": "active",
                "startedTime": started_dt.isoformat() + "Z",
                "duration": duration_minutes,
                "remainingSeconds": int(remaining_seconds),
                "candidates": [{"name": candidate} for candidate in visible_candidates],
                "yourApprovedCandidates": approved,
                "yourRejectedCandidate": rejected,
                "hasCurrentUserLockedIn": user_id in locked_in_users,
                "lockedInUserCount": len(locked_in_users),
                "totalMemberCount": len(members)
            }), 200

        elif is_two_phase and current_phase == "phase2":
            # Phase 2: Single selection from Top 3
            phase2_candidates = poll_data.get("phase2Candidates", [])
            phase2_votes = poll_data.get("phase2Votes", {})
            locked_in_users = poll_data.get("lockedInUsers", [])

            user_selection = phase2_votes.get(user_id)

            return jsonify({
                "pollId": poll_id,
                "pollTitle": poll_data["pollTitle"],
                "teamId": team_id,
                "teamName": poll_data["teamName"],
                "phase": "phase2",
                "status": "active",
                "startedTime": started_dt.isoformat() + "Z",
                "duration": duration_minutes,
                "remainingSeconds": int(remaining_seconds),
                "candidates": [{"name": candidate} for candidate in phase2_candidates],
                "yourSelectedCandidate": user_selection,
                "hasCurrentUserLockedIn": user_id in locked_in_users,
                "lockedInUserCount": len(locked_in_users),
                "totalMemberCount": len(members)
            }), 200

        else:
            # Legacy single-phase voting
            votes = poll_data.get("votes", {})
            current_votes = votes.get(user_id, [])

            return jsonify({
                "pollId": poll_id,
                "pollTitle": poll_data["pollTitle"],
                "teamId": poll_data["teamId"],
                "teamName": poll_data["teamName"],
                "status": poll_data["status"],
                "startedTime": started_dt.isoformat() + "Z",
                "duration": duration_minutes,
                "remainingSeconds": int(remaining_seconds),
                "candidates": [
                    {"name": candidate}
                    for candidate in poll_data["candidates"]
                ],
                "yourCurrentVotes": current_votes,
                "totalSelectedCountForYou": len(current_votes)
            }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/polls/<poll_id>/vote", methods=["POST"])
def cast_vote(poll_id):
    """
    Cast or update vote for a poll.
    
    Request body:
    {
        "choices": ["Bibimbap", "Naengmyeon"]
    }
    
    Empty array means cancel vote.
    """
    data = request.get_json(force=True)
    choices = data.get("choices", [])
    
    # Get current user ID from request
    user_id = request.headers.get("X-User-Id") or "demo_user"
    
    try:
        poll_ref = db.collection("polls").document(poll_id)
        poll_doc = poll_ref.get()
        
        if not poll_doc.exists:
            return jsonify({"error": "Poll not found"}), 404
        
        poll_data = poll_doc.to_dict()
        
        # Verify the user is a member of the team
        team_id = poll_data["teamId"]
        team_ref = db.collection("teams").document(team_id)
        team_doc = team_ref.get()
        
        if not team_doc.exists:
            return jsonify({"error": "Team not found"}), 404
        
        team_data = team_doc.to_dict()
        members = team_data.get("members", [])
        
        if user_id not in members:
            return jsonify({"error": "User is not a member of this team"}), 403
        
        # Check if poll is active
        if poll_data["status"] != "active":
            # Try to close if expired
            if poll_data["status"] == "active":
                poll_data = close_poll_internal(poll_id)
            
            # If still closed or closing failed, return error
            if poll_data["status"] != "active":
                return jsonify({
                    "pollId": poll_id,
                    "status": poll_data["status"],
                    "resultRanking": [
                        {"rank": idx + 1, "name": candidate}
                        for idx, candidate in enumerate(poll_data.get("resultRanking", []))
                    ]
                }), 400
        
        # Validate choices are valid candidates
        candidates = poll_data["candidates"]
        for choice in choices:
            if choice not in candidates:
                return jsonify({"error": f"Invalid choice: {choice}"}), 400
        
        # Update votes
        votes = poll_data.get("votes", {})
        votes[user_id] = choices
        
        poll_ref.update({"votes": votes})
        
        return jsonify({
            "ok": True,
            "yourCurrentVotes": choices,
            "totalSelectedCountForYou": len(choices)
        }), 200
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/polls/<poll_id>/phase1-vote", methods=["POST"])
def cast_phase1_vote(poll_id):
    """
    Cast Phase 1 vote: approval voting + optional rejection.
    Uses Firestore transaction for atomic updates and concurrent safety.

    Request body:
    {
        "approvedCandidates": ["Bibimbap", "Vegan Burger"],
        "rejectedCandidate": "Tonkotsu Ramen"  // optional, one-time only per user
    }
    """
    data = request.get_json(force=True)
    approved_candidates = data.get("approvedCandidates", [])
    rejected_candidate = data.get("rejectedCandidate")

    # Get current user ID from request
    user_id = request.headers.get("X-User-Id") or "demo_user"

    try:
        poll_ref = db.collection("polls").document(poll_id)

        # Pre-validate team membership (outside transaction)
        poll_doc = poll_ref.get()
        if not poll_doc.exists:
            return jsonify({"error": "Poll not found"}), 404

        poll_data_check = poll_doc.to_dict()
        team_id = poll_data_check["teamId"]
        team_doc = db.collection("teams").document(team_id).get()

        if not team_doc.exists:
            return jsonify({"error": "Team not found"}), 404

        members = team_doc.to_dict().get("members", [])
        if user_id not in members:
            return jsonify({"error": "User is not a member of this team"}), 403

        # Use transaction for atomic vote + replacement
        @firestore.transactional
        def update_vote_in_transaction(transaction, poll_ref, user_id, approved_candidates, rejected_candidate, members):
            # Read current poll state
            poll_snapshot = poll_ref.get(transaction=transaction)
            if not poll_snapshot.exists:
                raise ValueError("Poll not found")

            poll_data = poll_snapshot.to_dict()

            # Verify poll is in Phase 1
            if poll_data.get("phase") != "phase1":
                raise ValueError(f"Poll is not in Phase 1 (currently in {poll_data.get('phase')})")

            visible_candidates = poll_data.get("visibleCandidates", [])
            all_candidates = poll_data.get("allCandidates", [])
            phase1_votes = poll_data.get("phase1Votes", {})
            locked_in_users = poll_data.get("lockedInUsers", [])
            removed_candidates = poll_data.get("removedCandidates", [])  # Track globally removed

            # Validate approved candidates exist in visible list
            for candidate in approved_candidates:
                if candidate not in visible_candidates:
                    raise ValueError(f"Invalid candidate: {candidate}")

            # Validate rejected candidate
            if rejected_candidate and rejected_candidate not in visible_candidates:
                raise ValueError(f"Invalid rejected candidate: {rejected_candidate}")

            # Check if user already used their one-time reject
            previous_vote = phase1_votes.get(user_id, {})
            previous_rejection = previous_vote.get("rejected")

            if previous_rejection and rejected_candidate and rejected_candidate != previous_rejection:
                raise ValueError("You have already used your one-time reject on a different menu")

            # Store the vote
            phase1_votes[user_id] = {
                "approved": approved_candidates,
                "rejected": rejected_candidate
            }

            # Handle rejection and replacement (Problem 2 fix)
            replacement_candidate = None
            if rejected_candidate and rejected_candidate not in removed_candidates:
                # Remove from visible
                if rejected_candidate in visible_candidates:
                    visible_candidates.remove(rejected_candidate)

                # Mark as removed globally
                if rejected_candidate not in removed_candidates:
                    removed_candidates.append(rejected_candidate)

                # Find replacement from allCandidates pool (keep 5 visible)
                # Get candidates not already visible or removed, sorted by ranking
                available_candidates = [
                    c for c in all_candidates
                    if c["name"] not in visible_candidates and c["name"] not in removed_candidates
                ]

                if available_candidates and len(visible_candidates) < 5:
                    # Sort by ranking (lowest/best first)
                    available_candidates.sort(key=lambda x: x.get("ranking", 999))
                    replacement_candidate = available_candidates[0]["name"]
                    visible_candidates.append(replacement_candidate)

            # Add user to locked-in list (avoid duplicates with set logic)
            if user_id not in locked_in_users:
                locked_in_users.append(user_id)

            # Update poll document atomically
            update_data = {
                "phase1Votes": phase1_votes,
                "lockedInUsers": locked_in_users,
                "visibleCandidates": visible_candidates,
                "removedCandidates": removed_candidates
            }

            transaction.update(poll_ref, update_data)

            return {
                "locked_in_count": len(locked_in_users),
                "total_members": len(members),
                "visible_candidates": visible_candidates,
                "replacement": replacement_candidate,
                "approved": approved_candidates
            }

        # Execute transaction
        transaction = db.transaction()
        result = update_vote_in_transaction(
            transaction, poll_ref, user_id, approved_candidates, rejected_candidate, members
        )

        # Check if all members have locked in → transition to Phase 2
        if result["locked_in_count"] >= result["total_members"]:
            print(f"All {result['total_members']} members locked in Phase 1 - transitioning to Phase 2", flush=True)
            transition_phase1_to_phase2(poll_id)

        return jsonify({
            "ok": True,
            "yourCurrentVotes": result["approved"],
            "totalSelectedCountForYou": len(result["approved"]),
            "visibleCandidates": result["visible_candidates"],
            "replacementCandidate": result["replacement"],
            "lockedInUserCount": result["locked_in_count"],
            "totalMemberCount": result["total_members"]
        }), 200

    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"Error in cast_phase1_vote: {e}", flush=True)
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500


@app.route("/polls/<poll_id>/phase2-vote", methods=["POST"])
def cast_phase2_vote(poll_id):
    """
    Cast Phase 2 vote: single selection from Top 3.
    Uses Firestore transaction for atomic updates and concurrent safety.

    Request body:
    {
        "selectedCandidate": "Bibimbap"
    }
    """
    data = request.get_json(force=True)
    selected_candidate = data.get("selectedCandidate")

    # Get current user ID from request
    user_id = request.headers.get("X-User-Id") or "demo_user"

    try:
        poll_ref = db.collection("polls").document(poll_id)

        # Pre-validate team membership (outside transaction)
        poll_doc = poll_ref.get()
        if not poll_doc.exists:
            return jsonify({"error": "Poll not found"}), 404

        poll_data_check = poll_doc.to_dict()
        team_id = poll_data_check["teamId"]
        team_doc = db.collection("teams").document(team_id).get()

        if not team_doc.exists:
            return jsonify({"error": "Team not found"}), 404

        members = team_doc.to_dict().get("members", [])
        if user_id not in members:
            return jsonify({"error": "User is not a member of this team"}), 403

        # Use transaction for atomic vote
        @firestore.transactional
        def update_vote_in_transaction(transaction, poll_ref, user_id, selected_candidate, members):
            # Read current poll state
            poll_snapshot = poll_ref.get(transaction=transaction)
            if not poll_snapshot.exists:
                raise ValueError("Poll not found")

            poll_data = poll_snapshot.to_dict()

            # Verify poll is in Phase 2
            if poll_data.get("phase") != "phase2":
                raise ValueError(f"Poll is not in Phase 2 (currently in {poll_data.get('phase')})")

            # Validate selected candidate is in Top 3
            phase2_candidates = poll_data.get("phase2Candidates", [])
            if selected_candidate not in phase2_candidates:
                raise ValueError(f"Invalid candidate: {selected_candidate}. Must be one of Top 3")

            phase2_votes = poll_data.get("phase2Votes", {})
            locked_in_users = poll_data.get("lockedInUsers", [])

            # Store Phase 2 vote
            phase2_votes[user_id] = selected_candidate

            # Add user to locked-in list (avoid duplicates)
            if user_id not in locked_in_users:
                locked_in_users.append(user_id)

            # Update poll document atomically
            update_data = {
                "phase2Votes": phase2_votes,
                "lockedInUsers": locked_in_users
            }

            transaction.update(poll_ref, update_data)

            return {
                "locked_in_count": len(locked_in_users),
                "total_members": len(members),
                "selected": selected_candidate
            }

        # Execute transaction
        transaction = db.transaction()
        result = update_vote_in_transaction(
            transaction, poll_ref, user_id, selected_candidate, members
        )

        # Check if all members have locked in → close poll
        if result["locked_in_count"] >= result["total_members"]:
            print(f"All {result['total_members']} members locked in Phase 2 - closing poll", flush=True)
            close_poll_internal(poll_id)

        return jsonify({
            "ok": True,
            "yourCurrentVotes": [result["selected"]],
            "totalSelectedCountForYou": 1,
            "lockedInUserCount": result["locked_in_count"],
            "totalMemberCount": result["total_members"]
        }), 200

    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        print(f"Error in cast_phase2_vote: {e}", flush=True)
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500


def transition_phase1_to_phase2(poll_id: str) -> None:
    """
    Transition poll from Phase 1 to Phase 2.
    Calculates Top 3 candidates from Phase 1 approval votes.
    """
    poll_ref = db.collection("polls").document(poll_id)
    poll_doc = poll_ref.get()
    poll_data = poll_doc.to_dict()

    # Calculate approval scores (approvals - rejections)
    phase1_votes = poll_data.get("phase1Votes", {})
    all_candidates_data = poll_data.get("allCandidates", [])

    # Build score map
    scores = {}
    for candidate_data in all_candidates_data:
        candidate_name = candidate_data["name"]
        scores[candidate_name] = {
            "approvals": 0,
            "rejections": 0,
            "ranking": candidate_data["ranking"]
        }

    # Count approvals and rejections
    for vote_data in phase1_votes.values():
        approved = vote_data.get("approved", [])
        rejected = vote_data.get("rejected")

        for candidate in approved:
            if candidate in scores:
                scores[candidate]["approvals"] += 1

        if rejected and rejected in scores:
            scores[rejected]["rejections"] += 1

    # Calculate net scores (approvals - rejections)
    for candidate in scores:
        scores[candidate]["net_score"] = scores[candidate]["approvals"] - scores[candidate]["rejections"]

    # Sort by net_score (desc), then by LLM ranking (asc for tie-breaking)
    sorted_candidates = sorted(
        scores.items(),
        key=lambda x: (-x[1]["net_score"], x[1]["ranking"])
    )

    # Get Top 3
    top_3 = [candidate for candidate, data in sorted_candidates[:3]]

    # Update poll to Phase 2
    poll_ref.update({
        "phase": "phase2",
        "phase2Candidates": top_3,
        "lockedInUsers": [],  # Reset for Phase 2
        "candidates": top_3  # Update legacy field
    })

    print(f"Poll {poll_id} transitioned to Phase 2. Top 3: {top_3}", flush=True)


def close_poll_internal(poll_id: str) -> Dict[str, Any]:
    """
    Internal helper to close a poll and compute rankings.
    Handles both two-phase voting and legacy single-phase voting.

    Returns the updated poll data.
    """
    poll_ref = db.collection("polls").document(poll_id)
    poll_doc = poll_ref.get()
    poll_data = poll_doc.to_dict()

    # Check if this is a two-phase poll
    is_two_phase = "phase" in poll_data and poll_data.get("phase") in ["phase1", "phase2"]

    if is_two_phase:
        # Two-phase voting: use Phase 2 votes with LLM tie-breaking
        print(f"🔒 Closing two-phase poll {poll_id}", flush=True)

        phase2_votes = poll_data.get("phase2Votes", {})
        all_candidates_data = poll_data.get("allCandidates", [])
        phase2_candidates = poll_data.get("phase2Candidates", [])

        # Build score map for Phase 2 candidates
        scores = {}
        for candidate_data in all_candidates_data:
            candidate_name = candidate_data["name"]
            if candidate_name in phase2_candidates:
                scores[candidate_name] = {
                    "votes": 0,
                    "ranking": candidate_data["ranking"]
                }

        # Count Phase 2 votes
        for selected_candidate in phase2_votes.values():
            if selected_candidate in scores:
                scores[selected_candidate]["votes"] += 1

        # Sort by vote count (desc), then by LLM ranking (asc for tie-breaking)
        sorted_candidates = sorted(
            scores.items(),
            key=lambda x: (-x[1]["votes"], x[1]["ranking"])
        )

        result_ranking = [candidate for candidate, data in sorted_candidates]

        # Add any Phase 2 candidates that weren't voted for at the end
        for candidate in phase2_candidates:
            if candidate not in result_ranking:
                result_ranking.append(candidate)

        print(f"✅ Two-phase poll results: {result_ranking}", flush=True)

    else:
        # Legacy single-phase voting
        print(f"🔒 Closing legacy single-phase poll {poll_id}", flush=True)

        votes = poll_data.get("votes", {})
        candidate_scores = {}

        # Initialize scores
        for candidate in poll_data["candidates"]:
            candidate_scores[candidate] = 0

        # Count votes
        for user_choices in votes.values():
            for choice in user_choices:
                if choice in candidate_scores:
                    candidate_scores[choice] += 1

        # Sort by score descending
        sorted_candidates = sorted(
            candidate_scores.items(),
            key=lambda x: x[1],
            reverse=True
        )

        result_ranking = [candidate for candidate, score in sorted_candidates]

        print(f"✅ Legacy poll results: {result_ranking}", flush=True)

    # Update poll document
    poll_ref.update({
        "status": "closed",
        "phase": "closed",  # Mark phase as closed for two-phase polls
        "resultRanking": result_ranking
    })

    # Update team
    team_id = poll_data["teamId"]
    team_ref = db.collection("teams").document(team_id)
    team_ref.update({
        "currentlyOpenPoll": None,
        "lastMealPoll": result_ranking[0] if result_ranking else None  # Use lastMealPoll field
    })

    poll_data["status"] = "closed"
    poll_data["phase"] = "closed"
    poll_data["resultRanking"] = result_ranking

    return poll_data


@app.route("/polls/<poll_id>/close", methods=["POST"])
def close_poll_now(poll_id):
    """
    Manually close a poll immediately.
    Useful for admin/testing to reset team state when a poll got stuck.
    """
    try:
        poll_ref = db.collection("polls").document(poll_id)
        poll_doc = poll_ref.get()
        if not poll_doc.exists:
            return jsonify({"error": "Poll not found"}), 404
        poll_data = poll_doc.to_dict()
        if poll_data.get("status") == "closed":
            result_ranking = poll_data.get("resultRanking", [])
            return jsonify({
                "pollId": poll_id,
                "status": "closed",
                "resultRanking": [
                    {"rank": i + 1, "name": name} for i, name in enumerate(result_ranking)
                ],
                "winner": result_ranking[0] if result_ranking else None
            }), 200
        updated = close_poll_internal(poll_id)
        return jsonify({
            "pollId": poll_id,
            "status": updated.get("status"),
            "resultRanking": [
                {"rank": i + 1, "name": name} for i, name in enumerate(updated.get("resultRanking", []))
            ],
            "winner": updated.get("resultRanking", [None])[0] if updated.get("resultRanking") else None
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def load_mock_candidates_from_file() -> List[str]:
    dataset_path = os.path.join(os.path.dirname(__file__), "food_dataset.json")
    try:
        with open(dataset_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        food_names = [item["name"] for item in data if "name" in item]
        print(f"✅ Loaded {len(food_names)} meals from {dataset_path}")
        return food_names
    except Exception as e:
        print(f"⚠️ Error reading {dataset_path}: {e}")
        return [
            "Bibimbap", "Vegan Burger", "Tonkotsu Ramen", "Naengmyeon",
            "Nasi Goreng", "Pizza Margherita", "Vegan Tofu Bowl", "Kimchi Jjigae",
            "Pad Thai", "Falafel Wrap", "Miso Ramen", "Veggie Burger",
            "Bulgogi", "Sushi Platter", "Vegetarian Curry", "Huevos Rancheros"
        ]


if __name__ == "__main__":
    validate_env_on_startup()
    load_food_database()  # Load food database at startup
    port = int(os.environ.get("PORT", 5001))
    debug = str(os.environ.get("FLASK_DEBUG", "true")).lower() == "true"
    app.run(host="0.0.0.0", port=port, debug=debug)
