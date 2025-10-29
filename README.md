
# Veato Iteration 3 - Meal Poll Service

This repository contains **Iteration 3 of Veato** with a complete meal poll voting system implementation.

## Local Setup (Quickstart)

Backend
```bash
python3 -m pip install -r backend/requirements.txt
# Option A: place service account at backend/firebase-credentials.json
# Option B: export FIREBASE_CREDENTIALS='{"type":"service_account",...}'
# Optional for LLM: export OPENAI_API_KEY=sk-...
python3 backend/server.py
```

Android
- Run the app on the emulator (base URL is `http://10.0.2.2:5001/`).
- Sign in so requests include `X-User-Id`.

Troubleshooting
- If you see “Poll already active for this team”, the previous poll is still active. Wait for auto-close or POST `/polls/{pollId}/close`.
- Without `OPENAI_API_KEY`, the backend uses a safe dummy candidate list.

## Demo Video

### Food Profile Setup
In this step, users personalize their food preferences, dietary restrictions, and disliked ingredients to help the recommendation system generate better meal suggestions.
<video src="https://github.com/user-attachments/assets/4e66b7c3-5215-4d27-8258-9e5e903868fb" controls width="600"></video>

### Team Creation
Users can create or join a team with friends to vote on restaurant choices together. Team Leader can invite/remove members to ensure easy switch between teams, review team activities, and manage their collaborative sessions.
<video src="https://github.com/user-attachments/assets/2a3d8cf3-e54f-4a71-a2da-6140f5a68c3e" controls width="600"></video>

### Meal Poll Voting System
Complete voting system where team leaders can create polls, members vote on meal options, and results are automatically ranked and displayed.
<video src="docs/iteration-1-demo-video(signin_signout).mp4" controls width="600"></video>

## Key Features

- **Food Profile Setup:**  
  Customize your food preferences, cuisines, and dislikes to build a personal taste profile.

- **Team Creation:**  
  Form teams with friends to collaboratively vote and decide where to eat.

- **Meal Poll System:**  
  Team leaders can create polls with custom titles and durations. Members vote on LLM-generated meal candidates based on team constraints.

- **Real-time Voting:**  
  Live voting interface with countdown timer, multi-select voting, and instant result updates.

- **Smart Recommendations:**  
  The app uses team data and individual preferences to suggest optimal dining options via LLM integration.

- **User-Friendly Interface:**  
  Clean UI designed for quick setup and efficient decision-making.

## Technical Implementation

### Backend API (Flask + Firebase)
- **POST /polls/start** - Create new polls with LLM-generated candidates
- **GET /polls/{pollId}** - Get poll state with remaining time and votes
- **POST /polls/{pollId}/vote** - Cast/update votes for poll candidates
- **Auto-poll closing** - Automatic poll closure and ranking when time expires
- **Firebase Firestore** - Real-time data synchronization
- **LLM Integration** - OpenAI/Anthropic API ready for candidate generation

### Frontend (Android + Kotlin)
- **Retrofit Integration** - HTTP API client for backend communication
- **MVVM Architecture** - Clean separation with ViewModels and Repositories
- **Jetpack Compose** - Modern UI with reactive state management
- **Firebase Authentication** - Secure user management
- **Real-time Updates** - Live poll state synchronization

### Data Models
- **Teams Collection** - Team metadata, members, active polls
- **Users Collection** - User profiles with dietary constraints
- **Polls Collection** - Poll data, candidates, votes, results
- **Voting Logic** - Multi-select voting with automatic ranking

Environment
Android Studio: Narwhal Feature Drop | 2025.1.2
Gradle: auto-managed by Android Studio
JDK Version: 17 
Android Min SDK: as in build.gradle (Module)
Firebase Service: Authentication (Email/Password enabled)
Backend: Flask + Firebase Admin SDK
HTTP Client: Retrofit 2.11.0 + OkHttp 4.12.0

## User Authentication Demo (Firebase + Android)
- This demo showcases a Firebase-based user authentication system implemented in an Android app using Kotlin.
- It allows users to create an account, log in, and reset their password securely through Firebase Authentication.

## Features Demonstrated

### a. Create New User
Users can register by entering:
- Full Name
- Username
- Email Address
- Password and Confirm Password
The account is created in Firebase Authentication.

### b. Login
Registered users can log in using their email and password.
On successful login, users are directed to the main screen.

### c. Forgot Password
Users enter their email address and tap Forgot Password.
A message appears "Reset link has been sent if registered."
If the email is registered, a password reset link is sent automatically.

### d. Meal Poll Creation
Team leaders can create polls by:
- Entering poll title (e.g., "10/25 team dinner")
- Selecting duration (1, 3, or 5 minutes)
- Backend generates 5 meal candidates based on team constraints
- Poll becomes active with countdown timer

### e. Voting System
Team members can:
- View poll candidates and remaining time
- Select multiple meal options
- Submit votes or cancel selections
- See real-time poll updates

### f. Results & Ranking
When polls close:
- Automatic ranking by vote count
- Winner displayed as "Last: [meal name]"
- Complete ranked results shown
- Team data updated with poll results

## What This Demo Demonstrates:
- Integration of Firebase Authentication with Android Studio.
- Complete meal poll voting system with real-time updates.
- Backend API implementation with Flask and Firebase Firestore.
- LLM integration for intelligent meal candidate generation.
- MVVM architecture with Retrofit for HTTP communication.
- Real-time Firebase verification for secure user access.
- Use of Toast messages for user feedback and error handling.
- Automatic poll management with countdown timers and ranking.
