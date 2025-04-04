package com.example.mobilserver

import kotlinx.serialization.Serializable


@Serializable
data class Product(
    val price: Double,
    val name: String,
    val count: Int,
    val kdv: Double
)
