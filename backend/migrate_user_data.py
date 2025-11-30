#!/usr/bin/env python3
"""
Firestore user data migration script for dietary preferences taxonomy update.

This script:
1. Removes "KOSHER" from hardConstraints.dietaryRestrictions
2. Migrates old cuisine values to new 6-group taxonomy in softPreferences.favoriteCuisines
3. Generates migration report with statistics

Usage:
    python3 migrate_user_data.py [--dry-run]
"""

import sys
import argparse
from datetime import datetime

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    print("Error: Firebase Admin SDK not installed.")
    print("Install with: pip install firebase-admin")
    sys.exit(1)

# Legacy cuisine mapping (same as server.py)
LEGACY_CUISINE_MAP = {
    "ITALIAN": "EUROPEAN",
    "FRENCH": "EUROPEAN",
    "MEXICAN": "WESTERN",
    "THAI": "ASIAN",
    "INDIAN": "ASIAN",
    "SOUTHEAST_ASIAN": "ASIAN"
}

# New valid cuisine values
NEW_CUISINES = ["KOREAN", "JAPANESE", "CHINESE", "WESTERN", "EUROPEAN", "ASIAN"]

# New valid dietary restriction values (KOSHER removed)
NEW_DIETARY_RESTRICTIONS = [
    "VEGETARIAN", "VEGAN", "HALAL", "PESCATARIAN",
    "GLUTEN_FREE", "LACTOSE_FREE"
]

