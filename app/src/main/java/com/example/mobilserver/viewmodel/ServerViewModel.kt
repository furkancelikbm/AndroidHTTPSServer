package com.example.mobilserver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import com.example.mobilserver.model.Product
import com.example.mobilserver.repository.ServerRepository

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServerRepository(application.applicationContext)

    val productList: StateFlow<List<Product>> = repository.productList
    val receiptNumber: StateFlow<Int> = repository.receiptNumber

    init {
        repository.startServer()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopServer()
    }
}
