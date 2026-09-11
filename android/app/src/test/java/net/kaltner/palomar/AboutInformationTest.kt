package net.kaltner.palomar

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutInformationTest {
    @Test
    fun clientVersionIsDerivedFromSharedReleaseProperties() {
        val releaseFile =
            generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
                .map { File(it, "palomar-release.properties") }
                .first { it.isFile }
        val releaseVersion =
            releaseFile.readLines().first { it.startsWith("palomarVersion=") }.substringAfter('=')
        val releaseBuild =
            releaseFile.readLines().first { it.startsWith("releaseBuild=") }.substringAfter('=').toBooleanStrict()

        assertEquals(releaseVersion, BuildConfig.VERSION_NAME)
        assertEquals(releaseBuild, BuildConfig.PALOMAR_RELEASE_BUILD)
        assertTrue(BuildConfig.PALOMAR_BUILD_COMMIT.isNotBlank())
    }

    @Test
    fun disconnectedAboutStillContainsAndroidClientBuild() {
        val information = aboutVersionInformation(null, false, "1.2.3", "abc123def456", false)

        assertEquals("Unavailable while disconnected", information.server)
        assertEquals("1.2.3 (development build) · abc123def456", information.client)
    }

    @Test
    fun disconnectedAboutLabelsRetainedServerVersionAsLastConnected() {
        val information = aboutVersionInformation("1.0.1", false, "1.0.2", "unknown", false)

        assertEquals("1.0.1 (last connected)", information.server)
        assertEquals("1.0.2 (development build)", information.client)
    }

    @Test
    fun differingServerAndClientVersionsStayDistinct() {
        val information = aboutVersionInformation("0.9.0", true, "1.0.2", "unknown", false)

        assertEquals("0.9.0", information.server)
        assertEquals("1.0.2 (development build)", information.client)
        assertFalse(information.server == information.client)
    }

    @Test
    fun officialReleaseBuildDoesNotUseDevelopmentLabel() {
        assertEquals("1.0.2 · abc123def456", clientBuildDescription("1.0.2", "abc123def456", true))
    }

    @Test
    fun aboutLinksUsePublicHttpsTargets() {
        assertEquals(
            listOf(
                "GitHub repository" to "https://github.com/kaltner-net/palomar",
                "Current releases" to "https://github.com/kaltner-net/palomar/releases",
                "License" to "https://github.com/kaltner-net/palomar/blob/main/LICENSE",
                "Third-party notices" to "https://github.com/kaltner-net/palomar/blob/main/THIRD_PARTY_NOTICES.md",
            ),
            palomarAboutLinks,
        )
        assertTrue(palomarAboutLinks.all { (_, url) -> url.startsWith("https://") })
        assertEquals("https://kaltner.net", KALTNER_WEBSITE_URL)
    }

    @Test
    fun productAndNotificationMarksUsePurposeBuiltCanonicalVariants() {
        val project =
            generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
                .first { File(it, "app/src/main/AndroidManifest.xml").isFile }
        val manifest = File(project, "app/src/main/AndroidManifest.xml").readText()
        val notification = File(project, "app/src/main/res/drawable/palomar_notification_mask.xml").readText()
        val api26 = File(project, "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").readText()
        val api33 = File(project, "app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml").readText()
        val foreground = File(project, "app/src/main/res/drawable/palomar_launcher_foreground.xml").readText()
        val monochrome = File(project, "app/src/main/res/drawable/palomar_launcher_monochrome_mask.xml").readText()
        val fullColor = File(project, "app/src/main/res/drawable/palomar_mark.xml").readText()
        val darkMark = File(project, "app/src/main/res/drawable/palomar_mark_dark.xml").readText()
        val colors = File(project, "app/src/main/res/values/colors.xml").readText()
        val nightColors = File(project, "app/src/main/res/values-night/colors.xml").readText()

        assertTrue(manifest.contains("@mipmap/ic_launcher"))
        assertTrue(manifest.contains("@mipmap/ic_launcher_round"))
        assertFalse(api26.contains("<monochrome"))
        assertTrue(api33.contains("@drawable/palomar_launcher_monochrome_mask"))
        assertTrue(foreground.contains("android:translateX=\"22\""))
        assertTrue(foreground.contains("android:translateY=\"22\""))
        assertTrue(monochrome.contains("android:translateX=\"22\""))
        assertTrue(fullColor.contains("@color/palomar_lavender"))
        assertTrue(fullColor.contains("@color/palomar_cyan"))
        assertTrue(fullColor.contains("android:viewportWidth=\"108\""))
        assertTrue(fullColor.contains("android:translateX=\"22\""))
        assertTrue(fullColor.contains("android:translateY=\"22\""))
        assertTrue(darkMark.contains("android:viewportWidth=\"108\""))
        assertTrue(darkMark.contains("android:translateX=\"22\""))
        assertTrue(darkMark.contains("android:translateY=\"22\""))
        assertTrue(notification.contains("M 9.080,36.550"))
        assertTrue(colors.contains("<color name=\"app_background\">#F7F8FC</color>"))
        assertTrue(colors.contains("<color name=\"palomar_dark\">#171527</color>"))
        assertTrue(colors.contains("<color name=\"palomar_lavender\">#CFC1FD</color>"))
        assertTrue(colors.contains("<color name=\"palomar_cyan\">#62F9F8</color>"))
        assertTrue(colors.contains("<color name=\"palomar_platform_accent\">#006E73</color>"))
        assertTrue(nightColors.contains("<color name=\"app_background\">#090B16</color>"))
        assertTrue(nightColors.contains("<color name=\"palomar_platform_accent\">#62F9F8</color>"))
    }
}
