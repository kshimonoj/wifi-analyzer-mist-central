package com.kshimono.wifianalyzer.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.kshimono.wifianalyzer.data.db.dao.SnapshotDao
import com.kshimono.wifianalyzer.data.db.entities.ApLocationEntity
import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import com.kshimono.wifianalyzer.ui.map.SnapshotWithPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportSurveyUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val snapshotDao: SnapshotDao,
    private val exportCsvUseCase: ExportCsvUseCase,
) {
    private val dtf = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    suspend fun execute(
        floorMap: FloorMapEntity,
        apLocations: List<ApLocationEntity>,
        placedSnapshots: List<SnapshotWithPosition>,
    ): Result<Uri> = runCatching {
        withContext(Dispatchers.IO) {
            val outDir = File(context.cacheDir, "survey_export").also { it.mkdirs() }
            val timestamp = System.currentTimeMillis()
            val zipFile = File(outDir, "survey_$timestamp.zip")

            ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->

                // floor_map.png
                val imageFile = File(floorMap.imageUri.removePrefix("file://"))
                if (imageFile.exists()) {
                    zip.putNextEntry(ZipEntry("floor_map.png"))
                    imageFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

                // ap_locations.csv
                if (apLocations.isNotEmpty()) {
                    zip.putNextEntry(ZipEntry("ap_locations.csv"))
                    val header = "ap_name,mac_address,model,source,map_x,map_y,status,last_synced\n"
                    zip.write(header.toByteArray())
                    apLocations.forEach { ap ->
                        val row = listOf(
                            ap.apName,
                            ap.macAddress ?: "",
                            ap.model ?: "",
                            ap.source,
                            "%.6f".format(ap.mapX),
                            "%.6f".format(ap.mapY),
                            ap.status ?: "",
                            dtf.format(Instant.ofEpochMilli(ap.lastSynced)),
                        ).joinToString(",") { escapeCsv(it) } + "\n"
                        zip.write(row.toByteArray())
                    }
                    zip.closeEntry()
                }

                // snapshots.csv
                if (placedSnapshots.isNotEmpty()) {
                    zip.putNextEntry(ZipEntry("snapshots.csv"))
                    val csvBuilder = StringBuilder()
                    placedSnapshots.forEachIndexed { idx, sp ->
                        val observations = snapshotDao.getObservationsBySnapshotId(sp.snapshot.id)
                        val csv = exportCsvUseCase.execute(sp.snapshot, observations, floorMap)
                        if (idx == 0) {
                            csvBuilder.append(csv)
                        } else {
                            val lines = csv.lines()
                            if (lines.size > 1) csvBuilder.append(lines.drop(1).joinToString("\n"))
                        }
                    }
                    zip.write(csvBuilder.toString().toByteArray())
                    zip.closeEntry()
                }

                // summary.json
                zip.putNextEntry(ZipEntry("summary.json"))
                val json = buildString {
                    appendLine("{")
                    appendLine("  \"map_name\": ${jsonString(floorMap.name)},")
                    appendLine("  \"source\": ${jsonString(floorMap.source)},")
                    appendLine("  \"exported_at\": ${jsonString(dtf.format(Instant.ofEpochMilli(timestamp)))},")
                    appendLine("  \"ap_count\": ${apLocations.size},")
                    append("  \"snapshot_count\": ${placedSnapshots.size}")
                    appendLine()
                    append("}")
                }
                zip.write(json.toByteArray())
                zip.closeEntry()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile,
            )
        }
    }

    private fun escapeCsv(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n'))
            "\"${value.replace("\"", "\"\"")}\""
        else value

    private fun jsonString(value: String) =
        "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
