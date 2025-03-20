package com.example.mobilserver

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory

class HttpsServer(private val context: Context) : NanoHTTPD(8443) {

    private var lastReceivedData: String = "Henüz veri alınmadı."

    init {
        try {
            Log.d("HttpsServer", "SSL yapılandırması başlatılıyor...")
            makeSecure(createSSLServerSocketFactory(), null)
            Log.d("HttpsServer", "HTTPS server başarıyla başlatıldı! (Port: 8443)")
        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL yapılandırma hatası!", e)
        }
    }

    override fun serve(session: IHTTPSession): Response {
        Log.d("HttpsServer", "Yeni bir HTTP isteği alındı: ${session.method} ${session.uri}")

        return when (session.method) {
            Method.POST -> {
                Log.d("HttpsServer", "POST isteği alındı, veriler işleniyor...")

                // POST verilerini işle
                lastReceivedData = handlePostData(session)

                // Log: Veriyi ekrana bas
                Log.d("HttpsServer", "POST isteğinden gelen veriler: $lastReceivedData")

                // HTML formatında yanıt döndür (Ekrana yazdırma)
                newFixedLengthResponse(Response.Status.OK, "text/html", "Veri alındı, GET ile kontrol edebilirsiniz.")
            }
            else -> {
                Log.d("HttpsServer", "GET isteği alındı, son alınan veriler gösteriliyor...")
                val responseText = """
                    <html>
                        <head><title>HTTPS Server</title></head>
                        <body>
                            <h1>Gelen Veriler:</h1>
                            <pre>$lastReceivedData</pre>
                        </body>
                    </html>
                """.trimIndent()
                newFixedLengthResponse(Response.Status.OK, "text/html", responseText)
            }
        }
    }

    private fun handlePostData(session: IHTTPSession): String {
        val postData: MutableMap<String, String> = mutableMapOf()
        return try {
            Log.d("HttpsServer", "POST verileri ayrıştırılıyor...")
            session.parseBody(postData)

            // "postData" anahtarını kontrol et ve JSON string'ini al
            val jsonString = postData["postData"] ?: "{}"

            Log.d("HttpsServer", "Ayrıştırılan POST verisi: $jsonString")

            // JSON verisini tekrar düzenleyip döndür
            val jsonObject = JSONObject(jsonString)

            jsonObject.toString(4)  // JSON'ı daha okunaklı döndür (4 boşluklu format)
        } catch (e: Exception) {
            Log.e("HttpsServer", "POST verisi ayrıştırma hatası!", e)
            "status=error\nmessage=Failed to parse POST data"
        }
    }

    private fun createSSLServerSocketFactory(): SSLServerSocketFactory {
        var keystoreFile: InputStream? = null
        try {
            Log.d("HttpsServer", "Keystore dosyası yükleniyor...")
            keystoreFile = context.resources.openRawResource(R.raw.server)

            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(keystoreFile, "123456".toCharArray())  // Keystore şifresi

            Log.d("HttpsServer", "Keystore başarıyla yüklendi!")

            val alias = "1"  // Anahtar için alias
            if (!keyStore.containsAlias(alias)) {
                Log.e("HttpsServer", "Alias '$alias' keystore içinde bulunamadı!")
                throw RuntimeException("Alias bulunamadı")
            }

            val privateKey = keyStore.getKey(alias, "123456".toCharArray()) as? PrivateKey
            if (privateKey == null) {
                Log.e("HttpsServer", "Alias için özel anahtar bulunamadı!")
                throw RuntimeException("Özel anahtar bulunamadı")
            }

            Log.d("HttpsServer", "Özel anahtar başarıyla yüklendi!")

            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "123456".toCharArray())

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)

            Log.d("HttpsServer", "SSL yapılandırması tamamlandı, sunucu başlatılıyor...")

            return sslContext.serverSocketFactory as SSLServerSocketFactory

        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL yapılandırma hatası!", e)
            throw RuntimeException("SSL yapılandırması yüklenemedi", e)
        } finally {
            try {
                keystoreFile?.close()
                Log.d("HttpsServer", "Keystore dosyası kapatıldı.")
            } catch (e: Exception) {
                Log.e("HttpsServer", "Keystore dosyası kapatılamadı!", e)
            }
        }
    }
}
