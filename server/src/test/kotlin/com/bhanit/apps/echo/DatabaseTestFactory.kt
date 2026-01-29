package com.bhanit.apps.echo

import com.bhanit.apps.echo.data.table.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.addLogger

object DatabaseTestFactory {
    fun init() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
        transaction {
            addLogger(org.jetbrains.exposed.sql.StdOutSqlLogger)
            SchemaUtils.drop(Replies, Messages, Users, Emotions, UserRelationLimits)
            SchemaUtils.create(
                Emotions, // First (referenced by Messages)
                Users,    // Referenced by Messages, Connections
                Messages, // Referenced by Replies
                Replies,
                UserRelationLimits
            )
        }
        }
    }
}
