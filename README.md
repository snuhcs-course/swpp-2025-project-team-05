# Veato - AI-Powered Group Dining Decision App

Veato is a mobile application that helps groups make dining decisions through intelligent menu recommendations and a two-phase voting system. The app uses AI to understand individual dietary preferences and constraints, then recommends menus that satisfy the entire group.

## 📺 Demo Video
The demo video presents the full flow:
User Registration → Profile Setup → Team Creation → Poll Generation → Two-Phase Voting → Results

<video src="https://github.com/user-attachments/assets/6b71145d-cb58-44aa-a728-7f8c1d33f97d" controls width="600"></video>

---

## 🚀 Quick Start

### Backend Setup
```bash
cd backend
python3 -m pip install -r requirements.txt

# Set up Firebase credentials (choose one):
# Option A: Place serviceAccountKey.json in backend/
# Option B: export FIREBASE_CREDENTIALS='{"type":"service_account",...}'

# Set up OpenAI API key:
echo "OPENAI_API_KEY=your_key_here" > .env

# Start server:
python3 server.py
```

Expected output:
```
✅ Firebase credentials detected.
✅ OPENAI_API_KEY set: LLM recommendations enabled.
✅ Loaded 350 food items from database
 * Running on http://127.0.0.1:5001
```

### Android App Setup
```bash
# Open in Android Studio
cd veato
# Build → Make Project (Ctrl+F9)

# Run on emulator (auto-configured for http://10.0.2.2:5001/)
# Or physical device (update BASE_URL in RetrofitClient.kt)
```

**Troubleshooting:**
- "Poll already active": Previous poll still running - wait for auto-close or POST `/polls/{pollId}/close`
- Missing OpenAI key: Backend will use fallback dummy candidates
- Network errors: Verify backend URL matches your setup (10.0.2.2 for emulator, your IP for physical device)

---

## ✨ Key Features

### 1. User Onboarding & Preferences
- **7-step onboarding flow** to capture complete dietary profile
- **Hard constraints**: Dietary restrictions (vegetarian, vegan, halal, etc.), allergies, ingredients to avoid
- **Soft preferences**: Favorite cuisines (Korean, Japanese, Western, etc.), spice tolerance
- **Profile management**: Edit preferences anytime, changes apply immediately

### 2. Team Management
- **Create teams** for group dining decisions
- **Add members** through their username
- **View team members** including their position and age group
- **Leader-controlled member attributes** assign or update each member’s position & age group
- **Team-based polls** restricted to team members
- **Member management** with role-based access

### 3. AI-Powered Menu Recommendations
- **LLM-based ranking** using OpenAI GPT-4
- **350+ food items** with nutritional information
- **10 meal types supported**:
  - rice-based (66 items), soup-based (35), meat-based (50), noodle-based (50)
  - seafood-based (25), bread-based (42), salad-based (15)
  - snack (19), dessert (25), beverage (18)
- **Occasion-aware filtering**: "dessert", "light lunch", "high protein dinner"
- **Nutrition-based ranking**: Calories, protein, carbs, fat
- **Heaviness filtering**: Light, medium, heavy meals

### 4. Two-Phase Voting System ⭐ NEW

#### Phase 1: Approval Voting
- **Multi-select voting** from 5 AI-recommended candidates
- **One-time veto power** - reject unwanted options
- **Automatic replacement** - maintains exactly 5 visible candidates
- **Smart rejection tracking** - never shows rejected items again
- **Early transition** - moves to Phase 2 when all members lock in

#### Phase 2: Final Selection
- **Top 3 finalists** from Phase 1 approval voting
- **Single selection** - choose your top pick
- **Phase 1 vote counts** shown for reference
- **30-second grace period** - prevents premature poll closure
- **Immediate results** when all members vote

### 5. Concurrency & Reliability ⭐ NEW
- **Firestore transactions** - prevents race conditions
- **Atomic vote updates** - safe for simultaneous voting
- **Consistent state** - all clients see same data
- **Reject-and-replace logic** - maintains candidate count
- **Lock-in tracking** - knows when all members voted

---

## 🎯 What This Demo Demonstrates

