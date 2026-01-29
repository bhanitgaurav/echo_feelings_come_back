package com.bhanit.apps.echo.data.table

import org.jetbrains.exposed.dao.id.IntIdTable
import java.time.Instant
import org.jetbrains.exposed.sql.javatime.timestamp

object SystemConfig : IntIdTable("system_config") {
    val key = varchar("config_key", 100).uniqueIndex()
    val value = text("config_value")
    val updatedAt = timestamp("updated_at").default(Instant.now())
}
