package com.echo.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Test

class SearchTest {
    @Test
    fun testArtistPage() {
        runBlocking {
            println("Testing Artist Page for Michael Jackson:")
            val result = YouTube.artist("UCoIOOL7QKuBhQHVKL8y7BEQ")
            if (result.isSuccess) {
                val page = result.getOrNull()!!
                println("Artist: ${page.artist.title}")
                println("Sections: ${page.sections.size}")
                page.sections.forEach { sec ->
                    println(" - Section '${sec.title}' has ${sec.items.size} items")
                }
            } else {
                println("Artist Page failed!")
                result.exceptionOrNull()?.printStackTrace()
            }
        }
    }
}
