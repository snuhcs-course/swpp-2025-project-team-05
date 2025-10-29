package com.example.veato.utils

import com.example.veato.Team
import org.junit.Assert.*
import org.junit.Test

class TeamUtilsTest {

    @Test
    fun `valid team name returns true`() {
        val result = TeamUtils.isTeamNameValid("Team Alpha")
        assertTrue(result)
    }

    @Test
    fun `empty team name returns false`() {
        val result = TeamUtils.isTeamNameValid("")
        assertFalse(result)
    }

    @Test
    fun `team name with only spaces returns false`() {
        val result = TeamUtils.isTeamNameValid("   ")
        assertFalse(result)
    }

    @Test
    fun `buildTeam creates team with correct fields`() {
        val team = TeamUtils.buildTeam("Team Beta", "user123")
        assertEquals("Team Beta", team.name)
        assertEquals("user123", team.leaderId)
        assertTrue(team.members.contains("user123"))
        assertEquals("temp-id", team.id)
    }
}