package org.techtown.samplerecorder.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.techtown.samplerecorder.database.RoomItem.Companion.ROOM_TABLE_NAME

@Entity(tableName = ROOM_TABLE_NAME)
class RoomItem(@ColumnInfo var title: String,
               @ColumnInfo var fileName: String,
               @ColumnInfo var time: String,
               @ColumnInfo var channel: String,
               @ColumnInfo var rate: Int) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    var no: Long? = null

    companion object {
        const val ROOM_TABLE_NAME = "room_items"
    }
}