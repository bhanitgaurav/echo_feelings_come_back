package com.bhanit.apps.echo.core.util

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateTimeUtils {
    fun formatMessageTime(epochMillis: Long): String {
        try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            val isToday = localDateTime.date == now.date
            
            // Format time manually since Kotlinx-datetime doesn't have DateTimeFormatter yet in commonMain easily without platform specific code or extra libs.
            // We'll do basic string formatting.
            
            val hour = localDateTime.hour
            val minute = localDateTime.minute.toString().padStart(2, '0')
            val period = if (hour < 12) "AM" else "PM"
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            
            val timeString = "$hour12:$minute $period"

            return if (isToday) {
                "Today, $timeString"
            } else {
                val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                val day = localDateTime.dayOfMonth
                "$month $day, $timeString"
            }
        } catch (e: Exception) {
            return "Unknown"
        }
    }

    fun formatDateShort(epochMillis: Long): String {
        try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val day = localDateTime.dayOfMonth
            return "$month $day"
        } catch (e: Exception) {
            return "Unknown"
        }
    }

    fun formatDateForHeader(
        epochMillis: Long, 
        now: LocalDateTime? = null, 
        yesterday: LocalDateTime? = null
    ): String {
        try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val tz = TimeZone.currentSystemDefault()
            val localDateTime = instant.toLocalDateTime(tz)
            
            // Use provided values or calculate (caching optimization)
            val currentDate = now ?: Clock.System.now().toLocalDateTime(tz)
            val yesterdayDate = yesterday ?: Clock.System.now().minus(1, DateTimeUnit.DAY, tz).toLocalDateTime(tz)

            return when {
                localDateTime.date == currentDate.date -> "Today"
                localDateTime.date == yesterdayDate.date -> "Yesterday"
                localDateTime.year == currentDate.year -> {
                    val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    val day = localDateTime.dayOfMonth
                    "$month $day"
                }
                else -> {
                    val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    val day = localDateTime.dayOfMonth
                    "$month $day, ${localDateTime.year}"
                }
            }
        } catch (e: Exception) {
            return "Earlier"
        }
    }

    /**
     * Converts a picker-selected date (which is usually UTC midnight) to the Start of Day
     * in the user's current system timezone.
     */
    fun getStartOfDayInUserTimeZone(epochMillis: Long): Long {
        try {
            // 1. Interpret the input millis as a Date in UTC (since picker gives UTC midnight)
            val utcInstant = Instant.fromEpochMilliseconds(epochMillis)
            val utcDate = utcInstant.toLocalDateTime(TimeZone.UTC).date

            // 2. Create a LocalDateTime at 00:00:00 on that Date
            val startOfDay = LocalDateTime(utcDate, LocalTime(0, 0, 0, 0))

            // 3. Convert that Local Time to an Instant in the System TimeZone
            return startOfDay.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } catch (e: Exception) {
            return epochMillis // Fallback
        }
    }

    /**
     * Converts a picker-selected date (which is usually UTC midnight) to the End of Day (23:59:59.999)
     * in the user's current system timezone.
     */
    fun getEndOfDayInUserTimeZone(epochMillis: Long): Long {
        try {
             // 1. Interpret the input millis as a Date in UTC
            val utcInstant = Instant.fromEpochMilliseconds(epochMillis)
            val utcDate = utcInstant.toLocalDateTime(TimeZone.UTC).date

            // 2. Create a LocalDateTime at 23:59:59.999 on that Date
            val endOfDay = LocalDateTime(utcDate, LocalTime(23, 59, 59, 999_999_999))

            // 3. Convert that Local Time to an Instant in the System TimeZone
            return endOfDay.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } catch (e: Exception) {
            return epochMillis // Fallback
        }
    }
}
