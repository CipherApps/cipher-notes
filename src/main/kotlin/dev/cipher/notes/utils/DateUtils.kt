package dev.cipher.notes.utils

import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.Date

object DateUtils {
    fun formatRelative(timeMillis: Long): String {
        return DateUtils.getRelativeTimeSpanString(
            timeMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    fun formatFull(timeMillis: Long): String {
        return DateFormat.format("MMM dd, yyyy HH:mm", Date(timeMillis)).toString()
    }
}
