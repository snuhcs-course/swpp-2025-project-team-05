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

# Helper function to generate candidates using LLM
def generate_candidates_for_team(team_name: str, members_constraints: List[Dict[str, Any]]) -> List[str]:
    """
    Generate 5 meal candidates using ChatGPT based on team members' constraints.
    
    Args:
        team_name: Name of the team
        members_constraints: List of dicts with 'userId' and 'constraints' keys
    
    Returns:
        List of 5 meal candidate names
    """
    try:
        # Build constraint summary
        constraints_text = []
        for i, member in enumerate(members_constraints):
            constraints_text.append(f"Member {i+1}:")
            if member.get('allergies'):
                constraints_text.append(f"  Allergies: {', '.join(member['allergies'])}")
            if member.get('no'):
                constraints_text.append(f"  Avoids: {', '.join(member['no'])}")
            if member.get('budgetMax'):
                constraints_text.append(f"  Budget: {member['budgetMax']} won")
            if member.get('pref'):
                constraints_text.append(f"  Prefers: {', '.join(member['pref'])}")
        
        constraints_summary = "\n".join(constraints_text)
        
        # Create prompt
        prompt = f"""
        Generate exactly 5 diverse meal recommendations for team "{team_name}" based on these member constraints:

        {constraints_summary}

        Requirements:
        - Return exactly 5 meal names
        - Each meal should be one line, just the name
        - Consider all constraints (allergies, preferences, budget)
        - Make meals diverse (different cuisines/types)
        - Use simple, clear meal names
        - No explanations, just the 5 names

        Example format:
        Bibimbap
        Vegan Burger
        Tonkotsu Ramen
        Nasi Goreng
        Falafel Wrap
        """

        # Call ChatGPT (reads key from OPENAI_API_KEY env var)
        from openai import OpenAI
        client = OpenAI()
        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "You are a helpful assistant that generates meal recommendations based on dietary constraints."},
                {"role": "user", "content": prompt}
            ],
            max_tokens=200,
            temperature=0.7
        )
        
        # Parse response
        candidates = []
        for line in response.choices[0].message.content.strip().split('\n'):
            line = line.strip()
            if line and not line.startswith('#') and not line.startswith('-'):
                candidates.append(line)
        
        # Ensure we have exactly 5 candidates
        if len(candidates) >= 5:
            return candidates[:5]
        else:
            # Fallback to mock data if ChatGPT returns fewer than 5
            mock_candidates = [
                "Bibimbap", "Vegan Burger", "Tonkotsu Ramen", "Naengmyeon",
                "Nasi Goreng", "Pizza Margherita", "Vegan Tofu Bowl", "Kimchi Jjigae",
                "Pad Thai", "Falafel Wrap", "Miso Ramen", "Veggie Burger",
                "Bulgogi", "Sushi Platter", "Vegetarian Curry", "Huevos Rancheros"
            ]
            return random.sample(mock_candidates, 5)
            
    except Exception as e:
        print(f"Error calling ChatGPT: {e}")
        # Fallback to mock data on error
        mock_candidates = [
            "Bibimbap", "Vegan Burger", "Tonkotsu Ramen", "Naengmyeon",
            "Nasi Goreng", "Pizza Margherita", "Vegan Tofu Bowl", "Kimchi Jjigae",
            "Pad Thai", "Falafel Wrap", "Miso Ramen", "Veggie Burger",
            "Bulgogi", "Sushi Platter", "Vegetarian Curry", "Huevos Rancheros"
        ]
        return random.sample(mock_candidates, 5)


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
    duration_minutes = data.get("durationMinutes")
    
    if not team_id or not poll_title or not duration_minutes:
        return jsonify({"error": "Missing required fields"}), 400
    
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
                constraints = user_data.get("constraints", {})
                members_constraints.append({
                    "userId": user_id,
                    "constraints": constraints
                })
        
        # Generate candidates
        candidates = generate_candidates_for_team(team_data.get("teamName", ""), members_constraints)
        
        # Create poll document
        started_time = datetime.utcnow()
        poll_data = {
            "pollTitle": poll_title,
            "startedTime": started_time,
            "duration": duration_minutes,
            "teamId": team_id,
            "teamName": team_data.get("teamName", ""),
            "candidates": candidates,
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
            "candidates": candidates
        }), 200
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/polls/<poll_id>", methods=["GET"])
def get_poll(poll_id):
    """
    Get poll state (for voting session screen or closed screen).
    
    Returns active poll state with remaining time and current votes,
    or closed poll state with results.
    """
    # Get current user ID from request (assuming auth middleware adds it)
    user_id = request.headers.get("X-User-Id") or "demo_user"
    
    try:
        poll_ref = db.collection("polls").document(poll_id)
        poll_doc = poll_ref.get()
        
        if not poll_doc.exists:
            return jsonify({"error": "Poll not found"}), 404
        
        poll_data = poll_doc.to_dict()
        
        # Calculate remaining time
        started_time = poll_data["startedTime"]
        duration_minutes = poll_data["duration"]
        
        # Convert Firestore Timestamp to UTC datetime (avoid local-time drift)
        if hasattr(started_time, 'timestamp'):
            started_dt = datetime.utcfromtimestamp(started_time.timestamp())
        else:
            started_dt = started_time  # expected to be UTC
        
        elapsed_seconds = (datetime.utcnow() - started_dt).total_seconds()
        seconds_left = (duration_minutes * 60) - elapsed_seconds
        remaining_seconds = max(0, seconds_left)
        
        # Auto-close if expired and still active (be robust to small float errors)
        if seconds_left <= 0 and poll_data["status"] == "active":
            poll_data = close_poll_internal(poll_id)
        
        # Prepare response based on status
        if poll_data["status"] == "closed":
            result_ranking = poll_data.get("resultRanking", [])
            
            return jsonify({
                "pollId": poll_id,
                "pollTitle": poll_data.get("pollTitle", ""),
                "status": "closed",
                "resultRanking": [
                    {"rank": idx + 1, "name": candidate}
                    for idx, candidate in enumerate(result_ranking)
                ],
                "winner": result_ranking[0] if result_ranking else None
            }), 200
        
        else:  # active
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


def close_poll_internal(poll_id: str) -> Dict[str, Any]:
    """
    Internal helper to close a poll and compute rankings.
    
    Returns the updated poll data.
    """
    poll_ref = db.collection("polls").document(poll_id)
    poll_doc = poll_ref.get()
    poll_data = poll_doc.to_dict()
    
    # Compute ranking
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
    
    # Update poll document
    poll_ref.update({
        "status": "closed",
        "resultRanking": result_ranking
    })
    
    # Update team
    team_id = poll_data["teamId"]
    team_ref = db.collection("teams").document(team_id)
    team_ref.update({
        "currentlyOpenPoll": None,
        "lastMenu": result_ranking[0] if result_ranking else None
    })
    
    poll_data["status"] = "closed"
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

if __name__ == "__main__":
    validate_env_on_startup()
    port = int(os.environ.get("PORT", 5001))
    debug = str(os.environ.get("FLASK_DEBUG", "true")).lower() == "true"
    app.run(host="0.0.0.0", port=port, debug=debug)
