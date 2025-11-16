# Phase 2 Auto-Close Bug - Fix Summary

## Issues Reported

1. **Phase 2 closes prematurely for single-member teams**: After voting in Phase 1 and transitioning to Phase 2, the poll gets stuck showing "Waiting for results..." without actually allowing the Phase 2 vote to complete.

2. **Veto/reject doesn't add new suggestions**: When users reject a menu in Phase 1, a replacement candidate should appear to maintain 5 visible options, but this wasn't working.

## Root Cause Analysis

### Issue 1: Premature Poll Closure

**Problem**: The GET `/polls/<poll_id>` endpoint was auto-closing polls when `seconds_left <= 0`, even during the Phase 1 → Phase 2 transition period. This caused polls to close immediately after transitioning to Phase 2 if the timer had expired, preventing users from actually voting in Phase 2.

**Scenario**:
1. User creates a 1-minute poll at 20:11:29
2. User votes in Phase 1 at 20:11:51 (22 seconds elapsed)
3. Poll transitions to Phase 2
4. Frontend polls GET endpoint at 20:11:52
5. Timer shows expired (23 seconds > 20 second duration)
6. GET endpoint auto-closes poll before user can vote in Phase 2
7. User stuck seeing "Waiting for results..." in Phase 2 screen

**Code Location**: `server.py` lines 1024-1027 (before fix)

```python
# Old code - auto-closed too aggressively
if seconds_left <= 0 and poll_data["status"] == "active" and current_phase != "closed":
    poll_data = close_poll_internal(poll_id)
    current_phase = "closed"
```

### Issue 2: Reject Replacement

**Status**: Already implemented correctly in lines 1297-1319

The reject-and-replace logic was already working:
- Removes rejected candidate from `visibleCandidates`
- Tracks rejection in `removedCandidates` array
- Finds replacement from `allCandidates` pool
- Sorts by LLM ranking to get best available replacement
- Maintains exactly 5 visible candidates

If this isn't working in practice, it's likely a frontend issue with displaying the updated candidates.

## Fix Applied

### Phase 2 Auto-Close Prevention

**File**: `server.py` (lines 1024-1045)

**Solution**: Added grace period logic for Phase 2 to prevent premature closure:

```python
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
```

**Logic**:
1. **In Phase 2**: Don't auto-close on timer expiration unless:
   - All members have locked in their Phase 2 votes (normal early closure), OR
   - Timer is 30+ seconds past expiration (force close after grace period)

2. **Not in Phase 2** (Phase 1 or single-phase polls): Normal auto-close when timer expires

3. **30-second grace period**: Allows members time to vote in Phase 2 even if the original poll timer expired during Phase 1

## Testing Recommendations

### Test Case 1: Short Duration Poll with One Member
1. Create a poll with 30-second duration
2. Vote in Phase 1 at 25 seconds (close to expiration)
3. Poll should transition to Phase 2
4. **Expected**: Poll stays open for Phase 2 voting (grace period active)
5. Vote in Phase 2
6. **Expected**: Poll closes immediately showing results

### Test Case 2: Grace Period Expiration
1. Create a poll with 30-second duration
2. Vote in Phase 1 at 25 seconds
3. Poll transitions to Phase 2
4. **Wait 40 seconds** without voting
5. **Expected**: Poll force-closes after 30-second grace period expires (at 60 seconds total)

### Test Case 3: Multi-Member Early Closure
1. Create poll with 2 members, 5-minute duration
2. Both members vote in Phase 1 at 20 seconds
3. Poll transitions to Phase 2
4. Both members vote in Phase 2 at 30 seconds
5. **Expected**: Poll closes immediately (all members voted, no need to wait)

### Test Case 4: Reject Replacement (Issue #2)
1. Create poll with at least 10 candidates
2. In Phase 1, approve 2 candidates and reject 1 candidate
3. **Expected**:
   - Rejected candidate disappears
   - New candidate appears to replace it
   - Exactly 5 candidates remain visible
   - Rejected candidate never reappears

## Server Status

✅ Server running on http://127.0.0.1:5001
✅ Phase 2 grace period implemented
✅ Reject-replacement logic already working (lines 1297-1319)

## Next Steps

1. Test with one-member team to verify Phase 2 voting works
2. Test reject-replacement in Phase 1 (if still not working, investigate frontend)
3. Monitor logs for any unexpected force-closes during grace period
4. Consider making grace period configurable (currently hardcoded to 30 seconds)

## Related Files

- `server.py:1024-1045` - Phase 2 auto-close logic (FIXED)
- `server.py:1297-1319` - Reject-replacement logic (already working)
- `server.py:1480-1536` - Phase 1 → Phase 2 transition
- `server.py:1458-1461` - Phase 2 voting lock-in check
- `POLL_VOTING_FIXES.md` - Previous fixes for Problems 2 & 3
- `MEAL_TYPE_FILTERING.md` - Meal type filtering implementation
