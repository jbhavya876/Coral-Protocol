package org.coralprotocol.coralserver.mcptools

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*


import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.coralprotocol.coralserver.server.CoralAgentIndividualMcp

private val logger = KotlinLogging.logger {}

/**
 * Extension function to add the send message tool to a server.
 */
fun CoralAgentIndividualMcp.addSendMessageTool() {
    addTool(
        name = "send_message",
        description = "Send a message to a thread",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("threadId") {
                    put("type", "string")
                    put("description", "ID of the thread")
                }
                putJsonObject("content") {
                    put("type", "string")
                    put("description", "Content of the message")
                }
                putJsonObject("mentions") {
                    put("type", "array")
                    put("description", "List of agent IDs to mention in the message. You *must* mention an agent for them to be made aware of the message.")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                }
            },
            required = listOf("threadId", "content", "mentions")
        )
    ) { request ->
        handleSendMessage(request)
    }
}

/**
 * Handles the send message tool request.
 */
private suspend fun CoralAgentIndividualMcp.handleSendMessage(request: CallToolRequest): CallToolResult {
    try {
        val json = Json { ignoreUnknownKeys = true }
        val input = json.decodeFromString<SendMessageInput>(request.arguments.toString())

        val mcpPayload = buildJsonObject {
            put("threadId", input.threadId)
            put("content", input.content)
            put("mentions", Json.encodeToJsonElement(input.mentions))
            put("senderId", this@handleSendMessage.connectedAgentId)
        }

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val mcpUrl = AppConfigLoader.getApplication("default-app")?.mcpEndpoint
            ?: "http://127.0.0.1:6001/agent"

        val response: String = client.post(mcpUrl) {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(mcpPayload)
        }

        val responseJson = Json.parseToJsonElement(response).jsonObject
        val content = responseJson["content"]?.jsonPrimitive?.content ?: "No response"

        logger.info { "Message sent to MCP successfully. Response: $content" }

        return CallToolResult(
            content = listOf(TextContent(content))
        )

    } catch (e: Exception) {
        val errorMessage = "Error sending message: ${e.message}"
        logger.error(e) { errorMessage }
        return CallToolResult(
            content = listOf(TextContent(errorMessage))
        )
    }
}


