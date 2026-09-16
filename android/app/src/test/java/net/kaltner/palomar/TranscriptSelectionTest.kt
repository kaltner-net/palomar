package net.kaltner.palomar

import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TranscriptSelectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renderedMessagesHaveSeparateSelectionScopesAcrossMarkdownBlocks() {
        composeRule.setContent {
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                Column {
                    SelectableTranscriptMessage(identity(itemId = "user")) {
                        MarkdownText("User paragraph with `inline_code` and /workspace/path")
                    }
                    SelectableTranscriptMessage(identity(itemId = "assistant")) {
                        MarkdownText(
                            """
                            # Heading

                            - Bulleted item
                            1. Numbered item
                            > Quoted text

                            | Name | Value |
                            | --- | --- |
                            | identifier | result |

                            ```kotlin
                            val answer = 42
                            ```
                            """.trimIndent(),
                        )
                    }
                }
            }
        }

        composeRule.onAllNodesWithTag(TRANSCRIPT_SELECTION_CONTAINER_TEST_TAG).assertCountEquals(2)
        composeRule.onNodeWithText("User paragraph with inline_code and /workspace/path").assertExists()
        composeRule.onNodeWithText("Heading").assertExists()
        composeRule.onNodeWithText("Bulleted item").assertExists()
        composeRule.onNodeWithText("Numbered item").assertExists()
        composeRule.onNodeWithText("Quoted text").assertExists()
        composeRule.onNodeWithText("identifier").assertExists()
        composeRule.onNodeWithText("val answer = 42").assertExists()
    }

    @Test
    fun selectionScopeIsDisposedWhenItsHostProviderSessionOrItemChanges() {
        var updateIdentity: (TranscriptSelectionIdentity) -> Unit = {}
        var disposals = 0
        val initial = identity()
        composeRule.setContent {
            var currentIdentity by remember { mutableStateOf(initial) }
            updateIdentity = { currentIdentity = it }
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                SelectableTranscriptMessage(currentIdentity) {
                    DisposableEffect(Unit) {
                        onDispose { disposals++ }
                    }
                    Text(currentIdentity.itemId)
                }
            }
        }

        composeRule.runOnIdle { updateIdentity(initial) }
        composeRule.runOnIdle { assertEquals(0, disposals) }

        listOf(
            initial.copy(hostId = "host-2"),
            initial.copy(hostId = "host-2", provider = PROVIDER_CLAUDE_CODE),
            initial.copy(hostId = "host-2", provider = PROVIDER_CLAUDE_CODE, sessionId = "session-2"),
            initial.copy(hostId = "host-2", provider = PROVIDER_CLAUDE_CODE, sessionId = "session-2", itemId = "item-2"),
        ).forEachIndexed { index, changed ->
            composeRule.runOnIdle { updateIdentity(changed) }
            composeRule.runOnIdle { assertEquals(index + 1, disposals) }
        }
    }

    @Test
    fun interactiveMarkdownChromeHasNonSelectableBoundaries() {
        composeRule.setContent {
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                SelectableTranscriptMessage(identity()) {
                    MarkdownText(
                        """
                        - [ ] Device verification

                        ```text
                        selectable code
                        ```

                        ::git-create-pr{url="https://example.com/pull/121" branch="feat/121"}
                        """.trimIndent(),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(TRANSCRIPT_TASK_CONTROL_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(TRANSCRIPT_CODE_HEADER_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(TRANSCRIPT_DIRECTIVE_TEST_TAG).assertExists()
        composeRule.onNodeWithText("Device verification").assertExists()
        composeRule.onNodeWithText("selectable code").assertExists()
    }

    @Test
    fun codeBlockWholeCopyStillCopiesTheExactPayload() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val clipboard = checkNotNull(context.getSystemService(ClipboardManager::class.java))
        clipboard.clearPrimaryClip()
        val payload = "first line\n  indented line\nlast line"
        composeRule.setContent {
            PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                SelectableTranscriptMessage(identity()) {
                    MarkdownText("```text\n$payload\n```")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Copy").performClick()
        composeRule.waitForIdle()

        assertEquals(payload, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun linkLabelsRemainClickableInsideSelectionScope() {
        var opened: String? = null
        composeRule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            opened = uri
                        }
                    },
            ) {
                PalomarTheme(ThemeId.Palomar, darkTheme = false) {
                    SelectableTranscriptMessage(identity()) {
                        MarkdownText("[Open documentation](https://example.com/docs)")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Open documentation").performClick()
        composeRule.runOnIdle { assertEquals("https://example.com/docs", opened) }
    }

    private fun identity(
        hostId: String = "host-1",
        provider: String = PROVIDER_CODEX,
        sessionId: String = "session-1",
        itemId: String = "item-1",
    ) = TranscriptSelectionIdentity(hostId, provider, sessionId, itemId)
}
