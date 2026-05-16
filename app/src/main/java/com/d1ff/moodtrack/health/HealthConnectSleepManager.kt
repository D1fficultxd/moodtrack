package com.d1ff.moodtrack.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private const val HEALTH_CONNECT_PROVIDER = "com.google.android.apps.healthdata"

enum class HealthConnectAvailability {
    Available,
    InstallOrUpdateRequired,
    Unavailable
}

sealed interface SleepImportResult {
    data class Success(
        val hours: Float,
        val minutes: Long,
        val usedSessionFallback: Boolean
    ) : SleepImportResult
    data object NoData : SleepImportResult
    data object NoPermission : SleepImportResult
    data object Unavailable : SleepImportResult
    data class Error(val throwable: Throwable) : SleepImportResult
}

class HealthConnectSleepManager(private val context: Context) {
    val sleepPermission: String = HealthPermission.getReadPermission(SleepSessionRecord::class)
    val permissions: Set<String> = setOf(sleepPermission)

    fun availability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.InstallOrUpdateRequired
            else -> HealthConnectAvailability.Unavailable
        }
    }

    fun installIntent(): Intent {
        val uriString = "market://details?id=$HEALTH_CONNECT_PROVIDER&url=healthconnect%3A%2F%2Fonboarding"
        return Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse(uriString)
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
        }
    }

    suspend fun hasSleepPermission(): Boolean = withContext(Dispatchers.IO) {
        if (availability() != HealthConnectAvailability.Available) {
            false
        } else {
            client().permissionController.getGrantedPermissions().contains(sleepPermission)
        }
    }

    suspend fun readSleepHours(date: LocalDate): SleepImportResult = withContext(Dispatchers.IO) {
        when {
            availability() != HealthConnectAvailability.Available -> SleepImportResult.Unavailable
            !hasSleepPermission() -> SleepImportResult.NoPermission
            else -> runCatching {
                val zone = ZoneId.systemDefault()
                val dayStart = date.atStartOfDay(zone).toInstant()
                val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

                // Query a wider window so sleep sessions crossing midnight are still included.
                val response = client().readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(
                            date.minusDays(1).atStartOfDay(zone).toInstant(),
                            date.plusDays(1).atStartOfDay(zone).toInstant()
                        )
                    )
                )

                var usedSessionFallback = false
                val totalMinutes = response.records.sumOf { record ->
                    val (minutes, usedFallback) = record.actualSleepMinutes(dayStart, dayEnd)
                    if (usedFallback) usedSessionFallback = true
                    minutes
                }

                if (totalMinutes <= 0L) {
                    SleepImportResult.NoData
                } else {
                    val roundedHours = ((totalMinutes / 60f) * 2f).roundToInt() / 2f
                    SleepImportResult.Success(
                        hours = roundedHours.coerceIn(0f, 16f),
                        minutes = totalMinutes,
                        usedSessionFallback = usedSessionFallback
                    )
                }
            }.getOrElse { SleepImportResult.Error(it) }
        }
    }

    private fun client(): HealthConnectClient =
        HealthConnectClient.getOrCreate(context, HEALTH_CONNECT_PROVIDER)

    private fun SleepSessionRecord.actualSleepMinutes(dayStart: Instant, dayEnd: Instant): Pair<Long, Boolean> {
        if (stages.isNotEmpty()) {
            val sleepMinutes = stages
                .filter { it.stage.isActualSleepStage() }
                .sumOf { stage -> overlapMinutes(stage.startTime, stage.endTime, dayStart, dayEnd) }
            return sleepMinutes to false
        }

        return overlapMinutes(startTime, endTime, dayStart, dayEnd) to true
    }

    private fun Int.isActualSleepStage(): Boolean {
        return this == SleepSessionRecord.STAGE_TYPE_SLEEPING ||
            this == SleepSessionRecord.STAGE_TYPE_LIGHT ||
            this == SleepSessionRecord.STAGE_TYPE_DEEP ||
            this == SleepSessionRecord.STAGE_TYPE_REM
    }

    private fun overlapMinutes(startTime: Instant, endTime: Instant, dayStart: Instant, dayEnd: Instant): Long {
        val start = maxInstant(startTime, dayStart)
        val end = minInstant(endTime, dayEnd)
        return if (end.isAfter(start)) Duration.between(start, end).toMinutes() else 0L
    }

    private fun maxInstant(a: Instant, b: Instant): Instant = if (a.isAfter(b)) a else b

    private fun minInstant(a: Instant, b: Instant): Instant = if (a.isBefore(b)) a else b
}
