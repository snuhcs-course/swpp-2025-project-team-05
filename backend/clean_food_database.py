#!/usr/bin/env python3
"""
Clean food_dataset.json to match new dietary taxonomy:
1. Remove "kosher" from dietary_violations
2. Update allergen "nuts" → keep as generic "nuts" (backend handles mapping)
3. Update cuisine values to new 6-group taxonomy
"""

import json
import os

# Legacy cuisine migration mapping (matches server.py)
LEGACY_CUISINE_MAP = {
    "italian": "european",
    "french": "european",
    "mexican": "western",
    "american": "western",
    "british": "western",
    "thai": "asian",
    "vietnamese": "asian",
    "indonesian": "asian",
    "malaysian": "asian",
    "philippine": "asian",
    "indian": "asian",
    "southeast_asian": "asian"
}

NEW_CUISINES = ["korean", "japanese", "chinese", "western", "european", "asian"]

def clean_food_database():
    """Clean the food database according to new taxonomy"""

    food_db_path = os.path.join(os.path.dirname(__file__), "food_dataset.json")

    # Load food database
    with open(food_db_path, 'r', encoding='utf-8') as f:
        foods = json.load(f)

    print(f"📦 Loaded {len(foods)} food items")

    # Statistics
    stats = {
        'kosher_removed': 0,
        'cuisine_migrated': 0,
        'cuisine_unknown': 0
    }

    # Process each food item
    for food in foods:
        # 1. Remove kosher from dietary_violations
        if 'dietary_violations' in food:
            original_violations = food['dietary_violations'][:]
            food['dietary_violations'] = [v for v in food['dietary_violations'] if v != 'kosher']

            if len(original_violations) != len(food['dietary_violations']):
                stats['kosher_removed'] += 1

        # 2. Migrate cuisine values
        if 'cuisine' in food:
            original_cuisine = food['cuisine']
            cuisine_lower = original_cuisine.lower()

            # Check if it's a legacy value that needs migration
            if cuisine_lower in LEGACY_CUISINE_MAP:
                food['cuisine'] = LEGACY_CUISINE_MAP[cuisine_lower]
                stats['cuisine_migrated'] += 1
                print(f"  ✓ {food['food_id']}: {original_cuisine} → {food['cuisine']}")

            # Check if it's already in new taxonomy
            elif cuisine_lower not in NEW_CUISINES:
                stats['cuisine_unknown'] += 1
                print(f"  ⚠️  {food['food_id']}: Unknown cuisine '{original_cuisine}'")

    # Save cleaned database
    backup_path = food_db_path + '.backup'
    os.rename(food_db_path, backup_path)
    print(f"💾 Backup saved to {backup_path}")

    with open(food_db_path, 'w', encoding='utf-8') as f:
        json.dump(foods, f, indent=2, ensure_ascii=False)

    print(f"\n✅ Food database cleaned successfully!")
    print(f"\n📊 Statistics:")
    print(f"   - Kosher entries removed: {stats['kosher_removed']}")
    print(f"   - Cuisines migrated: {stats['cuisine_migrated']}")
    print(f"   - Unknown cuisines found: {stats['cuisine_unknown']}")
    print(f"\n💾 Saved to {food_db_path}")

if __name__ == '__main__':
    clean_food_database()
