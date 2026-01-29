package com.bhanit.apps.echo.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val driverClassName = config.property("storage.driverClassName").getString()
        val jdbcUrl = config.property("storage.jdbcUrl").getString()
        val username = config.property("storage.username").getString()
        val password = config.property("storage.password").getString()
        val maxPoolSize = config.propertyOrNull("storage.maxPoolSize")?.getString()?.toInt() ?: 3

        val database = Database.connect(hikari(driverClassName, jdbcUrl, username, password, maxPoolSize))
    }

    private fun hikari(driver: String, url: String, user: String, pass: String, maxPoolSize: Int): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = driver
        config.jdbcUrl = url
        config.username = user
        config.password = pass
        config.maximumPoolSize = maxPoolSize
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
