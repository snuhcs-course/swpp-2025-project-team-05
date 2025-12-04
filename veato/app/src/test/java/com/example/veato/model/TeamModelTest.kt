package com.example.veato

import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito

class TeamModelTest {
    @Test
    fun `test default constructor creates empty fields`() {
        val team = Team()

        assertEquals("", team.id)
        assertEquals("", team.name)
        assertEquals("", team.leaderId)
        assertTrue(team.members.isEmpty())
        assertNotNull(team.createdAt)
        assertNull(team.lastMealPoll)
    }

    @Test
    fun `test parameterized constructor sets all values correctly`() {
        val mockTimestamp = Mockito.mock(Timestamp::class.java)
        val team = Team(
            id = "team123",
            name = "Lunch Lovers",
            leaderId = "userA",
            members = listOf("userA", "userB"),
            occasionType = "Lunch",
            createdAt = mockTimestamp,
            lastMealPoll = "poll001"
        )

        assertEquals("team123", team.id)
        assertEquals("Lunch Lovers", team.name)
        assertEquals("userA", team.leaderId)
        assertEquals(listOf("userA", "userB"), team.members)
        assertEquals("Lunch", team.occasionType)
        assertEquals(mockTimestamp, team.createdAt)
        assertEquals("poll001", team.lastMealPoll)
    }

    @Test
    fun `test equality of identical teams`() {
        val timestamp = Timestamp.now()
        val team1 = Team(
            id = "t1",
            name = "TeamX",
            leaderId = "leader1",
            members = listOf("a", "b"),
            occasionType = "Dinner",
            createdAt = timestamp,
            lastMealPoll = "pollA"
        )
        val team2 = Team(
            id = "t1",
            name = "TeamX",
            leaderId = "leader1",
            members = listOf("a", "b"),
            occasionType = "Dinner",
            createdAt = timestamp,
            lastMealPoll = "pollA"
        )

        assertEquals(team1, team2)
        assertEquals(team1.hashCode(), team2.hashCode())
    }

    @Test
    fun `test copy function creates modified instance`() {
        val original = Team(id = "1", name = "Original", leaderId = "L1")
        val copied = original.copy(name = "Modified")

        assertEquals("1", copied.id)
        assertEquals("Modified", copied.name)
        assertEquals("L1", copied.leaderId)
        assertNotEquals(original, copied)
    }

    @Test
    fun `test toString contains key fields`() {
        val team = Team(id = "abc", name = "TeamY", leaderId = "leaderZ")
        val str = team.toString()

        assertTrue(str.contains("abc"))
        assertTrue(str.contains("TeamY"))
        assertTrue(str.contains("leaderZ"))
    }
}
