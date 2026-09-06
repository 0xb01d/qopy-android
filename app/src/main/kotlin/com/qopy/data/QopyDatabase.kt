package com.qopy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PeerEntity::class], version = 1, exportSchema = false)
abstract class QopyDatabase : RoomDatabase() {
    abstract fun peerDao(): PeerDao

    companion object {
        @Volatile
        private var instance: QopyDatabase? = null

        fun getInstance(context: Context): QopyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QopyDatabase::class.java,
                    "qopy.db"
                ).build().also { instance = it }
            }
        }
    }
}
