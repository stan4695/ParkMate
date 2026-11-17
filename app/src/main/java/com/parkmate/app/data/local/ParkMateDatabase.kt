package com.parkmate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ParkingSpotEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ParkMateDatabase : RoomDatabase() {
    abstract fun parkingSpotDao(): ParkingSpotDao
}
