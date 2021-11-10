package org.techtown.samplerecorder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import org.techtown.samplerecorder.database.RoomItem.Companion.ROOM_TABLE_NAME

@Dao
interface RoomItemDao {
    @Query("select * from $ROOM_TABLE_NAME")
    fun getList(): List<RoomItem>

    @Insert(onConflict = REPLACE)
    fun insert(item: RoomItem)

    @Delete
    fun delete(item: RoomItem)
}