package dev.cipher.notes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cipher.notes.data.Note
import dev.cipher.notes.data.NoteType
import dev.cipher.notes.utils.DateUtils
import dev.cipher.notes.utils.JsonUtils
import androidx.compose.foundation.text.BasicTextField
import dev.cipher.notes.data.TodoItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.SolidColor

@Composable
fun buildLinkifiedString(text: String): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        append(text)
        val urlPattern = "(?:https?://|www\\.)[a-zA-Z0-9.\\-_~%:/?#\\[\\]@!$&'()*+,;=]+".toRegex()

        runCatching {
            urlPattern.findAll(text).forEach { match ->
                val url = match.value
                val fullUrl = if (url.startsWith("www.")) "http://$url" else url

                addLink(
                    LinkAnnotation.Url(
                        url = fullUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: TodoItem,
    onDoneChanged: (Boolean) -> Unit,
    onTextChanged: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.done,
            onCheckedChange = { onDoneChanged(it) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (isEditing) {
            BasicTextField(
                value = item.text,
                onValueChange = { onTextChanged(it) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (item.done) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { isEditing = false }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Save item",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = buildLinkifiedString(item.text),
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (item.done) TextDecoration.LineThrough else null
                ),
                color = if (item.done) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { isEditing = true }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = "Edit item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                contentDescription = "Delete item",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (note.encrypted) "🔒" else if (note.type == NoteType.TODO) "☑️" else "📝",
                    fontSize = 12.sp
                )
            }

            if (note.encrypted) {
                Text(
                    text = "••••••••••••••••••••",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else if (note.type == NoteType.TODO) {
                val items = JsonUtils.jsonToTodoItems(note.itemsJson)
                if (items.isEmpty()) {
                    Text(
                        "Empty checklist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items.take(3).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (item.done) "☑ " else "☐ ",
                                    fontSize = 10.sp,
                                    color = if (item.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = buildLinkifiedString(item.text),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (item.done) TextDecoration.LineThrough else null
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (items.size > 3) {
                            Text(
                                "+ ${items.size - 3} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = buildLinkifiedString(note.content.ifBlank { "Empty note" }),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = DateUtils.formatRelative(note.modifiedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}