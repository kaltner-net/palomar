package net.kaltner.palomar

internal const val PALOMAR_REPOSITORY_URL = "https://github.com/kaltner-net/palomar"
internal const val PALOMAR_RELEASES_URL = "$PALOMAR_REPOSITORY_URL/releases"
internal const val PALOMAR_LICENSE_URL = "$PALOMAR_REPOSITORY_URL/blob/main/LICENSE"
internal const val PALOMAR_THIRD_PARTY_NOTICES_URL =
    "$PALOMAR_REPOSITORY_URL/blob/main/THIRD_PARTY_NOTICES.md"
internal const val KALTNER_WEBSITE_URL = "https://kaltner.net"

internal data class AboutVersionInformation(
    val server: String,
    val client: String,
)

internal fun clientBuildDescription(version: String, commit: String, releaseBuild: Boolean): String {
    val identity = if (releaseBuild) version else "$version (development build)"
    return if (commit.isNotBlank() && commit != "unknown") "$identity · $commit" else identity
}

internal fun aboutVersionInformation(
    serverVersion: String?,
    connected: Boolean,
    clientVersion: String,
    clientCommit: String,
    releaseBuild: Boolean,
): AboutVersionInformation =
    AboutVersionInformation(
        server = if (connected) {
            serverVersion ?: "Unavailable"
        } else {
            serverVersion?.let { "$it (last connected)" } ?: "Unavailable while disconnected"
        },
        client = clientBuildDescription(clientVersion, clientCommit, releaseBuild),
    )

internal val palomarAboutLinks =
    listOf(
        "GitHub repository" to PALOMAR_REPOSITORY_URL,
        "Current releases" to PALOMAR_RELEASES_URL,
        "License" to PALOMAR_LICENSE_URL,
        "Third-party notices" to PALOMAR_THIRD_PARTY_NOTICES_URL,
    )
