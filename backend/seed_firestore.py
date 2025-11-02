import json
import os
from typing import List, Dict

import firebase_admin
from firebase_admin import credentials, firestore


def init_firebase():
    cred_json = os.environ.get("FIREBASE_CREDENTIALS")
    if cred_json:
        cred = credentials.Certificate(json.loads(cred_json))
        firebase_admin.initialize_app(cred)
    else:
        cred_path = os.path.join(os.path.dirname(__file__), "firebase-credentials.json")
        if not os.path.exists(cred_path):
            raise RuntimeError("Missing firebase-credentials.json or FIREBASE_CREDENTIALS env var")
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
    return firestore.client()


def seed_team_and_users(db, team_id: str, team_name: str, members: List[str], users: Dict[str, Dict]):
    # users: { userId: {displayName: str, constraints: {...}} }
    for user_id, user_data in users.items():
        db.collection("users").document(user_id).set(user_data, merge=True)

    db.collection("teams").document(team_id).set({
        "teamName": team_name,
        "members": members,
        "currentlyOpenPoll": None,
        "lastMenu": ""
    }, merge=True)


if __name__ == "__main__":
    # Example usage; edit values or pass via env vars before running
    team_id = os.environ.get("SEED_TEAM_ID", "swpp5")
    team_name = os.environ.get("SEED_TEAM_NAME", "SWPP Team 5")
    members_env = os.environ.get("SEED_MEMBER_IDS", "U1_ABC123,U2_DEF456")
    members = [m.strip() for m in members_env.split(",") if m.strip()]

    users = {
        members[0]: {
            "displayName": os.environ.get("SEED_USER1_NAME", "Alice"),
            "constraints": {
                "allergies": ["peanut"],
                "cannotEat": ["pork"],
                "budgetMax": 15000,
                "spiceTolerance": "medium",
                "dietType": "none",
            },
        },
        (members[1] if len(members) > 1 else f"{members[0]}_2"): {
            "displayName": os.environ.get("SEED_USER2_NAME", "Bob"),
            "constraints": {
                "allergies": [],
                "cannotEat": [],
                "budgetMax": 12000,
                "spiceTolerance": "low",
                "dietType": "vegetarian",
            },
        },
    }

    db = init_firebase()
    seed_team_and_users(db, team_id, team_name, members, users)
    print(f"Seeded team '{team_id}' with members: {', '.join(members)}")


