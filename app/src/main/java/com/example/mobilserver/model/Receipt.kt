package com.example.mobilserver.model

// Yeni bir Receipt sınıfı oluşturuyoruz.
data class Receipt(
    val id: String,
    val products: List<Product>
)
