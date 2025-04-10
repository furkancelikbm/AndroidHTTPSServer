package com.example.mobilserver.model

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "app_database"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                price REAL,
                count INTEGER,
                kdv REAL,
                list_id INTEGER,
                created_at TEXT  -- Tarih ve saat bilgisi için sütun
            );
        """
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS products")
        onCreate(db)
    }

    // Ürünleri ve tarih bilgisini veritabanına eklemek
    fun insertProducts(products: List<Product>, listId: Int) {
        val db = writableDatabase
        val currentDateTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        db.beginTransaction()
        try {
            for (product in products) {
                val values = ContentValues().apply {
                    put("name", product.name)
                    put("price", product.price)
                    put("count", product.count)
                    put("kdv", product.kdv)
                    put("list_id", listId)
                    put("created_at", currentDateTime)  // Tarih ve saat bilgisini ekle
                }
                db.insert("products", null, values)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }


}

