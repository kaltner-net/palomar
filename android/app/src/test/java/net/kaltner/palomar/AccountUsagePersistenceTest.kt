package net.kaltner.palomar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountUsagePersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        listOf("usage-host-a", "usage-host-b").forEach { hostId ->
            context.getSharedPreferences(HostStore.preferenceFile(hostId), Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    @Test
    fun forceCloseRelaunchRestoresEveryWindowWithoutCrossHostLeakage() {
        val usage = AccountUsage(
            providers = mapOf(
                PROVIDER_CODEX to ProviderAccountUsage(
                    available = true,
                    rateLimits = RateLimitSnapshot(
                        windows = listOf(
                            RateLimitWindow("short", "Short limit", 25.0, resetsAt = 1_800_000_000),
                            RateLimitWindow("long", "Long limit", 50.0, resetsAt = 1_800_086_400),
                        ),
                    ),
                ),
                PROVIDER_CLAUDE_CODE to ProviderAccountUsage(
                    available = true,
                    rateLimits = RateLimitSnapshot(primary = RateLimitWindow(usedPercent = 10.0)),
                ),
            ),
        )
        PreferenceStore(context, "usage-host-a").setAccountUsage(usage)
        PreferenceStore(context, "usage-host-b").setAccountUsage(
            AccountUsage(
                mapOf(
                    PROVIDER_CODEX to ProviderAccountUsage(
                        available = true,
                        rateLimits = RateLimitSnapshot(
                            windows = listOf(RateLimitWindow("host-b", usedPercent = 5.0)),
                        ),
                    ),
                ),
            ),
        )

        val relaunched = PreferenceStore(context, "usage-host-a").loadAccountUsage()
        assertEquals(2, accountUsageWindows(relaunched.providers[PROVIDER_CODEX]).size)
        assertEquals("primary", accountUsageWindows(relaunched.providers[PROVIDER_CLAUDE_CODE]).single().id)
        assertTrue(relaunched.providers.values.all { it.stale })
        assertEquals(
            "host-b",
            accountUsageWindows(
                PreferenceStore(context, "usage-host-b").loadAccountUsage().providers[PROVIDER_CODEX],
            ).single().id,
        )

        HostStore(context).forget("usage-host-a")
        assertFalse(PreferenceStore(context, "usage-host-a").loadAccountUsage().providers.isNotEmpty())
        assertTrue(PreferenceStore(context, "usage-host-b").loadAccountUsage().providers.isNotEmpty())
    }
}
