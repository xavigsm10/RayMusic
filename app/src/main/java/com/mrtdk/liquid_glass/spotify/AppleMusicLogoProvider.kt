package com.mrtdk.liquid_glass.spotify

/**
 * Compatibility wrapper delegating directly to unified AppleMusicArtistProvider.
 */
object AppleMusicLogoProvider {
    fun cleanArtistName(rawName: String): String = AppleMusicArtistProvider.cleanArtistName(rawName)

    fun getCachedLogoUrl(artistName: String): String? {
        return AppleMusicArtistProvider.getCachedLogoUrl(artistName)
    }

    suspend fun getArtistLogoUrl(artistName: String): String? {
        return AppleMusicArtistProvider.getArtistLogoUrl(artistName)
    }

    fun prefetch(artistName: String) {
        AppleMusicArtistProvider.prefetch(artistName)
    }
}