### Goals Achieved

✅ **Personalized AI Recommendations** - Considers each member's dietary restrictions and preferences
✅ **Democratic Decision Making** - Two-phase system ensures fair group consensus
✅ **Real-time Collaboration** - Multiple users vote simultaneously without conflicts
✅ **Smart Filtering** - Auto-excludes incompatible foods based on allergies
✅ **Occasion Intelligence** - Understands context like "dessert", "light meal", "high protein"
✅ **Robust Voting** - Transaction-based updates prevent data corruption
✅ **Early Closure** - Polls end immediately when everyone votes

### Complete User Journey

**1. Registration & Onboarding (5 min)**
- Create account with Firebase authentication
- Complete 7-step preference setup:
  1. Welcome screen
  2. Dietary restrictions (vegetarian, vegan, etc.)
  3. Allergies (nuts, shellfish, dairy, etc.)
  4. Ingredients to avoid (optional custom list)
  5. Favorite cuisines (Korean, Japanese, Western, etc.)
  6. Spice tolerance (none, mild, medium, spicy, very spicy)
  7. Review & confirm

**2. Team Formation (3 min)**
- Create or join dining teams
- View team member profiles
- Access team-based polls

**3. Poll Creation & Voting (5 min)**
- **Create poll** - Team leader sets occasion and duration
- **AI generation** - System analyzes group constraints and generates ranked recommendations
- **Phase 1 voting** - Approve favorites and reject unwanted options
- **Automatic replacement** - Rejected items instantly replaced
- **Lock-in** - Confirm choices and wait for others
- **Phase 2 voting** - Select top pick from 3 finalists
- **Results** - View the chosen menu only

**4. Preference Management (ongoing)**
- Edit soft preferences (cuisines, spice level)
- Changes immediately affect future recommendations
- Profile syncs across devices

---

## 🏗️ Architecture

### Backend (Flask + Firebase)

```
backend/
├── server.py                 # Flask app with REST API
├── food_dataset.json         # 290 food items with metadata
├── serviceAccountKey.json    # Firebase credentials (not in repo)
├── .env                      # Environment variables (not in repo)
├── requirements.txt          # Python dependencies
└── [documentation].md        # Implementation docs
```

**Key Endpoints:**
- `POST /polls/start` - Create poll with LLM candidates
- `GET /polls/{pollId}` - Get poll state
- `POST /polls/{pollId}/phase1-vote` - Approval voting with veto
- `POST /polls/{pollId}/phase2-vote` - Final selection from top 3
- `POST /polls/{pollId}/close` - Manual poll closure

**Technologies:**
- **Flask 3.0.0** - Web framework
- **Firebase Admin SDK** - Database and auth
- **OpenAI GPT-4** - LLM recommendations
- **Firestore Transactions** - Atomic updates

### Android App (Kotlin + Jetpack Compose)

```
veato/app/src/main/java/com/example/veato/
├── ui/
│   ├── onboarding/          # 7-step preference flow
│   │   ├── OnboardingScreen.kt
│   │   ├── OnboardingViewModel.kt
│   │   └── screens/         # Individual step screens
│   ├── poll/                # Voting screens
│   │   ├── Phase1VoteScreen.kt   # Approval voting
│   │   ├── Phase2VoteScreen.kt   # Final selection
│   │   └── PollViewModel.kt
│   ├── profile/             # Profile management
│   │   ├── ProfileViewModel.kt
│   │   └── MyPreferencesActivity.kt
│   ├── main/                # Main navigation
│   └── components/          # Reusable UI
├── data/
│   ├── model/               # Data classes
│   │   ├── Poll.kt
│   │   ├── UserProfile.kt
│   │   ├── Team.kt
│   │   └── Candidate.kt
│   ├── remote/              # Retrofit services
│   │   ├── PollApiService.kt
│   │   └── ProfileApiDataSource.kt
│   └── repository/          # Data access layer
│       ├── PollRepository.kt
│       └── UserProfileRepository.kt
└── Activities/
    ├── LoginActivity.kt
    ├── OnboardingActivity.kt
    ├── MyTeamsActivity.kt
    ├── VoteSessionActivity.kt
    └── ProfileActivity.kt
```

