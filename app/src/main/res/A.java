package com.example.mobilserver


import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class HttpsServer(private val context: Context) : NanoHTTPD(8443) {

    init {
        makeSecure(loadKeystore(), null)  // HTTPS için SSL yükleme
    }

    override fun serve(session: IHTTPSession): Response {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "Android HTTPS Server Çalışıyor!")
    }

    private fun loadKeystore(): SSLContext {
        try {
            val keystoreFile: InputStream = context.resources.openRawResource(R.raw.) // Keystore dosyasını oku
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(keystoreFile, "123456".toCharArray()) // Keystore şifresi

            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "MyStrongKeyPass123".toCharArray()) // Key şifresi

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)
            return sslContext
        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL yüklenirken hata oluştu!", e)
            throw RuntimeException(e)
        }
    }
}
