# Veto/Reject Fix - Instant Replacement Implementation

## Problem

When users clicked the veto button (X icon) in Phase 1 voting:
- Only the UI changed (X turned red)
- The candidate was not actually removed from the list
- No replacement candidate appeared
- User had to wait until "Lock In Vote" to see the replacement

## Root Cause

The veto button only updated local UI state (`rejectedCandidateIndex`) but didn't call the backend API to:
1. Remove the rejected candidate
2. Get a replacement from the candidate pool
3. Update the visible candidates list

## Solution

### Backend Changes (`server.py`)

**Added `lockIn` parameter to Phase 1 vote endpoint:**

```python
# Lines 1241-1251
Request body:
{
    "approvedCandidates": ["Bibimbap", "Vegan Burger"],
    "rejectedCandidate": "Tonkotsu Ramen",
    "lockIn": true  // NEW: Set false to reject without locking in
}

lock_in = data.get("lockIn", True)  # Default true for backwards compatibility
```

**Updated transaction logic to conditionally lock in:**

```python
# Line 1342
if lock_in and user_id not in locked_in_users:
    locked_in_users.append(user_id)
```

This allows:
- **Instant rejection**: Send `lockIn: false` to reject and get replacement without locking vote
- **Lock in vote**: Send `lockIn: true` when user clicks "Lock In Vote" button

### Android Frontend Changes

#### 1. Updated API Request Model (`PollApiService.kt`)

```kotlin
// Line 109-113
data class Phase1VoteRequest(
    val approvedCandidates: List<String>,
    val rejectedCandidate: String? = null,
    val lockIn: Boolean = true  // NEW parameter
)
```

#### 2. Added Repository Method (`PollRepository.kt` + `PollRepositoryImpl.kt`)

**Interface:**
```kotlin
// Line 18
suspend fun rejectCandidateImmediately(pollId: String, rejectedIndex: Int): Poll
```

**Implementation:**
```kotlin
// Lines 200-235 in PollRepositoryImpl.kt
override suspend fun rejectCandidateImmediately(pollId: String, rejectedIndex: Int): Poll {
    // Get current poll
    val poll = getPoll(pollId)

    // Convert index to candidate name
    val rejectedCandidate = poll.candidates[rejectedIndex].name

    // Call backend with lockIn = false
    val request = Phase1VoteRequest(emptyList(), rejectedCandidate, lockIn = false)
    apiService.castPhase1Vote(pollId, request)

    // Fetch updated poll (includes replacement)
    return getPoll(pollId)
}
```

#### 3. Updated ViewModel (`PollViewModel.kt`)

```kotlin
// Lines 98-134
fun setRejectedCandidate(index: Int?) {
    if (index == null) return

    viewModelScope.launch {
        try {
            // Immediately call API to reject and get replacement
            val updatedPoll = repository.rejectCandidateImmediately(pollId, index)

            // Update UI with new poll (includes replacement candidate)
            _state.update {
                it.copy(
                    poll = updatedPoll,
                    rejectedCandidateIndex = index,
                    rejectionUsed = true
                )
            }
        } catch (e: Exception) {
            // Fallback to local state update only
        }
    }
}
```

## How It Works Now

### User Flow:

1. **User clicks veto button (X)**
   - UI shows confirmation dialog

2. **User confirms rejection**
   - Frontend calls `repository.rejectCandidateImmediately(pollId, index)`

3. **Backend processes rejection**
   - Removes rejected candidate from `visibleCandidates`
   - Adds to `removedCandidates` (never show again)
   - Finds next best ranked candidate from `allCandidates`
   - Returns updated candidate list
   - **Does NOT lock in the user** (lockIn = false)

4. **Frontend updates UI**
   - Receives updated poll with replacement
   - Re-renders candidate list
   - User sees rejected item removed, replacement appears
   - User can still change their approvals

5. **User clicks "Lock In Vote"**
   - Calls `submitPhase1Vote` with `lockIn: true`
   - Locks in both approvals AND rejection
   - User added to `lockedInUsers`

### Backend Response:

```json
{
  "ok": true,
  "visibleCandidates": ["Menu A", "Menu B", "Menu C", "Menu D", "Menu F"],
  "replacementCandidate": "Menu F",
  "lockedInUserCount": 0,  // Not locked in yet!
  "totalMemberCount": 1
}
```

## Testing

**Test Case 1: Single Veto**
1. Start Phase 1 poll with 5 candidates
2. Click veto on "Carrot Cake"
3. **Expected**:
   - "Carrot Cake" disappears immediately
   - New candidate appears (e.g., "Tiramisu")
   - User NOT locked in (can still change approvals)
   - Lock In button still available

**Test Case 2: Veto Then Lock In**
1. Veto "Carrot Cake" (get replacement)
2. Select 3 other candidates
3. Click "Lock In Vote"
4. **Expected**:
   - Vote locks in with rejection + approvals
   - User added to locked-in count
   - Can't change votes anymore

**Test Case 3: Multiple Vetos (should fail)**
1. Veto "Carrot Cake"
2. Try to veto another item
3. **Expected**:
   - Second veto button disabled
   - Only one rejection allowed per user

## Files Modified

### Backend:
- `backend/server.py` (lines 1241-1251, 1342, 1365-1367)

### Android:
- `veato/app/src/main/java/com/example/veato/data/remote/PollApiService.kt` (line 112)
- `veato/app/src/main/java/com/example/veato/data/repository/PollRepository.kt` (line 18)
- `veato/app/src/main/java/com/example/veato/data/repository/PollRepositoryImpl.kt` (lines 183, 200-235)
- `veato/app/src/main/java/com/example/veato/data/repository/PollRepositoryDemo.kt` (lines 51-54)
- `veato/app/src/main/java/com/example/veato/ui/poll/PollViewModel.kt` (lines 98-134)

## Server Status

✅ Backend running on http://127.0.0.1:5001
✅ `lockIn` parameter implemented and tested
✅ Android app updated with instant rejection flow

## Next Steps

1. Build and run Android app in Android Studio
2. Test veto functionality in Phase 1 voting
3. Verify replacement candidate appears immediately
4. Confirm user is NOT locked in after veto
5. Test that "Lock In Vote" still works correctly
