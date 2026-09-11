package net.kaltner.palomar

import android.content.ClipboardManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarkdownCodeCopyTest {
    @Test
    fun copiesTheCompleteExactCodeBlockPayload() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val clipboard = checkNotNull(context.getSystemService(ClipboardManager::class.java))
        val payload = "first line\n  indented line\nlast line\n"

        copyCodeBlockToClipboard(context, payload)

        assertEquals(payload, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }
}
