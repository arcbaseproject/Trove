package com.trove.app.ui.components

import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.trove.app.R

/**
 * API 30+ shows the system delete dialog. Older versions delete straight away, so this asks first
 * there. Returns `guard(count) { delete() }`.
 */
@Composable
fun rememberDeleteGuard(): (count: Int, onConfirm: () -> Unit) -> Unit {
    var pending by remember { mutableStateOf<Pair<Int, () -> Unit>?>(null) }

    pending?.let { (count, onConfirm) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(pluralStringResource(R.plurals.delete_confirm_title, count, count)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    onConfirm()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    return { count, onConfirm ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            onConfirm()
        } else {
            pending = count to onConfirm
        }
    }
}
