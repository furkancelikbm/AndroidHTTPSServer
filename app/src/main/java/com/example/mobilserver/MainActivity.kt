package com.example.mobilserver

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope  // lifecycleScope ekleyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            System.setProperty("javax.net.debug", "all")

            // Burada Compose UI'yi tanımlayabilirsiniz
        }

        // HTTPS sunucusunu başlat
        startHttpsServer()
    }

    private fun startHttpsServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Server keystore'u PKCS12 formatında assets klasöründen yükle
                val keyStore = KeyStore.getInstance("PKCS12").apply {
                    load(applicationContext.assets.open("server-keystore.p12"), "123456".toCharArray())
                }

                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
                    init(keyStore, "123456".toCharArray())
                }

                // Client truststore'u PKCS12 formatında assets klasöründen yükle
                val trustStore = KeyStore.getInstance("PKCS12").apply {
                    load(applicationContext.assets.open("client-truststore.p12"), "123456".toCharArray())
                }

                val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                    init(trustStore)
                }

                // SSLContext oluştur
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(kmf.keyManagers, tmf.trustManagers, SecureRandom())
                }

                val serverSocket = (sslContext.serverSocketFactory.createServerSocket(8443) as SSLServerSocket).apply {
                    useClientMode = false
                    needClientAuth = true
                    soTimeout = 20000
                }

                println("HTTPS server started on https://localhost:8443")

                while (true) {
                    println("Waiting for client connection...")
                    try {
                        val sslSocket = serverSocket.accept() as SSLSocket
                        println("Client connected!")

                        sslSocket.soTimeout = 20000
                        sslSocket.startHandshake()
                        handleClient(sslSocket)
                    } catch (e: SSLHandshakeException) {
                        println("SSL handshake failed: ${e.message}")
                    } catch (e: SocketTimeoutException) {
                        println("Socket timeout: ${e.message}")
                    } catch (e: Exception) {
                        println("Error during client connection: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Error starting server: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(sslSocket: SSLSocket) {
        try {
            println("Handling client connection...")

            // HTTP Başlıklarını Okuyun
            val reader = sslSocket.inputStream.bufferedReader()
            val firstLine = reader.readLine() // HTTP metodunu (GET, POST vs.) ve URL'yi okur
            val headers = mutableListOf<String>()

            // Başka başlıkları oku
            var line = reader.readLine()
            while (line.isNotEmpty()) {
                headers.add(line)
                line = reader.readLine()
            }

            // POST isteği kontrolü
            if (firstLine.startsWith("POST")) {
                println("Received POST request")

                // Content-Length başlığını kontrol et
                val contentLength = headers.find { it.startsWith("Content-Length") }?.split(":")?.get(1)?.trim()?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    // POST verisini oku
                    val postData = CharArray(contentLength)
                    reader.read(postData, 0, contentLength)
                    val jsonString = String(postData)
                    println("Received JSON: $jsonString")

                    val jsonObject = JSONObject(jsonString)
                    val suryaValue = jsonObject.optString("surya", "default_value")

                    // Yanıtı gönder
                    OutputStreamWriter(sslSocket.outputStream).use { writer ->
                        serve(writer, suryaValue)
                    }
                } else {
                    OutputStreamWriter(sslSocket.outputStream).use { writer ->
                        serve(writer, "No data in POST body.")
                    }
                }
            } else {
                OutputStreamWriter(sslSocket.outputStream).use { writer ->
                    serve(writer, "Unsupported HTTP method: ${firstLine.split(" ")[0]}")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                OutputStreamWriter(sslSocket.outputStream).use { writer ->
                    serve(writer, "Server error: ${e.localizedMessage}")
                }
            } catch (innerEx: Exception) {
                innerEx.printStackTrace()
            }
        } finally {
            try {
                println("Closing connection.")
                sslSocket.close()
            } catch (e: Exception) {
                println("Error closing socket: ${e.message}")
            }
        }
    }



    private fun serve(writer: OutputStreamWriter, response: String) {
        try {
            // HTTP 200 OK yanıt başlığını ekleyin
            val responseMessage = """
            HTTP/1.1 200 OK
            Content-Type: text/html
            Connection: close

            <html>
                <head>
                    <title>Mobil Server Response</title>
                </head>
                <body>
                    <h1>Mobil Server Yanıtı</h1>
                    <p>$response</p>
                    <hr/>
                    <p>Time: ${System.currentTimeMillis()}</p>
                </body>
            </html>
        """.trimIndent()

            // Yanıtı yaz
            writer.write(responseMessage)
            writer.flush()  // Veriyi gerçekten göndermek için flush kullanıyoruz
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }




}

