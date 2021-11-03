package org.techtown.samplerecorder.Database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RoomItem::class], version = 2, exportSchema = false)
abstract class RoomHelper : RoomDatabase() {
    abstract fun roomItemDao(): RoomItemDao
}