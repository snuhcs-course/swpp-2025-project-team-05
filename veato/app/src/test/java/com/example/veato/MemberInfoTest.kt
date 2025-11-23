package com.example.veato

import org.junit.Assert
import org.junit.Test
import kotlin.hashCode
import kotlin.toString

class MemberInfoTest {
    @Test
    fun `constructor should store values correctly`() {
        val member = MemberInfo(id = "123", name = "Alice")

        Assert.assertEquals("123", member.id)
        Assert.assertEquals("Alice", member.name)
    }

    @Test
    fun `equals should return true for identical objects`() {
        val m1 = MemberInfo("1", "John")
        val m2 = MemberInfo("1", "John")

        Assert.assertEquals(m1, m2)
    }

    @Test
    fun `equals should return false for different objects`() {
        val m1 = MemberInfo("1", "John")
        val m2 = MemberInfo("2", "Adam")

        Assert.assertNotEquals(m1, m2)
    }

    @Test
    fun `hashCode should be consistent with equals`() {
        val m1 = MemberInfo("123", "Tom")
        val m2 = MemberInfo("123", "Tom")

        Assert.assertEquals(m1.hashCode(), m2.hashCode())
    }

    @Test
    fun `copy should create a new object with updated fields`() {
        val original = MemberInfo("1", "Lisa")
        val copy = original.copy(name = "Jenny")

        Assert.assertEquals("1", copy.id)
        Assert.assertEquals("Jenny", copy.name)
        Assert.assertNotEquals(original, copy)
    }

    @Test
    fun `toString should include class properties`() {
        val member = MemberInfo("55", "Mark")
        val str = member.toString()

        Assert.assertTrue(str.contains("55"))
        Assert.assertTrue(str.contains("Mark"))
        Assert.assertTrue(str.contains("MemberInfo"))
    }
}