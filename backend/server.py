from flask import Flask, request, jsonify
import firebase_admin
from firebase_admin import credentials, auth

app = Flask(__name__)
cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)

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

if __name__ == "__main__":
    app.run(debug=True)
