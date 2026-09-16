package net.kaltner.palomar

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.pressBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SessionCardContextActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actionsReflectLifecycleProviderAvailabilityAndOrganizerState() {
        val codex = session(capabilities = listOf("session.archive", "session.delete"))
        assertEquals(
            listOf(
                SessionContextAction.Pin,
                SessionContextAction.Hide,
                SessionContextAction.Archive,
                SessionContextAction.Delete,
            ),
            sessionContextActions(codex, false, false, emptySet(), providerUsable = true),
        )

        val archived =
            session(
                id = "archived",
                archived = true,
                readOnly = true,
                capabilities = listOf("session.read", "session.restore"),
            )
        assertEquals(
            listOf(SessionContextAction.Unpin, SessionContextAction.Show, SessionContextAction.Restore),
            sessionContextActions(archived, true, true, emptySet(), providerUsable = true),
        )

        listOf("working", "waiting", "stopping").forEach { status ->
            assertEquals(
                listOf(SessionContextAction.Pin, SessionContextAction.Hide),
                sessionContextActions(codex.copy(status = status), false, false, emptySet(), providerUsable = true),
            )
        }
        assertEquals(
            listOf(SessionContextAction.Pin, SessionContextAction.Hide),
            sessionContextActions(codex, false, false, emptySet(), providerUsable = false),
        )

        val claude =
            session(
                id = "claude",
                provider = PROVIDER_CLAUDE_CODE,
                capabilities = listOf("session.archive", "session.delete"),
            )
        assertEquals(
            listOf(SessionContextAction.Pin, SessionContextAction.Hide, SessionContextAction.Delete),
            sessionContextActions(claude, false, false, setOf("archive", "delete"), providerUsable = true),
        )
        assertFalse(
            sessionContextActions(
                claude.copy(capabilities = emptyList()),
                false,
                false,
                setOf("archive", "delete"),
                providerUsable = true,
            ).any { it.lifecycleAction() != null },
        )
    }

    @Test
    fun tapOpensWhileLongPressOnlyOpensActions() {
        val target = session(capabilities = listOf("session.archive", "session.delete"))
        var openCount = 0
        setSessionCard(target, onOpen = { openCount++ })

        composeRule.onNodeWithTag(sessionCardTestTag(target)).performTouchInput { click() }
        assertEquals(1, openCount)

        composeRule.onNodeWithTag(sessionCardTestTag(target)).performTouchInput { longClick() }
        assertEquals(1, openCount)
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Archive").assertIsDisplayed()
        composeRule.onNodeWithText("Delete permanently").assertIsDisplayed()
    }

    @Test
    fun nestedButtonsDoNotOpenTheCardActionSheet() {
        val target = session()
        var pinned = 0
        setSessionCard(target, onPin = { pinned++ })

        composeRule.onNodeWithContentDescription("Pin session").performTouchInput { longClick() }
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertDoesNotExist()
        assertEquals(1, pinned)

        composeRule.onNodeWithContentDescription("Pin session").performClick()
        assertEquals(2, pinned)
    }

    @Test
    fun organizerAndLifecycleSelectionsDispatchAndDismiss() {
        val target = session(capabilities = listOf("session.archive", "session.delete"))
        var pinned = 0
        var hidden = 0
        var lifecycle: SessionAction? = null
        composeRule.setContent {
            var isPinned by remember { mutableStateOf(false) }
            var isHidden by remember { mutableStateOf(false) }
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                SessionCard(
                    session = target,
                    matches = emptyList(),
                    pinned = isPinned,
                    hidden = isHidden,
                    repositoryLabel = "Repository",
                    onClick = {},
                    onAction = { lifecycle = it },
                    onPin = {
                        pinned++
                        isPinned = !isPinned
                    },
                    onHide = {
                        hidden++
                        isHidden = !isHidden
                    },
                    capabilities = emptySet(),
                    providerUsable = true,
                    showProviderIdentity = false,
                    hapticsEnabled = false,
                )
            }
        }

        openActions(target)
        composeRule.onNodeWithText("Pin session").performClick()
        assertEquals(1, pinned)
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertDoesNotExist()

        openActions(target)
        composeRule.onNodeWithText("Hide from session list").performClick()
        assertEquals(1, hidden)
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertDoesNotExist()

        openActions(target)
        composeRule.onNodeWithText("Archive").performClick()
        assertEquals(SessionAction.Archive, lifecycle)
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertDoesNotExist()

        openActions(target)
        composeRule.onNodeWithText("Unpin session").performClick()
        assertEquals(2, pinned)
        openActions(target)
        composeRule.onNodeWithText("Show in session list").performClick()
        assertEquals(2, hidden)
    }

    @Test
    fun lifecycleSelectionsReuseDestructiveConfirmationDialogs() {
        val active = session(capabilities = listOf("session.archive", "session.delete"))
        composeRule.setContent {
            var pending by remember { mutableStateOf<PendingSessionAction?>(null) }
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                SessionCard(
                    session = active,
                    matches = emptyList(),
                    pinned = false,
                    hidden = false,
                    repositoryLabel = "Repository",
                    onClick = {},
                    onAction = { pending = PendingSessionAction(active.id, active.title, it) },
                    onPin = {},
                    onHide = {},
                    capabilities = emptySet(),
                    providerUsable = true,
                    showProviderIdentity = false,
                    hapticsEnabled = false,
                )
                pending?.let {
                    SessionActionDialog(
                        pending = it,
                        busy = false,
                        onConfirm = {},
                        onDismiss = { pending = null },
                    )
                }
            }
        }

        openActions(active)
        composeRule.onNodeWithText("Delete permanently").performClick()
        composeRule.onNodeWithText("Delete session permanently?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete permanently").assertIsEnabled()
        composeRule.onNodeWithText("Cancel").performClick()

        openActions(active)
        composeRule.onNodeWithText("Archive").performClick()
        composeRule.onNodeWithText("Archive session?").assertIsDisplayed()
    }

    @Test
    fun archivedCardOffersRestoreConfirmationInsteadOfArchiveOrDelete() {
        val archived =
            session(
                id = "archived",
                archived = true,
                readOnly = true,
                capabilities = listOf("session.restore"),
            )
        composeRule.setContent {
            var pending by remember { mutableStateOf<PendingSessionAction?>(null) }
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                SessionCard(
                    session = archived,
                    matches = emptyList(),
                    pinned = false,
                    hidden = false,
                    repositoryLabel = null,
                    onClick = {},
                    onAction = { pending = PendingSessionAction(archived.id, archived.title, it) },
                    onPin = {},
                    onHide = {},
                    capabilities = setOf("archive", "delete"),
                    providerUsable = true,
                    showProviderIdentity = false,
                    hapticsEnabled = false,
                )
                pending?.let {
                    SessionActionDialog(it, busy = false, onConfirm = {}, onDismiss = { pending = null })
                }
            }
        }

        openActions(archived)
        composeRule.onNodeWithText("Restore").performClick()
        composeRule.onNodeWithText("Restore session?").assertIsDisplayed()
        composeRule.onNodeWithText("Archive").assertDoesNotExist()
    }

    @Test
    fun dashboardCardsExposeTheSameActionSurface() {
        val target = session(capabilities = listOf("session.archive"))
        composeRule.setContent {
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                DashboardSessionCard(
                    session = target,
                    repositoryLabel = "Repository",
                    detail = "Recently completed",
                    showProviderIdentity = false,
                    pinned = false,
                    hidden = false,
                    capabilities = emptySet(),
                    providerUsable = true,
                    hapticsEnabled = false,
                    onOpen = {},
                    onAction = {},
                    onPin = {},
                    onHide = {},
                )
            }
        }

        openActions(target)
        composeRule.onNodeWithText("Pin session").assertIsDisplayed()
        composeRule.onNodeWithText("Archive").assertIsDisplayed()
    }

    @Test
    fun talkBackActionOpensAndDismissSemanticsCloseTheSheet() {
        val target = session()
        setSessionCard(target)

        val actions =
            composeRule.onNodeWithTag(sessionCardTestTag(target)).fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        var exposed = false
        composeRule.runOnIdle {
            exposed = actions.single { it.label == "Session actions" }.action()
        }
        assertTrue(exposed)
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun transcriptMarkdownDoesNotAcquireSessionActions() {
        composeRule.setContent {
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                Column {
                    MarkdownText("Selectable transcript text")
                }
            }
        }

        composeRule.onNodeWithText("Selectable transcript text").performTouchInput { longClick() }
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertDoesNotExist()
    }

    private fun setSessionCard(
        session: SessionSummary,
        onOpen: () -> Unit = {},
        onPin: () -> Unit = {},
        onHide: () -> Unit = {},
        onAction: (SessionAction) -> Unit = {},
    ) {
        composeRule.setContent {
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                SessionCard(
                    session = session,
                    matches = emptyList(),
                    pinned = false,
                    hidden = false,
                    repositoryLabel = "Repository",
                    onClick = onOpen,
                    onAction = onAction,
                    onPin = onPin,
                    onHide = onHide,
                    capabilities = emptySet(),
                    providerUsable = true,
                    showProviderIdentity = false,
                    hapticsEnabled = false,
                )
            }
        }
    }

    private fun openActions(session: SessionSummary) {
        composeRule.onNodeWithTag(sessionCardTestTag(session)).performTouchInput { longClick() }
        composeRule.onNodeWithTag(SESSION_ACTIONS_SHEET_TEST_TAG).assertIsDisplayed()
    }

    private fun session(
        id: String = "session-1",
        status: String = "idle",
        provider: String = PROVIDER_CODEX,
        archived: Boolean = false,
        readOnly: Boolean = false,
        capabilities: List<String> = emptyList(),
    ): SessionSummary =
        SessionSummary(
            id = id,
            repository = "/repo",
            title = "Session $id",
            status = status,
            provider = provider,
            archived = archived,
            readOnly = readOnly,
            capabilities = capabilities,
        )
}
