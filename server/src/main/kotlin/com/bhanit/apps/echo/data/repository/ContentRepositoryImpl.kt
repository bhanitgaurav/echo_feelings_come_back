package com.bhanit.apps.echo.data.repository

import com.bhanit.apps.echo.data.db.DatabaseFactory.dbQuery
import com.bhanit.apps.echo.data.table.AppContents
import com.bhanit.apps.echo.data.table.ContentType
import com.bhanit.apps.echo.domain.repository.AppContent
import com.bhanit.apps.echo.domain.repository.ContentRepository
import org.jetbrains.exposed.sql.*

class ContentRepositoryImpl : ContentRepository {
    override suspend fun getContent(type: ContentType): AppContent? = dbQuery {
        AppContents.selectAll().where { AppContents.id eq type }
            .map {
                AppContent(
                    type = it[AppContents.id],
                    content = it[AppContents.content],
                    updatedAt = it[AppContents.updatedAt]
                )
            }
            .singleOrNull()
    }

    override suspend fun updateContent(type: ContentType, content: String): Unit = dbQuery {
        // Upsert logic
        val exists = AppContents.selectAll().where { AppContents.id eq type }.count() > 0
        if (exists) {
            AppContents.update({ AppContents.id eq type }) {
                it[this.content] = content
                it[this.updatedAt] = java.time.Instant.now()
            }
        } else {
            AppContents.insert {
                it[this.id] = type
                it[this.content] = content
                it[this.updatedAt] = java.time.Instant.now()
            }
        }
    }
}
