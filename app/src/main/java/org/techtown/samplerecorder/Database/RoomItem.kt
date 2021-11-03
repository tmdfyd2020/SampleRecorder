package org.techtown.samplerecorder.Database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "room_items")
class RoomItem(@ColumnInfo var title: String,
               @ColumnInfo var fileName: String,
               @ColumnInfo var time: String,
               @ColumnInfo var channel: String,
               @ColumnInfo var rate: Int) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    var no: Long? = null
}