# Poll Voting System - Implementation Summary

## Overview
This document summarizes the fixes applied to address Problems 2 and 3 from the poll voting system requirements.

## Problems Addressed

### Problem 1: Early Phase Transition ✅ Already Working
**Status**: Already correctly implemented in existing code
- Lines 1286-1287 (Phase 1): Checks if all members have locked in
- Lines 1363-1364 (Phase 2): Triggers poll closure when all members vote
- If not working in practice, investigate frontend polling frequency

### Problem 2: Reject-and-Replace ✅ FIXED
**Status**: Fully implemented with transaction support

**Changes Made**:
1. Added `removedCandidates` field to poll creation (server.py:942)
   - Tracks globally rejected candidates to prevent re-showing

2. Rewrote `/polls/<poll_id>/phase1-vote` endpoint (server.py:1212-1365)
   - Wrapped in Firestore transaction for atomic updates
   - Enforces one-time reject per user
   - When user rejects a menu:
     - Removes from `visibleCandidates`
     - Adds to `removedCandidates` (global tracking)
     - Finds replacement from `allCandidates` pool
     - Sorts available candidates by LLM ranking
     - Adds best available replacement to maintain 5 visible options
   - Returns updated state including replacement candidate

**Key Logic**:
```python
# Find replacement from allCandidates pool (keep 5 visible)
available_candidates = [
    c for c in all_candidates
    if c["name"] not in visible_candidates and c["name"] not in removed_candidates
]

if available_candidates and len(visible_candidates) < 5:
    # Sort by ranking (lowest/best first)
    available_candidates.sort(key=lambda x: x.get("ranking", 999))
    replacement_candidate = available_candidates[0]["name"]
    visible_candidates.append(replacement_candidate)
```

### Problem 3: Concurrency Safety ✅ FIXED
**Status**: Fully implemented with Firestore transactions

**Changes Made**:
1. Phase 1 voting now uses `@firestore.transactional` (server.py:1212-1365)
   - Atomic read-modify-write prevents race conditions
   - Multiple users can vote/reject simultaneously without conflicts
   - Lock-in tracking is thread-safe

2. Phase 2 voting now uses `@firestore.transactional` (server.py:1368-1472)
   - Atomic vote recording and lock-in tracking
   - Safe concurrent final selections

**Transaction Pattern**:
```python
@firestore.transactional
def update_vote_in_transaction(transaction, poll_ref, user_id, ...):
    # 1. Read current state with transaction
    poll_snapshot = poll_ref.get(transaction=transaction)
    poll_data = poll_snapshot.to_dict()

    # 2. Modify state based on current values
    # ... business logic ...

    # 3. Write atomically
    transaction.update(poll_ref, update_data)

    return result_data
```

### Problem 4: Poll Start Notifications ❌ NOT IMPLEMENTED
**Status**: Not started - requires Firebase Cloud Messaging setup

**What's Needed**:
1. Set up FCM in Flask backend
2. Store device tokens in user profiles
3. Send notifications when poll is created
4. Filter recipients to only selected members
5. Include both in-app alerts and push notifications

## API Changes

### Phase 1 Vote Response (POST /polls/<poll_id>/phase1-vote)
Now returns:
```json
{
  "message": "Phase 1 vote recorded",
  "locked_in_count": 3,
  "total_members": 5,
  "visible_candidates": ["Menu A", "Menu B", "Menu C", "Menu D", "Menu E"],
  "replacement": "Menu F",  // Only present if rejection triggered replacement
  "approved": ["Menu A", "Menu C"]
}
```

### Phase 2 Vote Response (POST /polls/<poll_id>/phase2-vote)
Now returns:
```json
{
  "message": "Phase 2 vote recorded",
  "locked_in_count": 4,
  "total_members": 5,
  "selected": "Menu A"
}
```

## Data Model Updates

### Poll Document Structure
New/Modified fields:
```javascript
{
  // ... existing fields ...
  "removedCandidates": [],  // NEW: Globally rejected menu names
  "allCandidates": [        // Existing: Full ranked list
    {"name": "...", "ranking": 0, "score": 0.95},
    // ...
  ],
  "visibleCandidates": [...], // Existing: Currently shown 5 menus
  "lockedInUsers": [...]      // Existing: Users who locked their vote
}
```

## Testing Recommendations

### 1. Test Concurrent Voting
- Have multiple users vote simultaneously in Phase 1
- Verify no votes are lost or overwritten
- Check that lock-in count increments correctly

### 2. Test Reject-and-Replace
- Create poll with at least 10 candidates
- Have user reject one menu
- Verify:
  - Rejected menu disappears from visible list
  - Replacement appears (6th best ranked)
  - Exactly 5 menus remain visible
  - Rejected menu never reappears
  - User cannot reject a second menu

### 3. Test Early Phase Transition
- Create poll with 3 members
- Have all 3 lock in their Phase 1 votes
- Verify immediate transition to Phase 2 (don't wait for timer)
- Repeat for Phase 2 → poll closure

### 4. Test Edge Cases
- Reject when fewer than 5 candidates remain in pool
- All users reject different menus
- User tries to reject menu that's already been rejected by others
- User tries to change their rejection target

## Server Status
✅ Server running on http://127.0.0.0:5001
✅ Firestore transactions enabled
✅ LLM recommendations active (290 food items loaded)

## Next Steps
1. Test the transaction-based voting with Android app
2. Verify replacement logic works correctly in real polls
3. Implement Problem 4 (FCM notifications) if required
4. Monitor server logs for any transaction errors during concurrent voting
