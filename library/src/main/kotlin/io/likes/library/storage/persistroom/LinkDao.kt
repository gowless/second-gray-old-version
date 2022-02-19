package io.likes.library.storage.persistroom

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.likes.library.storage.persistroom.model.Model


@Database(entities = [Model::class], version = 1, exportSchema = false)
abstract class LinkDao : RoomDatabase() {

    abstract fun linkDao(): ModelDao

    companion object {
        @Volatile
        private var INSTANCE: LinkDao? = null

        fun getDatabase(context: Context): LinkDao {

            val tempInstance = INSTANCE
            if(tempInstance != null){
                return tempInstance
            }
            synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LinkDao::class.java,
                    "link_database"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                return instance
            }
        }
    }

}