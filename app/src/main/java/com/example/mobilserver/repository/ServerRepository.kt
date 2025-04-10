package com.example.mobilserver.repository

import android.content.Context
import com.example.mobilserver.model.DatabaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.mobilserver.model.Product
import kotlinx.serialization.json.Json
import java.io.*
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*
import kotlinx.coroutines.*


class ServerRepository(private val context: Context) {

    private val _productList = MutableStateFlow<List<Product>>(emptyList())
    val productList = _productList.asStateFlow()

    private val _receiptNumber = MutableStateFlow(loadReceiptNumber())
    val receiptNumber = _receiptNumber.asStateFlow()

    private var serverJob: Job? = null

    fun startServer() {
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val sslContext = setupSSLContext()
                val serverSocket = (sslContext.serverSocketFactory.createServerSocket(8443) as SSLServerSocket).apply {
                    useClientMode = false
                    needClientAuth = true
                    soTimeout = 5000
                }

                while (isActive) {
                    try {
                        serverSocket.accept().use { socket ->
                            (socket as SSLSocket).apply {
                                soTimeout = 2000
                                startHandshake()
                                handleClient(this)
                            }
                        }
                    } catch (e: Exception) {
                        println("Server error: ${e.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                println("Failed to start server: ${e.localizedMessage}")
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
    }

    private fun setupSSLContext(): SSLContext {
        val keyStore = loadKeyStore("server-keystore.p12")
        val trustStore = loadKeyStore("client-truststore.p12")

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "123456".toCharArray())
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }

        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, tmf.trustManagers, SecureRandom())
        }
    }

    private fun loadKeyStore(fileName: String): KeyStore {
        return KeyStore.getInstance("PKCS12").apply {
            load(context.assets.open(fileName), "123456".toCharArray())
        }
    }

    private fun handleClient(socket: SSLSocket) {
        val reader = socket.inputStream.bufferedReader()
        val requestLine = reader.readLine() ?: return
        val headers = generateSequence { reader.readLine()?.takeIf { it.isNotEmpty() } }.toList()

        // Gelen istek POST olmalı
        if (requestLine.startsWith("POST")) {
            val contentLength = headers.find { it.startsWith("Content-Length") }
                ?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0

            // Veriyi oku
            val data = CharArray(contentLength)
            reader.read(data)
            val received = data.concatToString()
            println("Received data: $received")

            // JSON verisini parse et ve listeyi güncelle
            try {
                val products = Json.decodeFromString<List<Product>>(received)
                _productList.value = products

                // Receipt numarasını artır ve kaydet
                _receiptNumber.value += 1
                saveReceiptNumber(_receiptNumber.value)

                // Veritabanına kaydet
                val dbHelper = DatabaseHelper(context)
                val listId = _receiptNumber.value  // list_id olarak receiptNumber kullanılabilir

                // Ürünleri ve tarih bilgisini veritabanına kaydet
                dbHelper.insertProducts(products, listId)

                println("Parsed product list: ${_productList.value}")
            } catch (e: Exception) {
                println("Error parsing JSON: ${e.localizedMessage}")
            }

            // HTTP yanıtı gönder
            OutputStreamWriter(socket.outputStream).use { writer ->
                val response = """
                HTTP/1.1 200 OK
                Content-Type: text/html
                Connection: close

                <html>
                    <body>
                        <h1>Veri alındı</h1>
                        <p>$received</p>
                    </body>
                </html>
            """.trimIndent()
                writer.write(response)
                writer.flush()
            }
        }
    }



    private fun loadReceiptNumber(): Int {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return prefs.getInt("receipt_number", 0)
    }

    private fun saveReceiptNumber(number: Int) {
        val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        prefs.edit().putInt("receipt_number", number).apply()
    }
}
