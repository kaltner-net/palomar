package net.kaltner.palomar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemePersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val hostIds = listOf("theme-host-neon", "theme-host-obsidian")

    @Before
    fun clear() {
        hostIds.forEach { hostId ->
            context.getSharedPreferences(HostStore.preferenceFile(hostId), Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    @Test
    fun recreationRestoresNewThemesWithoutCrossHostLeakage() {
        PreferenceStore(context, hostIds[0]).setThemeId(ThemeId.NeonWave)
        PreferenceStore(context, hostIds[1]).setThemeId(ThemeId.Obsidian)

        assertEquals(ThemeId.NeonWave, PreferenceStore(context, hostIds[0]).load().themeId)
        assertEquals(ThemeId.Obsidian, PreferenceStore(context, hostIds[1]).load().themeId)
    }

    @Test
    fun forgettingAHostClearsOnlyItsTheme() {
        PreferenceStore(context, hostIds[0]).setThemeId(ThemeId.NeonWave)
        PreferenceStore(context, hostIds[1]).setThemeId(ThemeId.Obsidian)

        HostStore(context).forget(hostIds[0])

        assertEquals(ThemeId.Palomar, PreferenceStore(context, hostIds[0]).load().themeId)
        assertEquals(ThemeId.Obsidian, PreferenceStore(context, hostIds[1]).load().themeId)
    }
}
