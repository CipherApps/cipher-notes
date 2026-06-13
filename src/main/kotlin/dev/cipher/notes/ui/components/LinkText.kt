package dev.cipher.notes.ui.components

import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun LinkText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val fontSize = MaterialTheme.typography.bodyMedium.fontSize.value

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                this.setTextColor(textColor)
                this.setLinkTextColor(linkColor)
                this.textSize = fontSize
                this.autoLinkMask = Linkify.WEB_URLS
                this.linksClickable = true
            }
        },
        update = { textView ->
            textView.text = text
            textView.maxLines = maxLines
            Linkify.addLinks(textView, Linkify.WEB_URLS)
        }
    )
}