**Technologies:**
- **Kotlin 1.9** - Programming language
- **Jetpack Compose** - Declarative UI
- **Retrofit 2.11.0** - HTTP client
- **Firebase Android SDK** - Auth + Firestore
- **Material 3** - Design system
- **Coroutines + StateFlow** - Async + reactive state

---

## 📋 Detailed Setup Instructions

### Prerequisites

**Backend:**
- Python 3.8+
- pip package manager
- Firebase project with Firestore
- OpenAI API key

**Android:**
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK API 24+ (Android 7.0+)
- Kotlin 1.9+
- JDK 17

### Backend Configuration

1. **Install dependencies:**
   ```bash
   cd backend
   pip install -r requirements.txt
   ```

2. **Firebase setup:**
   - Go to Firebase Console → Project Settings → Service Accounts
   - Generate new private key → save as `serviceAccountKey.json`
   - Place in `backend/` directory

3. **Environment variables:**
   Create `backend/.env`:
   ```env
   OPENAI_API_KEY=sk-proj-your_key_here
   FLASK_DEBUG=true
   PORT=5001
   ```

4. **Verify data:**
   ```bash
   # Check food database loaded
   ls -la backend/food_dataset.json
   ```

5. **Start server:**
   ```bash
   python3 backend/server.py
   ```

### Android Configuration

1. **Open project:**
   ```bash
   # Open veato/ folder in Android Studio
   # Wait for Gradle sync
   ```

2. **Firebase setup:**
   - Firebase Console → Project Settings → General
   - Download `google-services.json`
   - Place in `veato/app/`

3. **Update backend URL** (if needed):
   Edit `veato/app/src/main/java/com/example/veato/data/remote/RetrofitClient.kt`:
   ```kotlin
   // For Android Emulator:
   private const val BASE_URL = "http://10.0.2.2:5001/"

   // For Physical Device:
   private const val BASE_URL = "http://192.168.1.X:5001/"  // Your computer's IP
   ```

4. **Build:**
   ```bash
   Build → Make Project (Ctrl+F9)
   ```

---

## 🧪 Testing the Demo

### Scenario 1: Basic Flow (10 minutes)

1. **Register & Onboard**
   - Launch app → Register with email/password
   - Complete all 7 onboarding steps
   - Select: Vegetarian, Nuts allergy, Korean/Japanese cuisines, Medium spice

2. **Create Team**
   - Navigate to "My Teams"
   - Create team: "CS Lunch Group"

3. **Start Poll**
   - Click "Start New Poll"
   - Title: "Dinner time"
   - Occasion: "recommend me desserts"
   - Duration: 5 minutes
   - Click "Start Poll"

4. **Vote in Phase 1**
   - Review 5 AI-generated desserts
   - Approve 3 favorites (tap to select)
   - Reject 1 unwanted option (tap veto button)
   - Observe automatic replacement
   - Click "Lock In Vote"

5. **Vote in Phase 2**
   - Poll transitions to top 3
   - See Phase 1 vote counts
   - Select final choice
   - Click "Lock In Vote"

6. **View Results**
   - Poll closes automatically
   - See winner and full ranking
   - View vote distribution

### Scenario 2: Advanced Features (15 minutes)

**Test Meal Type Filtering:**
```
"something light" → Light meals only
"high protein lunch" → Sorted by protein
"seafood dinner" → Seafood items only
"soup for cold weather" → Soup-based meals
"spicy noodles" → Noodles with higher spice
```

**Test Multi-user (requires 2 accounts):**
1. Create 2 accounts, both join same team
2. User 1 creates poll
3. Both users vote in Phase 1 simultaneously
4. Verify no race conditions
5. Both transition to Phase 2 together
6. Both vote → poll closes immediately

**Test Edge Cases:**
1. **Reject all 5** - What happens when pool exhausted?
2. **Change preferences mid-poll** - Does it affect active polls?
3. **Leave poll before voting** - Can you come back?
4. **Timer expiry** - Does 30-second grace period work?

---

## 🛠️ Technologies & Environment

