package com.jrlabapps.coffeegrams.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewMethod
import java.time.Instant
import java.util.UUID

/**
 * The Room persistence model for a saved brew — mirrors the iOS app's
 * SwiftData `BrewLogRecord`'s 11 columns exactly. Lives in `:app`, not
 * `:core`: the domain uses the plain value type [BrewLogEntry], and the
 * mapping to/from this entity happens here, keeping Room out of the pure
 * core the same way SwiftData is kept out of `CoffeeGramsCore`.
 *
 * [methodRawValue] stores the brew method's stable raw string (not the
 * enum) — see [BrewMethod]'s persistence-contract note — so the on-disk
 * format is independent of the Kotlin type and survives a case being
 * renamed or reordered.
 */
@Entity(tableName = "brew_log")
data class BrewLogEntity(
    @PrimaryKey val id: UUID,
    val date: Instant,
    val methodRawValue: String,
    val doseGrams: Double,
    /** For espresso this holds the shot yield (mirrors [BrewLogEntry]). */
    val waterGrams: Double,
    val ratio: Double,
    val shotSeconds: Int?,
    val plannedSeconds: Int?,
    val actualSeconds: Int?,
    val rating: Int?,
    val notes: String?,
)

fun BrewLogEntry.toEntity(): BrewLogEntity = BrewLogEntity(
    id = id,
    date = date,
    methodRawValue = method.rawValue,
    doseGrams = doseGrams,
    waterGrams = waterGrams,
    ratio = ratio,
    shotSeconds = shotSeconds,
    plannedSeconds = plannedSeconds,
    actualSeconds = actualSeconds,
    rating = rating,
    notes = notes,
)

fun BrewLogEntity.toEntry(): BrewLogEntry = BrewLogEntry(
    id = id,
    date = date,
    method = BrewMethod.fromRawValue(methodRawValue),
    doseGrams = doseGrams,
    waterGrams = waterGrams,
    ratio = ratio,
    shotSeconds = shotSeconds,
    plannedSeconds = plannedSeconds,
    actualSeconds = actualSeconds,
    rating = rating,
    notes = notes,
)

/** Room has no native column type for [UUID] or [Instant]; convert to primitives it does. */
object BrewLogConverters {
    @TypeConverter
    fun uuidToString(id: UUID): String = id.toString()

    @TypeConverter
    fun stringToUuid(value: String): UUID = UUID.fromString(value)

    @TypeConverter
    fun instantToEpochMillis(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(epochMillis: Long): Instant = Instant.ofEpochMilli(epochMillis)
}
