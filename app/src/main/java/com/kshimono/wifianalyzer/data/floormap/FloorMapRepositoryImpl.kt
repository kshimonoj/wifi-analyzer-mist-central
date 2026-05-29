package com.kshimono.wifianalyzer.data.floormap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import com.kshimono.wifianalyzer.data.aruba.ArubaApiClient
import com.kshimono.wifianalyzer.data.aruba.ArubaFloor
import com.kshimono.wifianalyzer.data.aruba.ArubaResult
import com.kshimono.wifianalyzer.data.db.dao.ApLocationDao
import com.kshimono.wifianalyzer.data.db.dao.FloorMapDao
import com.kshimono.wifianalyzer.data.db.dao.SnapshotDao
import com.kshimono.wifianalyzer.data.db.entities.ApLocationEntity
import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import com.kshimono.wifianalyzer.data.mist.MistApiClient
import com.kshimono.wifianalyzer.data.mist.MistMap
import com.kshimono.wifianalyzer.data.mist.MistResult
import com.kshimono.wifianalyzer.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FloorMapRepo"

@Singleton
class FloorMapRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val floorMapDao: FloorMapDao,
    private val apLocationDao: ApLocationDao,
    private val snapshotDao: SnapshotDao,
    private val arubaApiClient: ArubaApiClient,
    private val mistApiClient: MistApiClient,
    private val settingsRepository: SettingsRepository,
) : FloorMapRepository {

    private val mapDir = File(context.filesDir, "floormaps").also { it.mkdirs() }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun getAllMaps(): Flow<List<FloorMapEntity>> = floorMapDao.getAll()

    override suspend fun getById(id: Long): FloorMapEntity? = floorMapDao.getById(id)

    override suspend fun deleteMap(id: Long) {
        val entity = floorMapDao.getById(id)
        apLocationDao.deleteByFloorMapId(id)
        floorMapDao.delete(id)
        entity?.imageUri?.removePrefix("file://")?.let { path ->
            File(path).takeIf { it.exists() }?.delete()
        }
    }

    override suspend fun importLocalFile(uri: Uri, name: String): Result<Long> = runCatching {
        withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(uri) ?: "image/png"
            val timestamp = System.currentTimeMillis()

            val (bytes, widthPx, heightPx) = when {
                mimeType == "application/pdf" -> {
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: throw Exception("Cannot open PDF")
                    val renderer = PdfRenderer(pfd)
                    val page = renderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pfd.close()
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                    Triple(bos.toByteArray(), bitmap.width, bitmap.height)
                }
                else -> {
                    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw Exception("Cannot read file")
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(raw, 0, raw.size, opts)
                    Triple(raw, opts.outWidth, opts.outHeight)
                }
            }

            val ext = if (mimeType == "application/pdf") "png"
                      else mimeType.substringAfter("/").takeIf { it.isNotBlank() } ?: "png"
            val file = File(mapDir, "local_$timestamp.$ext")
            file.writeBytes(bytes)

            floorMapDao.insert(
                FloorMapEntity(
                    name    = name,
                    source  = "local",
                    siteId  = null,
                    floorId = null,
                    imageUri     = "file://${file.absolutePath}",
                    widthPx      = widthPx.coerceAtLeast(0),
                    heightPx     = heightPx.coerceAtLeast(0),
                    widthM       = null,
                    heightM      = null,
                    scalePixelsPerMeter = null,
                )
            )
        }
    }

    override suspend fun importFromMist(map: MistMap, token: String, region: String): Result<Long> = runCatching {
        withContext(Dispatchers.IO) {
            val url = map.url ?: throw Exception("Map has no image URL")
            val bytes = downloadWithAuth(url, "Token $token")

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

            val file = File(mapDir, "mist_${map.id}.png")
            file.writeBytes(bytes)

            val widthPx  = opts.outWidth.takeIf  { it > 0 } ?: (map.width  ?: 0)
            val heightPx = opts.outHeight.takeIf { it > 0 } ?: (map.height ?: 0)
            val scaleM = if (map.widthM != null && widthPx > 0)
                widthPx.toDouble() / map.widthM else null
            Log.d(TAG, "importFromMist: widthPx=$widthPx heightPx=$heightPx widthM=${map.widthM} heightM=${map.heightM}")

            floorMapDao.insert(
                FloorMapEntity(
                    name    = map.name,
                    source  = "mist",
                    siteId  = map.siteId,
                    floorId = map.id,
                    imageUri     = "file://${file.absolutePath}",
                    widthPx      = widthPx,
                    heightPx     = heightPx,
                    widthM       = map.widthM,
                    heightM      = map.heightM,
                    scalePixelsPerMeter = scaleM,
                )
            )
        }
    }

    override suspend fun importFromAruba(
        siteId: String,
        floor: ArubaFloor,
        buildingName: String,
    ): Result<Long> = runCatching {
        withContext(Dispatchers.IO) {
            val clientId     = settingsRepository.arubaClientId.first()
            val clientSecret = settingsRepository.arubaClientSecret.first()
            val cluster      = settingsRepository.arubaCluster.first()
            arubaApiClient.configure(clientId, clientSecret, cluster)

            runCatching { doArubaFloorImport(siteId, floor, buildingName) }
                .getOrElse { e ->
                    if (isAuthError(e.message)) {
                        Log.w(TAG, "Auth error during import, refreshing token and retrying")
                        arubaApiClient.forceRefreshToken()
                        doArubaFloorImport(siteId, floor, buildingName)
                    } else throw e
                }
        }
    }

    private fun isAuthError(msg: String?) =
        msg != null && (msg.contains("401") || msg.contains("Authentication", ignoreCase = true))

    private suspend fun doArubaFloorImport(siteId: String, floor: ArubaFloor, buildingName: String): Long {
        var lengthM: Double? = null
        var breadthM: Double? = null
        when (val detailResult = arubaApiClient.getFloorDetail(siteId, floor.floorId)) {
            is ArubaResult.Success -> {
                lengthM  = detailResult.data.properties?.length
                breadthM = detailResult.data.properties?.breadth
                Log.d(TAG, "Floor detail: lengthM=$lengthM breadthM=$breadthM")
            }
            is ArubaResult.Error -> {
                Log.w(TAG, "Could not fetch floor detail: ${detailResult.message}")
            }
        }

        return when (val result = arubaApiClient.downloadFloorImage(siteId, floor.floorId)) {
            is ArubaResult.Error   -> throw Exception(result.message)
            is ArubaResult.Success -> {
                val bytes = result.data
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

                val floorName = floor.properties.name
                    ?: "Floor ${floor.properties.ordinal ?: ""}"
                val name = "$buildingName - $floorName"
                val file = File(mapDir, "aruba_${floor.floorId}.png")
                file.writeBytes(bytes)

                floorMapDao.insert(
                    FloorMapEntity(
                        name    = name,
                        source  = "aruba",
                        siteId  = siteId,
                        floorId = floor.floorId,
                        imageUri = "file://${file.absolutePath}",
                        widthPx  = opts.outWidth.coerceAtLeast(0),
                        heightPx = opts.outHeight.coerceAtLeast(0),
                        widthM   = null,
                        heightM  = null,
                        scalePixelsPerMeter = null,
                        lengthM  = lengthM,
                        breadthM = breadthM,
                    )
                )
            }
        }
    }

    override suspend fun removeSnapshotFromMap(snapshotId: Long) {
        snapshotDao.removeMapPosition(snapshotId)
    }

    override suspend fun removeAllSnapshotsFromMap(floorMapId: Long) {
        snapshotDao.removeAllFromMap(floorMapId)
    }

    override suspend fun syncApLocations(floorMap: FloorMapEntity): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            when (floorMap.source) {
                "mist" -> syncMistAps(floorMap)
                "aruba" -> syncArubaAps(floorMap)
                else -> throw Exception("AP sync not supported for source '${floorMap.source}'")
            }
        }
    }

    private suspend fun syncMistAps(floorMap: FloorMapEntity): Int {
        val siteId = floorMap.siteId ?: throw Exception("Floor map has no siteId")
        val mapId  = floorMap.floorId ?: throw Exception("Floor map has no floorId")

        val token  = settingsRepository.mistToken.first()
        val region = settingsRepository.mistRegion.first()
        mistApiClient.configure(token, region)

        val apStats = when (val result = mistApiClient.getApStats(siteId)) {
            is MistResult.Success -> result.data
            is MistResult.Error   -> throw Exception(result.message)
        }

        val entities = apStats
            .filter { it.mapId == mapId && it.x != null && it.y != null }
            .map { ap ->
                val radios = ap.radioStat?.entries?.mapNotNull { (bandKey, stat) ->
                    val bandLabel = when (bandKey) {
                        "band_24" -> "2.4 GHz"
                        "band_5"  -> "5 GHz"
                        "band_6"  -> "6 GHz"
                        else      -> bandKey
                    }
                    if (stat.channel != null) {
                        com.kshimono.wifianalyzer.data.aruba.ArubaRadio(
                            band       = bandLabel,
                            channel    = "${stat.channel} (${stat.bandwidth} MHz)",
                            power      = "${stat.power} dBm",
                            macAddress = stat.mac,
                            number     = null,
                        )
                    } else null
                } ?: emptyList()

                val radiosJson = if (radios.isNotEmpty()) {
                    runCatching { json.encodeToString(radios) }.getOrNull()
                } else null

                val relX = (ap.x!! / floorMap.widthPx).toFloat().coerceIn(0f, 1f)
                val relY = (ap.y!! / floorMap.heightPx).toFloat().coerceIn(0f, 1f)
                Log.d(TAG, "syncAP: ${ap.name} x=${ap.x} y=${ap.y} relX=$relX relY=$relY")
                ApLocationEntity(
                    floorMapId = floorMap.id,
                    apName     = ap.name ?: "Unknown",
                    macAddress = ap.mac,
                    model      = ap.model,
                    source     = "mist",
                    mapX       = relX,
                    mapY       = relY,
                    status     = ap.status,
                    radiosJson = radiosJson,
                )
            }

        apLocationDao.deleteByFloorMapId(floorMap.id)
        apLocationDao.insertAll(entities)
        Log.d(TAG, "Synced ${entities.size} Mist APs for map ${floorMap.name}")
        return entities.size
    }

    private suspend fun syncArubaAps(floorMap: FloorMapEntity): Int {
        val siteId  = floorMap.siteId  ?: throw Exception("Floor map has no siteId")
        val floorId = floorMap.floorId ?: throw Exception("Floor map has no floorId")
        val lengthM  = floorMap.lengthM  ?: throw Exception("Floor dimensions not available. Re-import the floor map.")
        val breadthM = floorMap.breadthM ?: throw Exception("Floor dimensions not available. Re-import the floor map.")

        val clientId     = settingsRepository.arubaClientId.first()
        val clientSecret = settingsRepository.arubaClientSecret.first()
        val cluster      = settingsRepository.arubaCluster.first()
        arubaApiClient.configure(clientId, clientSecret, cluster)

        val devices = when (val result = arubaApiClient.getDeployedDevices(siteId, floorId)) {
            is ArubaResult.Success -> result.data
            is ArubaResult.Error   -> throw Exception(result.message)
        }

        val entities = devices
            .filter { it.deviceType == "ACCESSPOINT" && it.geometryRelative?.size == 2 }
            .map { device ->
                val radiosJson = device.accesspointProperties?.radios?.let { radios ->
                    runCatching { json.encodeToString(radios) }.getOrNull()
                }
                ApLocationEntity(
                    floorMapId = floorMap.id,
                    apName     = device.deviceName ?: "Unknown",
                    macAddress = device.accesspointProperties?.macAddress,
                    model      = device.accesspointProperties?.model,
                    source     = "aruba",
                    mapX       = (device.geometryRelative!![0] / lengthM).toFloat().coerceIn(0f, 1f),
                    mapY       = (device.geometryRelative[1] / breadthM).toFloat().coerceIn(0f, 1f),
                    status     = device.accesspointProperties?.status,
                    radiosJson = radiosJson,
                )
            }

        apLocationDao.deleteByFloorMapId(floorMap.id)
        apLocationDao.insertAll(entities)
        Log.d(TAG, "Synced ${entities.size} Aruba APs for map ${floorMap.name}")
        return entities.size
    }

    private fun downloadWithAuth(url: String, authHeader: String): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", authHeader)
            connectTimeout = 15_000
            readTimeout    = 60_000
        }
        return try {
            val status = conn.responseCode
            if (status != 200) throw Exception("HTTP $status downloading map image")
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}