### Development Environment
- **OS**: macOS Sonoma 14.3 / Windows 11 / Linux
- **Python**: 3.10.12
- **Android Studio**: Hedgehog (2023.1.1) or Narwhal (2025.1.2)
- **Android SDK**: API 34 (Android 14)
- **JDK**: OpenJDK 17
- **Gradle**: Auto-managed by Android Studio

### Tech Stack

**Backend:**
- Python 3.10+
- Flask 3.0.0
- Firebase Admin SDK
- OpenAI API (GPT-4)
- python-dotenv

**Android:**
- Kotlin 1.9
- Jetpack Compose
- Retrofit 2.11.0
- OkHttp 4.12.0
- Firebase Android SDK
- Material 3
- Coil (image loading)

**Database:**
- Firebase Firestore
  - Collections: `users`, `teams`, `polls`
  - Real-time sync
  - Transaction support

---

## 📚 API Documentation

### Poll Lifecycle

```
1. Create Poll
   POST /polls/start
   Body: { pollTitle, durationMinutes, selectedMembers }

2. Phase 1 Voting
   POST /polls/{pollId}/phase1-vote
   Body: { approvedCandidates: [...], rejectedCandidate: "..." }

3. Automatic Phase Transition
   (when all members lock in Phase 1)

4. Phase 2 Voting
   POST /polls/{pollId}/phase2-vote
   Body: { selectedCandidate: "..." }

5. Poll Closure
   (automatic when all members vote or timer expires + grace period)

6. Results
   GET /polls/{pollId}
   Response: { phase: "closed", results: [...], winner: "..." }
```

---

## 🐛 Troubleshooting

### Backend Issues

**Port already in use:**
```bash
lsof -ti:5001 | xargs kill -9
python3 server.py
```

**Firebase connection failed:**
```bash
# Verify credentials
ls -la backend/serviceAccountKey.json
cat backend/serviceAccountKey.json | jq .project_id
```

**OpenAI API errors:**
- Check API key in `.env`
- Verify quota: https://platform.openai.com/usage
- Check rate limits

**No recommendations generated:**
- Verify `food_dataset.json` exists
- Check server logs for filtering errors
- Ensure constraints aren't too restrictive

### Android Issues

**Build fails:**
```bash
./gradlew clean build
# In Android Studio: File → Invalidate Caches → Restart
```

**Network errors:**
```bash
# Verify server running
curl http://localhost:5001/health

# Check backend URL in RetrofitClient.kt
# Emulator: 10.0.2.2
# Physical: Your computer's IP address
```

**Firebase auth fails:**
- Verify `google-services.json` in `app/`
- Check package name matches Firebase console
- Enable Email/Password auth in Firebase

**Vote not registering:**
- Check server logs for transaction errors
- Verify user ID in request headers
- Check team membership

---

## 📈 Future Enhancements

### User Experience Improvements
- [ ] Save favorite restaurants and their menus to speed up future polls
- [ ] Allow users to suggest custom menu items during voting
- [ ] Show "Why this was recommended" explanations for each menu item
- [ ] Add dietary preference intensity levels (e.g., "strongly prefer Korean" vs "slightly prefer Korean")

### Team & Social Features
- [ ] Recurring polls for regular team lunches (e.g., "Every Friday at noon")
- [ ] Team dining history - see what the group ordered before and how it was rated
- [ ] Post-meal ratings to improve future recommendations
- [ ] Split teams into subgroups for different occasions

### Practical Integrations
- [ ] Budget constraints per person or per team
- [ ] Delivery app deep links (Coupang Eats, Baemin) for winner menu
- [ ] Nearby restaurant filtering using user location
- [ ] Operating hours awareness (don't recommend closed restaurants)

### Smart Recommendations
- [ ] Preference retraining from vote history - automatically adjust user preferences based on what they actually vote for vs what they say they like
- [ ] Learn from past votes: "Your team always picks Korean for Friday lunch"
- [ ] Weather-aware suggestions (hot soup on cold days, cold noodles in summer)
- [ ] Time-of-day awareness (lighter options for late dinner)
- [ ] Track individual vote patterns to better understand soft preferences

---
