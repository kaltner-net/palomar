package net.kaltner.palomar

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

internal const val TRANSCRIPT_SELECTION_CONTAINER_TEST_TAG = "transcript-selection-container"
internal const val TRANSCRIPT_TASK_CONTROL_TEST_TAG = "transcript-non-selectable-task-control"
internal const val TRANSCRIPT_CODE_HEADER_TEST_TAG = "transcript-non-selectable-code-header"
internal const val TRANSCRIPT_DIRECTIVE_TEST_TAG = "transcript-non-selectable-directive"

internal data class TranscriptSelectionContext(
    val hostId: String?,
    val provider: String,
    val sessionId: String,
) {
    fun forItem(itemId: String): TranscriptSelectionIdentity =
        TranscriptSelectionIdentity(hostId, provider, sessionId, itemId)
}

internal data class TranscriptSelectionIdentity(
    val hostId: String?,
    val provider: String,
    val sessionId: String,
    val itemId: String,
)

/** Owns native selection for one message and disposes it whenever its transcript identity changes. */
@Composable
internal fun SelectableTranscriptMessage(
    identity: TranscriptSelectionIdentity,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    key(identity) {
        SelectionContainer(
            modifier = modifier.testTag(TRANSCRIPT_SELECTION_CONTAINER_TEST_TAG),
            content = content,
        )
    }
}
