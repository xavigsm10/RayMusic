package com.echo.innertube.models

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class YouTubeLocale(
    val gl: String, // geolocation
    val hl: String, // host language
) {
    companion object {
        val SUPPORTED_REGIONS = setOf(
            "DZ", "AR", "AU", "AT", "AZ", "BH", "BD", "BY", "BE", "BO",
            "BA", "BR", "BG", "KH", "CA", "CL", "HK", "CO", "CR", "HR",
            "CY", "CZ", "DK", "DO", "EC", "EG", "SV", "EE", "FI", "FR",
            "GE", "DE", "GH", "GR", "GT", "HN", "HU", "IS", "IN", "ID",
            "IQ", "IE", "IL", "IT", "JM", "JP", "JO", "KZ", "KE", "KR",
            "KW", "LA", "LV", "LB", "LY", "LI", "LT", "LU", "MK", "MY",
            "MT", "MX", "ME", "MA", "NP", "NL", "NZ", "NI", "NG", "NO",
            "OM", "PK", "PA", "PG", "PY", "PE", "PH", "PL", "PT", "PR",
            "QA", "RO", "RU", "SA", "SN", "RS", "SG", "SK", "SI", "ZA",
            "ES", "LK", "SE", "CH", "TW", "TZ", "TH", "TN", "TR", "UG",
            "UA", "AE", "GB", "US", "UY", "VE", "VN", "YE", "ZW"
        )

        fun sanitize(gl: String?, hl: String?): YouTubeLocale {
            val upperGl = gl?.trim()?.uppercase(Locale.US)
            val sanitizedGl = if (upperGl != null && upperGl.length == 2 && upperGl in SUPPORTED_REGIONS) {
                upperGl
            } else {
                "US"
            }

            val sanitizedHl = hl?.trim()?.takeIf {
                it.isNotBlank() && it.length in 2..15 && it != "null" && it != "undefined"
            } ?: "en"

            return YouTubeLocale(gl = sanitizedGl, hl = sanitizedHl)
        }

        fun fromSystem(): YouTubeLocale {
            val defaultLocale = Locale.getDefault()
            val country = defaultLocale.country.uppercase(Locale.US).takeIf { it in SUPPORTED_REGIONS }
                ?: if (defaultLocale.language.length == 2 && defaultLocale.language.uppercase(Locale.US) in SUPPORTED_REGIONS) {
                    defaultLocale.language.uppercase(Locale.US)
                } else {
                    "US"
                }
            val language = defaultLocale.toLanguageTag().takeIf { it.isNotBlank() && it != "null" }
                ?: defaultLocale.language.takeIf { it.isNotBlank() && it != "null" }
                ?: "en"
            return sanitize(country, language)
        }
    }
}
