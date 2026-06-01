package com.ant.tunes.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ant.tunes.ui.theme.AntSurface1
import com.ant.tunes.ui.theme.AntText
import com.ant.tunes.ui.theme.AntText2
import com.ant.tunes.ui.theme.AntText3

@Composable
fun UpdateDialog(onDismiss: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AntSurface1,
        title = { Text("Update Available", color = AntText) },
        text = { Text("A new version of Ant Tunes is ready. Update now to get the latest features.", color = AntText2) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                uriHandler.openUri("https://github.com/Bat-Noir/Ant-tunes/releases/latest")
            }) {
                Text("UPDATE", color = Color(0xFF10B981))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("CANCEL", color = AntText3)
            }
        }
    )
}
