import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ProductDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "products.db"
        const val DATABASE_VERSION = 1
        const val TABLE_PRODUCTS = "products"
        const val COLUMN_PRODUCT_ID = "product_id"
        const val COLUMN_LIST_ID = "list_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_PRICE = "price"
        const val COLUMN_COUNT = "count"
        const val COLUMN_KDV = "kdv"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COLUMN_PRODUCT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LIST_ID INTEGER,
                $COLUMN_NAME TEXT,
                $COLUMN_PRICE REAL,
                $COLUMN_COUNT INTEGER,
                $COLUMN_KDV REAL
            )
        """
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    // Yeni ürün ekleme fonksiyonu
    fun insertProduct(listId: Int, name: String, price: Double, count: Int, kdv: Double) {
        val db = writableDatabase
        val insertQuery = """
            INSERT INTO $TABLE_PRODUCTS ($COLUMN_LIST_ID, $COLUMN_NAME, $COLUMN_PRICE, $COLUMN_COUNT, $COLUMN_KDV)
            VALUES ($listId, '$name', $price, $count, $kdv)
        """
        db.execSQL(insertQuery)
    }
}
