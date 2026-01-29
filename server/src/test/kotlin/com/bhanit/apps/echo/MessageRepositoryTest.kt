package com.bhanit.apps.echo

import com.bhanit.apps.echo.data.model.MessageDTO
import com.bhanit.apps.echo.data.repository.MessageRepositoryImpl
import com.bhanit.apps.echo.data.table.Messages
import com.bhanit.apps.echo.data.table.Replies
import com.bhanit.apps.echo.data.table.Users
import com.bhanit.apps.echo.data.table.Emotions
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.addLogger
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking

class MessageRepositoryTest {

    private lateinit var repository: MessageRepositoryImpl
    private val senderId = UUID.randomUUID()
    private val receiverId = UUID.randomUUID()

    @Before
    fun setup() {
        DatabaseTestFactory.init()
        repository = MessageRepositoryImpl()
        
        transaction {
            addLogger(org.jetbrains.exposed.sql.StdOutSqlLogger)
            // Seed Data
            Users.insert {
                it[id] = senderId
                it[username] = "sender"
                it[phoneNumber] = "123"
                it[phoneHash] = "hash1"
                it[createdAt] = java.time.Instant.now()
            }
            Users.insert {
                it[id] = receiverId
                it[username] = "receiver"
                it[phoneNumber] = "456"
                it[phoneHash] = "hash2"
                it[createdAt] = java.time.Instant.now()
            }
            Emotions.insert {
                it[id] = "JOY"
                it[displayName] = "Joy"
                it[colorHex] = 0xFFFFFF
                it[exampleText] = "Example"
                it[createdAt] = java.time.Instant.now()
                // Add defaults ...
            }
        }
    }
    
    @After
    fun tearDown() {
         transaction {
             Replies.deleteAll()
             Messages.deleteAll()
             Users.deleteAll()
         }
    }

    @Test
    fun testGetSentMessagesWithReplies() = runBlocking {
        // 1. Create a message
        val msgId = repository.sendMessage(senderId, receiverId, "JOY", "Hello", false)
        
        // 2. Reply to it
        repository.replyToMessage(msgId, receiverId, "Hi back")
        
        // 3. Fetch with onlyReplied=true
        val result = repository.getSentMessages(senderId, null, 10, 0, onlyReplied = true)
        
        assertEquals(1, result.size)
        assertEquals("Hi back", result[0].replyContent)
        assertEquals(msgId.toString(), result[0].id)
    }
    
    @Test
    fun testGetSentMessagesWithoutReplies_ShouldBeEmpty() = runBlocking {
        // 1. Create a message but NO reply
        repository.sendMessage(senderId, receiverId, "JOY", "Hello No Reply", false)
        
        // 2. Fetch with onlyReplied=true
        val result = repository.getSentMessages(senderId, null, 10, 0, onlyReplied = true)
        
        assertEquals(0, result.size)
    }
}
