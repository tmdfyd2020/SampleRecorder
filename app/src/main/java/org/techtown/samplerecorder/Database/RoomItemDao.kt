package org.techtown.samplerecorder.Database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface RoomItemDao {
    @Query("select * from room_items")
    fun getList(): List<RoomItem>

    @Insert(onConflict = REPLACE)
    fun insert(item: RoomItem)

    @Delete
    fun delete(item: RoomItem)
}