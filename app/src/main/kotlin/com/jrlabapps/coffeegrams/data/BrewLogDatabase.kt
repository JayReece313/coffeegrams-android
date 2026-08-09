package com.jrlabapps.coffeegrams.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * One table, no migration history to honor yet — schema export is still
 * enabled (`exportSchema = true`, wired to `room.schemaLocation` in
 * `app/build.gradle.kts`) so the first migration, whenever it lands, has a
 * committed baseline to diff against instead of reconstructing v1 from
 * memory.
 */
@Database(entities = [BrewLogEntity::class], version = 1, exportSchema = true)
@TypeConverters(BrewLogConverters::class)
abstract class BrewLogDatabase : RoomDatabase() {
    abstract fun brewLogDao(): BrewLogDao

    companion object {
        private const val DATABASE_NAME = "coffeegrams.db"

        fun build(context: Context): BrewLogDatabase =
            Room.databaseBuilder(context.applicationContext, BrewLogDatabase::class.java, DATABASE_NAME).build()
    }
}
