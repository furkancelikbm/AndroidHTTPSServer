package com.example.mobilserver.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.mobilserver.model.Product

class ProductRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // Ürünleri veritabanına aynı işlem ID'siyle eklemek için metod
    fun insertProducts(transactionId: String, products: List<Product>) {
        val db: SQLiteDatabase = dbHelper.writableDatabase

        products.forEach { product ->
            val contentValues = ContentValues().apply {
                put(DatabaseHelper.COLUMN_NAME, product.name)
                put(DatabaseHelper.COLUMN_PRICE, product.price)
                put(DatabaseHelper.COLUMN_KDV, product.kdv)
                put(DatabaseHelper.COLUMN_COUNT, product.count)
                put(DatabaseHelper.COLUMN_TRANSACTION_ID, transactionId) // İşlem ID'sini ekliyoruz
            }

            db.insert(DatabaseHelper.TABLE_PRODUCTS, null, contentValues)
        }
        db.close()
    }

//    @SuppressLint("Range")
//    fun getProductsByTransactionId(transactionId: String): List<Product> {
//        val db: SQLiteDatabase = dbHelper.readableDatabase
//        val productList = mutableListOf<Product>()
//
//        val cursor = db.query(
//            DatabaseHelper.TABLE_PRODUCTS,
//            null, // Tüm sütunları seçiyoruz
//            "${DatabaseHelper.COLUMN_TRANSACTION_ID} = ?",
//            arrayOf(transactionId), // Transaction ID'ye göre filtreleme yapıyoruz
//            null,
//            null,
//            null
//        )
//
//        with(cursor) {
//            while (moveToNext()) {
//                // ID'yi String olarak almak için toString() kullanıyoruz çünkü Product sınıfı String istiyor.
//                val id = getLong(getColumnIndex(DatabaseHelper.COLUMN_ID)).toString()
//
//                // Diğer alanları doğru türde alıyoruz:
//                val name = getString(getColumnIndex(DatabaseHelper.COLUMN_NAME)) ?: ""
//                val price = getDouble(getColumnIndex(DatabaseHelper.COLUMN_PRICE))
//                val kdv = getDouble(getColumnIndex(DatabaseHelper.COLUMN_KDV))
//                val count = getInt(getColumnIndex(DatabaseHelper.COLUMN_COUNT))
//
//                // Transaction ID'yi alıyoruz
//                val transactionId = getString(getColumnIndex(DatabaseHelper.COLUMN_TRANSACTION_ID)) ?: ""
//
//                // Ürünleri listeye ekliyoruz, id'yi Long olarak geçiriyoruz
//                productList.add(Product(id, name, price, kdv, count, transactionId))
//            }
//        }
//        cursor.close()
//        db.close()
//        return productList
//    }


}
