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
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Burada Compose UI'yi tanımlayabilirsiniz
        }

        // HTTPS sunucusunu başlat
        startHttpsServer()
    }

    private fun startHttpsServer() {
        // lifecycleScope ile coroutine başlatıyoruz
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Keystore dosyasını yükle
                val keystore: KeyStore = KeyStore.getInstance("PKCS12")
                val keystoreInputStream: InputStream = assets.open("server.p12") // Sertifika dosyasını assets klasörüne koyun
                keystore.load(keystoreInputStream, "123456".toCharArray())

                // Anahtar yöneticisini kur
                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                kmf.init(keystore, "123456".toCharArray())

                // Güvenilen yöneticiyi kur
                val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                tmf.init(keystore)

                // SSLContext'i oluştur
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(kmf.keyManagers, tmf.trustManagers, SecureRandom())

                // SSLServerSocketFactory al
                val sslServerSocketFactory = sslContext.serverSocketFactory

                // Sunucu soketi oluştur (HTTPS portu)
                val serverSocket = sslServerSocketFactory.createServerSocket(8443) as SSLServerSocket
                serverSocket.useClientMode=false   // Sunucu modunda olacak

                println("HTTPS server started on https://localhost:8443")

                // Sunucu bağlantılarını dinle
                while (true) {
                    val sslSocket = serverSocket.accept() as SSLSocket // SSL soketini kabul et
                    handleClient(sslSocket) // Yeni bağlantıyı işleyin
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(sslSocket: SSLSocket) {
        try {
            sslSocket.startHandshake()

            // Bağlantı giriş ve çıkış stream'lerini al
            val inputStream: InputStream = sslSocket.inputStream
            val outputStream: OutputStream = sslSocket.outputStream

            // Gelen JSON verisini oku
            val jsonString = readInputStream(inputStream)

            // JSON'u işleyin ve "surya" anahtarını kontrol edin
            try {
                val jsonObject = JSONObject(jsonString)

                // "surya" anahtarını güvenli bir şekilde al
                val suryaValue = if (jsonObject.has("surya")) {
                    jsonObject.getString("surya")
                } else {
                    "default_value"  // Anahtar yoksa varsayılan değer
                }

                // Yanıt ver
                serve(outputStream, suryaValue)
            } catch (e: org.json.JSONException) {
                // JSON parse hatası
                val errorResponse = "Invalid JSON format"
                serve(outputStream, errorResponse)
            }

            // Bağlantıyı kapat
            outputStream.flush()
            sslSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serve(outputStream: OutputStream, response: String) {
        try {
            // Yanıt ver
            val responseMessage = "Response: $response"
            outputStream.write(responseMessage.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readInputStream(inputStream: InputStream): String {
        val stringBuilder = StringBuilder()
        val buffer = ByteArray(1024)
        var bytesRead: Int

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                stringBuilder.append(String(buffer, 0, bytesRead))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return stringBuilder.toString()
    }
}

