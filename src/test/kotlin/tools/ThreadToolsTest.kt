package tools

import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class ThreadToolsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        // Clear any existing data by creating new agents and threads for each test
    }

    @Test
    fun `test formatMessagesAsXml`() {
        // Register agents
        val agent1 = Agent(id = "agent1", name = "Agent 1")
        val agent2 = Agent(id = "agent2", name = "Agent 2")
        ThreadManager.registerAgent(agent1)
        ThreadManager.registerAgent(agent2)

        // Create a thread
        val thread = ThreadManager.createThread(
            name = "Test Thread",
            creatorId = "agent1",
            participantIds = listOf("agent2")
        )

        // Send messages
        val message1 = ThreadManager.sendMessage(
            threadId = thread?.id ?: "",
            senderId = "agent1",
            content = "Hello, agent2!",
            mentions = listOf("agent2")
        )

        val message2 = ThreadManager.sendMessage(
            threadId = thread?.id ?: "",
            senderId = "agent2",
            content = "Hi, agent1!",
            mentions = listOf("agent1")
        )

        // Format messages as XML
        val xml = ThreadTools.formatMessagesAsXml(listOf(message1!!, message2!!))

        // Verify XML structure
        assertTrue(xml.contains("<messages>"))
        assertTrue(xml.contains("<thread id=\"${thread?.id}\" name=\"Test Thread\">"))
        assertTrue(xml.contains("<status>open</status>"))
        assertTrue(xml.contains("<participants>"))
        assertTrue(xml.contains("<participant id=\"agent1\" name=\"Agent 1\" />"))
        assertTrue(xml.contains("<participant id=\"agent2\" name=\"Agent 2\" />"))
        assertTrue(xml.contains("<messages>"))
        assertTrue(xml.contains("<message id=\"${message1.id}\""))
        assertTrue(xml.contains("<sender id=\"agent1\" name=\"Agent 1\" />"))
        assertTrue(xml.contains("<content>Hello, agent2!</content>"))
        assertTrue(xml.contains("<mentions>"))
        assertTrue(xml.contains("<mention id=\"agent2\" name=\"Agent 2\" />"))
        assertTrue(xml.contains("<message id=\"${message2.id}\""))
        assertTrue(xml.contains("<sender id=\"agent2\" name=\"Agent 2\" />"))
        assertTrue(xml.contains("<content>Hi, agent1!</content>"))
        assertTrue(xml.contains("</messages>"))
    }

    @Test
    fun `test thread operations end-to-end`() {
        // Register agents
        val agent1 = Agent(id = "agent1", name = "Agent 1")
        val agent2 = Agent(id = "agent2", name = "Agent 2")
        val agent3 = Agent(id = "agent3", name = "Agent 3")

        ThreadManager.registerAgent(agent1)
        ThreadManager.registerAgent(agent2)
        ThreadManager.registerAgent(agent3)

        // Create a thread
        val thread = ThreadManager.createThread(
            name = "Discussion Thread",
            creatorId = "agent1",
            participantIds = listOf("agent2")
        )

        assertNotNull(thread)
        assertEquals(2, thread?.participants?.size)

        // Add a participant
        val addSuccess = ThreadManager.addParticipant(
            threadId = thread?.id ?: "",
            participantId = "agent3"
        )

        assertTrue(addSuccess)
        assertEquals(3, ThreadManager.getThread(thread?.id ?: "")?.participants?.size)

        // Send messages
        val message1 = ThreadManager.sendMessage(
            threadId = thread?.id ?: "",
            senderId = "agent1",
            content = "Welcome to the discussion!",
            mentions = listOf("agent2", "agent3")
        )

        assertNotNull(message1)
        assertEquals(2, message1?.mentions?.size)

        // Close the thread
        val closeSuccess = ThreadManager.closeThread(
            threadId = thread?.id ?: "",
            summary = "Productive discussion completed"
        )

        assertTrue(closeSuccess)
        val closedThread = ThreadManager.getThread(thread?.id ?: "")
        assertTrue(closedThread?.isClosed ?: false)
        assertEquals("Productive discussion completed", closedThread?.summary)
    }
}
