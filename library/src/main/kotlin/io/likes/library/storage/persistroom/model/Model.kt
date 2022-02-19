package io.likes.library.storage.persistroom.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test")
data class Model(
    @PrimaryKey var uid: Int,
    @ColumnInfo(name = "link")
    var link: String?
)