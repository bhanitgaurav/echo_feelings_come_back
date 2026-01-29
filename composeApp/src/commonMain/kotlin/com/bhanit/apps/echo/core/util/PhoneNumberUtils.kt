package com.bhanit.apps.echo.core.util

object PhoneNumberUtils {
    fun normalizePhoneNumber(phone: String, defaultCountryCode: String = "+91"): String {
        // 1. Remove all non-numeric characters except '+'
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        
        if (cleaned.isEmpty()) return phone

        // 2. Handle existing '+'
        if (cleaned.startsWith("+")) {
            return cleaned
        }

        // 3. Handle numbers that might start with Country Code but missing '+' (e.g., 9198765...)
        // Heuristic: If length is > 10 and starts with '91' (for India default), prepend +
        // Flexible for future: Check if starts with defaultCountryCode digits
        val countryCodeDigits = defaultCountryCode.removePrefix("+")
        if (cleaned.startsWith(countryCodeDigits) && cleaned.length > 10) {
             return "+$cleaned"
        }

        // 4. Handle leading zero (e.g. 09876...)
        if (cleaned.startsWith("0")) {
            val withoutZero = cleaned.removePrefix("0")
            return "$defaultCountryCode$withoutZero"
        }

        // 5. Default case: Append country code (e.g. 9876543210 -> +9198765...)
        return "$defaultCountryCode$cleaned"
    }
}