def migrate_user_data(dry_run=False):
    """
    Migrate user data in Firestore to match new taxonomy.

    Args:
        dry_run: If True, report changes without modifying database
    """
    print(f"\n{'=' * 60}")
    print(f"Firestore User Data Migration")
    print(f"Mode: {'DRY RUN (no changes will be made)' if dry_run else 'LIVE (will modify database)'}")
    print(f"Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"{'=' * 60}\n")

    # Initialize Firebase Admin SDK
    try:
        # Try to use existing app if already initialized
        app = firebase_admin.get_app()
        print("✓ Using existing Firebase app")
    except ValueError:
        # Initialize new app with default credentials
        try:
            cred = credentials.ApplicationDefault()
            firebase_admin.initialize_app(cred)
            print("✓ Initialized Firebase with application default credentials")
        except Exception as e:
            print(f"✗ Failed to initialize Firebase: {e}")
            print("\nPlease ensure you have set up Firebase credentials:")
            print("  export GOOGLE_APPLICATION_CREDENTIALS='/path/to/serviceAccountKey.json'")
            sys.exit(1)

    db = firestore.client()
    users_ref = db.collection('users')

    stats = {
        'total_users': 0,
        'users_with_kosher': 0,
        'users_with_legacy_cuisines': 0,
        'kosher_removed_count': 0,
        'cuisines_migrated': {},
        'errors': []
    }

    print("Fetching all users from Firestore...")
    try:
        users = users_ref.stream()
    except Exception as e:
        print(f"✗ Failed to fetch users: {e}")
        sys.exit(1)

    for user_doc in users:
        stats['total_users'] += 1
        user_id = user_doc.id
        user_data = user_doc.to_dict()

        changes_made = []
        updated_data = {}

        # Check and remove KOSHER from dietaryRestrictions
        if 'hardConstraints' in user_data and 'dietaryRestrictions' in user_data['hardConstraints']:
            dietary_restrictions = user_data['hardConstraints']['dietaryRestrictions']

            if 'KOSHER' in dietary_restrictions:
                stats['users_with_kosher'] += 1
                stats['kosher_removed_count'] += 1

                cleaned_restrictions = [r for r in dietary_restrictions if r != 'KOSHER']

                if 'hardConstraints' not in updated_data:
                    updated_data['hardConstraints'] = user_data['hardConstraints'].copy()
                updated_data['hardConstraints']['dietaryRestrictions'] = cleaned_restrictions

                changes_made.append(f"Removed KOSHER from dietaryRestrictions")

        # Migrate legacy cuisine values
        if 'softPreferences' in user_data and 'favoriteCuisines' in user_data['softPreferences']:
            favorite_cuisines = user_data['softPreferences']['favoriteCuisines']

            migrated_cuisines = []
            cuisine_changes = []

            for cuisine in favorite_cuisines:
                if cuisine in LEGACY_CUISINE_MAP:
                    new_cuisine = LEGACY_CUISINE_MAP[cuisine]
                    migrated_cuisines.append(new_cuisine)
                    cuisine_changes.append(f"{cuisine} → {new_cuisine}")

                    # Track migration stats
                    if cuisine not in stats['cuisines_migrated']:
                        stats['cuisines_migrated'][cuisine] = 0
                    stats['cuisines_migrated'][cuisine] += 1

                elif cuisine in NEW_CUISINES:
                    migrated_cuisines.append(cuisine)
                else:
                    # Unknown cuisine value
                    stats['errors'].append({
                        'user_id': user_id,
                        'issue': f"Unknown cuisine value: {cuisine}"
                    })
                    migrated_cuisines.append(cuisine)  # Keep it for now

            # Remove duplicates and update if changed
            migrated_cuisines = list(set(migrated_cuisines))

            if cuisine_changes:
                stats['users_with_legacy_cuisines'] += 1

                if 'softPreferences' not in updated_data:
                    updated_data['softPreferences'] = user_data['softPreferences'].copy()
                updated_data['softPreferences']['favoriteCuisines'] = migrated_cuisines

                changes_made.append(f"Migrated cuisines: {', '.join(cuisine_changes)}")

        # Apply updates to Firestore
        if changes_made:
            print(f"\n👤 User: {user_id}")
            for change in changes_made:
                print(f"   • {change}")

            if not dry_run:
                try:
                    users_ref.document(user_id).update(updated_data)
                    print("   ✓ Changes applied")
                except Exception as e:
                    error_msg = f"Failed to update user {user_id}: {e}"
                    print(f"   ✗ {error_msg}")
                    stats['errors'].append({
                        'user_id': user_id,
                        'issue': error_msg
                    })
            else:
                print("   ⚠️  Dry run - changes not applied")

    # Print migration report
    print(f"\n{'=' * 60}")
    print(f"Migration Report")
    print(f"{'=' * 60}\n")
    print(f"Total users processed: {stats['total_users']}")
    print(f"Users with KOSHER removed: {stats['users_with_kosher']}")
    print(f"Users with legacy cuisines migrated: {stats['users_with_legacy_cuisines']}")

    if stats['cuisines_migrated']:
        print(f"\nCuisine migration breakdown:")
        for old_cuisine, count in stats['cuisines_migrated'].items():
            new_cuisine = LEGACY_CUISINE_MAP.get(old_cuisine, old_cuisine)
            print(f"   {old_cuisine} → {new_cuisine}: {count} occurrences")

    if stats['errors']:
        print(f"\n⚠️  Errors encountered: {len(stats['errors'])}")
        for error in stats['errors'][:10]:  # Show first 10 errors
            print(f"   • User {error['user_id']}: {error['issue']}")
        if len(stats['errors']) > 10:
            print(f"   ... and {len(stats['errors']) - 10} more errors")

    print(f"\nCompleted: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"{'=' * 60}\n")

    if dry_run:
        print("ℹ️  This was a dry run. Re-run without --dry-run to apply changes.")
    else:
        print("✅ Migration completed successfully!")

def main():
    parser = argparse.ArgumentParser(
        description='Migrate Firestore user data to new dietary preferences taxonomy'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Report changes without modifying the database'
    )

    args = parser.parse_args()

    if not args.dry_run:
        response = input("\n⚠️  WARNING: This will modify the Firestore database. Continue? (yes/no): ")
        if response.lower() != 'yes':
            print("Migration cancelled.")
            sys.exit(0)

    migrate_user_data(dry_run=args.dry_run)

if __name__ == "__main__":
    main()
