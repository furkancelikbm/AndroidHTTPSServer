package com.example.mobilserver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

class MainActivity : ComponentActivity() {
    private var serverJob: Job? = null
    private val _responseText = mutableStateOf("Henüz veri alınmadı.") // UI için değişken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ServerScreen(_responseText.value) // Güncellenen veriyi ekranda göster
        }

        startHttpsServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        serverJob?.cancel() // Sunucuyu temizle
    }

    private fun startHttpsServer() {
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val sslContext = setupSSLContext()
                val serverSocket = (sslContext.serverSocketFactory.createServerSocket(8443) as SSLServerSocket).apply {
                    useClientMode = false
                    needClientAuth = true
                    soTimeout = 5000
                }
                println("HTTPS server started on https://localhost:8443")

                while (isActive) { // Sunucu çalışırken sürekli bekle
                    try {
                        serverSocket.accept().use { sslSocket ->
                            (sslSocket as SSLSocket).apply {
                                soTimeout = 2000
                                startHandshake()
                                handleClient(this)
                            }
                        }
                    } catch (e: SSLHandshakeException) {
                        println("SSL Handshake failed: ${e.localizedMessage}")
                    } catch (e: SocketTimeoutException) {
                        println("Client connection timed out: ${e.localizedMessage}")
                    } catch (e: Exception) {
                        println("Client connection error: ${e.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                println("Error starting server: ${e.localizedMessage}")
            }
        }
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
            defaultSSLParameters.protocols = arrayOf("TLSv1.2", "TLSv1.3")
        }
    }

    private fun loadKeyStore(fileName: String): KeyStore {
        return KeyStore.getInstance("PKCS12").apply {
            load(applicationContext.assets.open(fileName), "123456".toCharArray())
        }
    }

    private fun handleClient(sslSocket: SSLSocket) {
        sslSocket.use {
            try {
                val reader = it.inputStream.bufferedReader()
                val requestLine = reader.readLine() ?: return
                val headers = generateSequence { reader.readLine().takeIf { line -> line.isNotEmpty() } }.toList()
                if (requestLine.startsWith("POST")) handlePostRequest(reader, headers, it)
                else serve(it, "Unsupported request method.")
            } catch (e: Exception) {
                println("Error handling client: ${e.localizedMessage}")
                serve(sslSocket, "Server error: ${e.localizedMessage}")
            }
        }
    }

    private fun handlePostRequest(reader: BufferedReader, headers: List<String>, sslSocket: SSLSocket) {
        val contentLength = headers.find { it.startsWith("Content-Length") }?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
        val postData = if (contentLength > 0) CharArray(contentLength).apply { reader.read(this, 0, contentLength) } else null
        val receivedData = postData?.concatToString() ?: "No data"

        println("Received POST request: $receivedData")

        // UI güncellemesi için ana iş parçacığında çalıştır
        CoroutineScope(Dispatchers.Main).launch {
            if (receivedData.isNotEmpty()) {
                try {
                    // JSON verisini düzgün parse et
                    val productList = Json.decodeFromString<List<Product>>(receivedData)
                    _responseText.value = Json.encodeToString(productList) // JSON verisi düzgünse, responseText'i güncelle
                } catch (e: Exception) {
                    _responseText.value = "Veri işlenemedi: ${e.localizedMessage}"
                }
            } else {
                _responseText.value = "Veri alınamadı."
            }
        }

        serve(sslSocket, receivedData)
    }

    private fun serve(socket: SSLSocket, response: String) {
        OutputStreamWriter(socket.outputStream).use { writer ->
            writer.write(
                """HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n""" +
                        """<html><head><title>Mobil Server Response</title></head><body><h1>Response</h1><p>$response</p></body></html>"""
            )
            writer.flush()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(responseText: String) {
    var response by remember { mutableStateOf(responseText) }

    LaunchedEffect(responseText) {
        try {
            // Log response text
            println("Gelen Response Text: $responseText")
            response = responseText
        } catch (e: Exception) {
            println("Response Text güncellenirken hata: ${e.localizedMessage}")
        }
    }

    val productList = try {
        println("Veri parse ediliyor: $response")
        Json.decodeFromString<List<Product>>(response).also {
            println("Veri başarıyla parse edildi: $it")
        }
    } catch (e: Exception) {
        println("JSON parse hatası: ${e.localizedMessage}")
        emptyList() // Eğer JSON verisi hatalıysa boş liste döndür
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Market Fişi") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Market Fişi",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (productList.isEmpty()) {
                println("Henüz veri yok.")
                Text(text = "Henüz veri yok.", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                println("Ürün listesi mevcut: ${productList.size} ürün")
                ReceiptList(productList) // Gelen ürün listesine göre UI'yi render et
            }
        }
    }
}

@Composable
fun ReceiptList(products: List<Product>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ürün", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                Text("KDV", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Adet", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
        items(products) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(product.name, fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    Text("%${product.kdv.toInt()}", fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("x${product.count}", fontSize = 18.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }
        }
    }
}
