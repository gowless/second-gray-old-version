package io.likes.library

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import io.likes.library.managers.MainManager
import io.likes.library.storage.Repository
import io.likes.library.storage.persistroom.LinkDao
import kotlinx.coroutines.InternalCoroutinesApi

object FormBuilder {

    var repository: Repository? = null



    @OptIn(InternalCoroutinesApi::class)
    fun createAppsInstance(context: AppCompatActivity): MainManager {
       return MainManager(context)
    }


    fun createRepoInstance(context: Context): Repository {
        if (repository == null){
            return Repository(LinkDao.getDatabase(context).linkDao())
        } else {
            return repository as Repository
        }
    }

}