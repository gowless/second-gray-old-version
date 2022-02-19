package io.likes.library.storage

import androidx.lifecycle.LiveData
import io.likes.library.storage.persistroom.ModelDao
import io.likes.library.storage.persistroom.model.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Repository(var modelDao: ModelDao) {

    val readAllData: LiveData<List<Model>> = modelDao.getAll()


    fun getAllData(): List<Model>{
        return modelDao.getAllData()
    }

    fun insert(model: Model){
        GlobalScope.launch(Dispatchers.IO){ modelDao.addLink(model) }
    }

    fun updateLink(model: Model){
        GlobalScope.launch(Dispatchers.IO) { modelDao.updateLink(model)  }
    }
}