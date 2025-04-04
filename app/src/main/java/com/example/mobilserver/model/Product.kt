package com.example.mobilserver.model

import kotlinx.serialization.Serializable


@Serializable
data class Product(
    val price: Double,
    val name: String,
    val count: Int,
    val kdv: Double
)
