package com.kshimono.wifianalyzer.ui.snapshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kshimono.wifianalyzer.data.location.LocationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val connectedGreen = Color(0xFF1D9E75)

@Composable
fun SaveSnapshotDialog(
    onSave:          (name: String, location: String, floor: String, note: String) -> Unit,
    onCancel:        () -> Unit,
    gpsLocation:     LocationResult? = null,
    gpsLoading:      Boolean         = false,
    gpsError:        String?         = null,
    onFetchGps:      () -> Unit      = {},
    connectedSsid:   String?         = null,
    connectedBssid:  String?         = null,
    connectedApName: String?         = null,
) {
    var name     by remember { mutableStateOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
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

                // GPS row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick  = onFetchGps,
                        enabled  = !gpsLoading,
                        modifier = Modifier,
                    ) {
                        if (gpsLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Get GPS")
                    }

                    Spacer(Modifier.width(8.dp))

                    // Connected AP info
                    if (connectedSsid != null) {
                        val apLabel = connectedApName ?: connectedBssid ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Wifi,
                                contentDescription = null,
                                tint     = connectedGreen,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text  = "Connected: $connectedSsid${if (apLabel.isNotBlank()) " ($apLabel)" else ""}",
                                color = connectedGreen,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    when {
                        gpsLocation != null -> {
                            val lat    = gpsLocation.latitude
                            val lon    = gpsLocation.longitude
                            val latDir = if (lat >= 0) "N" else "S"
                            val lonDir = if (lon >= 0) "E" else "W"
                            Text(
                                text  = "%.4f° %s, %.4f° %s  (±%.0fm)".format(
                                    abs(lat), latDir, abs(lon), lonDir, gpsLocation.accuracy
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        gpsError != null -> {
                            Text(
                                text  = gpsError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
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
