package com.example.a206730_caoyunhan_cikguizwan_project2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TreeOrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TreeDatabase : RoomDatabase() {

    abstract fun treeDao(): TreeDao

    companion object {

        @Volatile
        private var INSTANCE: TreeDatabase? = null

        fun getDatabase(context: Context): TreeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TreeDatabase::class.java,
                    "tree_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}