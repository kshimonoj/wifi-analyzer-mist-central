package com.kshimono.wifianalyzer.ui.snapshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaveSnapshotDialog(
    onSave:   (name: String, location: String, floor: String, note: String) -> Unit,
    onCancel: () -> Unit,
) {
    var name     by remember { mutableStateOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    ) }
    var location by remember { mutableStateOf("") }
    var floor    by remember { mutableStateOf("") }
    var note     by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title   = { Text("Save Snapshot") },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Snapshot name *") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    isError       = name.isBlank(),
                    supportingText = if (name.isBlank()) {
                        { Text("Required") }
                    } else null,
                )
                OutlinedTextField(
                    value         = location,
                    onValueChange = { location = it },
                    label         = { Text("Location") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = floor,
                    onValueChange = { floor = it },
                    label         = { Text("Floor") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("Note") },
                    minLines      = 2,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank()) onSave(name, location, floor, note) },
                enabled  = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}
