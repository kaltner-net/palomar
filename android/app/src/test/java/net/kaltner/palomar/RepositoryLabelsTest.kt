package net.kaltner.palomar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepositoryLabelsTest {
    private fun repository(path: String) = RepositoryInfo(
        id = path,
        name = path.substringAfterLast('/'),
        path = path,
        branch = "main",
        dirty = false,
    )

    private fun labels(vararg paths: String): Map<String, String> =
        resolveRepositorySet(paths.map(::repository), "/projects")
            .associate { it.info.path to it.label }

    @Test
    fun uniqueBasenamesStayConcise() {
        assertEquals(
            mapOf("palomar" to "palomar", "tools/android" to "android"),
            labels("palomar", "tools/android"),
        )
    }

    @Test
    fun duplicateBasenamesUseTheShortestDistinctParentSuffix() {
        assertEquals(
            mapOf("mobile/android" to "mobile/android", "accounts/android" to "accounts/android"),
            labels("mobile/android", "accounts/android"),
        )
    }

    @Test
    fun rootLevelRepositoryStaysConciseWhenNestedRepositoryCollides() {
        assertEquals(
            mapOf("android" to "android", "accounts-admin/android" to "accounts-admin/android"),
            labels("android", "accounts-admin/android"),
        )
    }

    @Test
    fun deeperSuffixCollisionsExpandUntilEveryLabelIsDistinct() {
        assertEquals(
            mapOf(
                "clients/mobile/android" to "clients/mobile/android",
                "archive/mobile/android" to "archive/mobile/android",
            ),
            labels("clients/mobile/android", "archive/mobile/android"),
        )
    }

    @Test
    fun discoveryOrderDoesNotAffectLabels() {
        val paths = arrayOf("android", "clients/mobile/android", "archive/mobile/android")
        assertEquals(labels(*paths), labels(*paths.reversedArray()))
    }

    @Test
    fun removingCollisionReturnsRemainingRepositoryToConciseLabel() {
        assertEquals("mobile/android", labels("mobile/android", "accounts/android")["mobile/android"])
        assertEquals("android", labels("mobile/android")["mobile/android"])
    }

    @Test
    fun canonicalGroupsFiltersCollapsedStateAndCardContextStaySeparate() {
        val repositories = listOf(repository("android"), repository("accounts-admin/android"))
        val sessions = listOf(
            SessionSummary("root", "/projects/android", "Root", "idle"),
            SessionSummary("admin", "/projects/accounts-admin/android/src", "Admin", "idle"),
        )
        val visible = filterSessions(
            sessions,
            SessionSearchFilters(),
            emptySet(),
            emptySet(),
            emptyList(),
            repositories,
            "/projects",
        )

        assertEquals(
            listOf(
                SessionRepositoryOption("/projects/accounts-admin/android", "Repository: accounts-admin/android"),
                SessionRepositoryOption("/projects/android", "Repository: android"),
            ),
            sessionRepositoryOptions(sessions, repositories, "/projects"),
        )
        assertEquals(
            listOf("admin", "root"),
            repositorySessionGroups(visible, repositories, "/projects")
                .map { it.sessions.single().session.id },
        )
        assertEquals(
            listOf("root"),
            filterSessions(
                sessions,
                SessionSearchFilters(repository = "/projects/android"),
                emptySet(),
                emptySet(),
                emptyList(),
                repositories,
                "/projects",
            ).map { it.session.id },
        )

        val collapsed = toggleCollapsedRepository(emptyMap(), "home", "/projects/accounts-admin/android")
        assertEquals(setOf("/projects/accounts-admin/android"), collapsed["home"])
        assertNull(
            sessionCardRepositoryLabel(
                sessions[1],
                repositories,
                "/projects",
                SessionCardRenderContext(true, "/projects/accounts-admin/android"),
            ),
        )
        assertEquals(
            "Repository: accounts-admin/android",
            sessionCardRepositoryLabel(
                sessions[1],
                repositories,
                "/projects",
                SessionCardRenderContext(),
            ),
        )
        assertEquals(
            "Collapse Repository: accounts-admin/android (1 sessions)",
            repositoryGroupContentDescription(
                SessionRepositoryOption(
                    "/projects/accounts-admin/android",
                    "Repository: accounts-admin/android",
                ),
                1,
                false,
            ),
        )
    }

    @Test
    fun workspaceAndUnknownRepositoryFallbacksStayUnchanged() {
        val repositories = listOf(repository("palomar"))
        assertEquals(
            SessionRepositoryOption("/home/operator", "Workspace: /home/operator"),
            sessionRepositoryIdentity("/home/operator", repositories, "/projects"),
        )
        assertEquals(
            SessionRepositoryOption("/", "Workspace: /"),
            sessionRepositoryIdentity("", repositories, "/projects"),
        )
    }
}
