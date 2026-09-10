package org.bhashasetu.fln.edge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NipunMilestone::class,
        ExtractiveResource::class,
        LessonScript::class,
        Worksheet::class,
        Flashcard::class,
        PhrasebookEntry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NipunDatabase : RoomDatabase() {
    abstract fun nipunDao(): NipunDao

    companion object {
        @Volatile
        private var INSTANCE: NipunDatabase? = null

        fun getDatabase(context: Context): NipunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NipunDatabase::class.java,
                    "nipun_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
