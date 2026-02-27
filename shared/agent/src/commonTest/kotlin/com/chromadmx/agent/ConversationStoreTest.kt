package com.chromadmx.agent

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationStoreTest {

    @Test
    fun emptyStoreHasNoMessages() {
        val store = ConversationStore()
        assertEquals(0, store.size)
        assertEquals(emptyList(), store.messages.value)
    }

    @Test
    fun addUserMessageAppendsToHistory() {
        val store = ConversationStore()
        store.addUserMessage("hello")

        assertEquals(1, store.size)
        assertEquals(ChatRole.USER, store.messages.value[0].role)
        assertEquals("hello", store.messages.value[0].content)
    }

    @Test
    fun addAssistantMessageAppendsToHistory() {
        val store = ConversationStore()
        store.addAssistantMessage("I can help with that")

        assertEquals(1, store.size)
        assertEquals(ChatRole.ASSISTANT, store.messages.value[0].role)
        assertEquals("I can help with that", store.messages.value[0].content)
    }

    @Test
    fun addSystemMessageAppendsToHistory() {
        val store = ConversationStore()
        store.addSystemMessage("Agent error: timeout")

        assertEquals(1, store.size)
        assertEquals(ChatRole.SYSTEM, store.messages.value[0].role)
        assertEquals("Agent error: timeout", store.messages.value[0].content)
    }

    @Test
    fun multipleMessagesPreserveOrder() {
        val store = ConversationStore()
        store.addUserMessage("set lights to red")
        store.addAssistantMessage("Setting lights to red")
        store.addUserMessage("now make them blue")

        assertEquals(3, store.size)
        assertEquals("set lights to red", store.messages.value[0].content)
        assertEquals("Setting lights to red", store.messages.value[1].content)
        assertEquals("now make them blue", store.messages.value[2].content)
    }

    @Test
    fun getRecentReturnsLastNMessages() {
        val store = ConversationStore()
        repeat(15) { i -> store.addUserMessage("message $i") }

        val recent = store.getRecent(5)
        assertEquals(5, recent.size)
        assertEquals("message 10", recent[0].content)
        assertEquals("message 14", recent[4].content)
    }

    @Test
    fun getRecentWithDefaultCountReturns10() {
        val store = ConversationStore()
        repeat(20) { i -> store.addUserMessage("msg $i") }

        val recent = store.getRecent()
        assertEquals(10, recent.size)
        assertEquals("msg 10", recent[0].content)
        assertEquals("msg 19", recent[9].content)
    }

    @Test
    fun getRecentReturnsAllWhenFewerThanCount() {
        val store = ConversationStore()
        store.addUserMessage("only one")

        val recent = store.getRecent(10)
        assertEquals(1, recent.size)
        assertEquals("only one", recent[0].content)
    }

    @Test
    fun maxMessagesCapsHistory() {
        val store = ConversationStore(maxMessages = 5)
        repeat(10) { i -> store.addUserMessage("msg $i") }

        assertEquals(5, store.size)
        // Oldest messages should have been evicted
        assertEquals("msg 5", store.messages.value[0].content)
        assertEquals("msg 9", store.messages.value[4].content)
    }

    @Test
    fun clearResetsAllMessages() {
        val store = ConversationStore()
        store.addUserMessage("hello")
        store.addAssistantMessage("hi there")
        assertEquals(2, store.size)

        store.clear()
        assertEquals(0, store.size)
        assertEquals(emptyList(), store.messages.value)
    }

    @Test
    fun stateFlowReflectsLatestState() = runTest {
        val store = ConversationStore()

        assertEquals(emptyList(), store.messages.value)

        store.addUserMessage("test")
        assertEquals(1, store.messages.value.size)

        store.addAssistantMessage("response")
        assertEquals(2, store.messages.value.size)

        store.clear()
        assertEquals(0, store.messages.value.size)
    }

    @Test
    fun maxMessagesOfOneKeepsOnlyLatest() {
        val store = ConversationStore(maxMessages = 1)
        store.addUserMessage("first")
        store.addUserMessage("second")

        assertEquals(1, store.size)
        assertEquals("second", store.messages.value[0].content)
    }

    @Test
    fun getRecentOnEmptyStoreReturnsEmpty() {
        val store = ConversationStore()
        val recent = store.getRecent(10)
        assertTrue(recent.isEmpty())
    }

    @Test
    fun boundaryExactlyAtMaxMessages() {
        val store = ConversationStore(maxMessages = 3)
        store.addUserMessage("a")
        store.addUserMessage("b")
        store.addUserMessage("c")

        assertEquals(3, store.size)
        assertEquals("a", store.messages.value[0].content)
        assertEquals("c", store.messages.value[2].content)

        // Adding one more should evict the oldest
        store.addUserMessage("d")
        assertEquals(3, store.size)
        assertEquals("b", store.messages.value[0].content)
        assertEquals("d", store.messages.value[2].content)
    }

    @Test
    fun mixedRolesAllPreserved() {
        val store = ConversationStore()
        store.addUserMessage("hello")
        store.addAssistantMessage("hi")
        store.addSystemMessage("Tool executed")
        store.addUserMessage("thanks")

        val messages = store.messages.value
        assertEquals(4, messages.size)
        assertEquals(ChatRole.USER, messages[0].role)
        assertEquals(ChatRole.ASSISTANT, messages[1].role)
        assertEquals(ChatRole.SYSTEM, messages[2].role)
        assertEquals(ChatRole.USER, messages[3].role)
    }
}
