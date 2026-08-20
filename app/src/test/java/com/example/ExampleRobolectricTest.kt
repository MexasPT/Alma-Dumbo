package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SupportedLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Alma Dumbo", appName)
    }

    @Test
    fun `verify supported languages catalog contains required languages`() {
        val codes = SupportedLanguages.ALL.map { it.code }
        // Core & New Requested Languages
        assertTrue("Contains Portuguese", codes.contains("pt"))
        assertTrue("Contains Spanish / Castilian", codes.contains("es"))
        assertTrue("Contains French", codes.contains("fr"))
        assertTrue("Contains English", codes.contains("en"))
        assertTrue("Contains Italian", codes.contains("it"))
        assertTrue("Contains German", codes.contains("de"))
        assertTrue("Contains Dutch", codes.contains("nl"))
        assertTrue("Contains Croatian", codes.contains("hr"))
        assertTrue("Contains Albanian", codes.contains("sq"))
        assertTrue("Contains Danish", codes.contains("da"))
        assertTrue("Contains Finnish", codes.contains("fi"))
        assertTrue("Contains Tamazight", codes.contains("ber"))
        
        // Asian, African and Eastern European languages
        assertTrue("Contains Urdu/Pakistani", codes.contains("ur"))
        assertTrue("Contains Hindi/Indian", codes.contains("hi"))
        assertTrue("Contains Bengali/Bangladesh", codes.contains("bn"))
        assertTrue("Contains Arabic", codes.contains("ar"))
        assertTrue("Contains Ukrainian", codes.contains("uk"))
        assertTrue("Contains Moldavian", codes.contains("mo"))
        assertTrue("Contains Romanian", codes.contains("ro"))
        assertTrue("Contains Kimbundu", codes.contains("kmb"))
        assertTrue("Contains Korean", codes.contains("ko"))
        assertTrue("Contains Japanese", codes.contains("ja"))
        assertTrue("Contains Thai", codes.contains("th"))
        assertTrue("Contains Vietnamese", codes.contains("vi"))
        assertTrue("Contains Chinese", codes.contains("zh"))
        assertTrue("Contains Russian", codes.contains("ru"))
    }
}
