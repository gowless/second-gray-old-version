package io.likes.library.storage.persistroom

import androidx.lifecycle.LiveData
import androidx.room.*
import io.likes.library.storage.persistroom.model.Model

@Dao
interface ModelDao {
    @Query("SELECT * FROM test")
    fun getAll(): LiveData<List<Model>>

    @Query("SELECT * FROM test")
    fun getAllData(): List<Model>

    @Update
    fun updateLink(model: Model)

    @Insert
    fun addLink(model: Model)

}