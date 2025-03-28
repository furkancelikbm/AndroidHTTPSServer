package com.example.mobilserver

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope  // lifecycleScope ekleyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
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
                // Sunucu tarafında istemci sertifikasını doğrulamak için TrustManager eklemek
                val keystore: KeyStore = KeyStore.getInstance("PKCS12")
                val keystoreInputStream: InputStream = assets.open("server.p12")
                keystore.load(keystoreInputStream, "123456".toCharArray())

                // Sunucunun kendi sertifikalarını yükle
                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                kmf.init(keystore, "123456".toCharArray())

                // İstemci sertifikalarını doğrulamak için TrustManager'ı yükleyin
                val trustStore = KeyStore.getInstance("PKCS12")
                val trustStoreInputStream: InputStream = assets.open("client.p12") // İstemci sertifikasını içeren dosya
                trustStore.load(trustStoreInputStream, "123456".toCharArray())

                // TrustManagerFactory kullanarak TrustManager'ı yapılandırın
                val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                tmf.init(trustStore)

                // SSLContext'i oluşturun
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(kmf.keyManagers, tmf.trustManagers, SecureRandom())

                val sslServerSocketFactory = sslContext.serverSocketFactory

                val serverSocket = sslServerSocketFactory.createServerSocket(8443) as SSLServerSocket
                serverSocket.useClientMode = false
                serverSocket.needClientAuth=true

                // Set connection timeout for SSLServerSocket
                serverSocket.soTimeout = 20000  // 20 seconds connection timeout
                println("HTTPS server started on https://localhost:8443")

                while (true) {
                    println("Waiting for client connection...")
                    try {
                        val sslSocket = serverSocket.accept() as SSLSocket
                        println("Client connected!")

                        sslSocket.soTimeout = 20000  // 20 seconds read timeout

                        sslSocket.startHandshake()  // SSL handshake
                        handleClient(sslSocket)
                    } catch (e: SSLHandshakeException) {
                        println("SSL handshake failed: ${e.message}")
                    } catch (e: SocketTimeoutException) {
                        println("Socket timeout: ${e.message}")
                    } catch (e: Exception) {
                        println("Error during client connection: ${e.message}")
                        e.printStackTrace()
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
            // HTML formatında basit bir "Hello World" sayfası gönder
            val responseMessage = """
        <html>
            <head><title>Hello World</title></head>
            <body>
                <h1>Hello World</h1>
                <p>$response</p>
            </body>
        </html>
        """.trimIndent()

            // Yanıtı yaz
            writer.write(responseMessage)
            writer.flush()  // Verinin gerçekten gönderilmesini sağlamak için flush kullanıyoruz
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



}

