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

    init {
        try {
            makeSecure(createSSLServerSocketFactory(), null)
            Log.d("HttpsServer", "HTTPS server started successfully on port 8443")
        } catch (e: Exception) {
            Log.e("HttpsServer", "Error while setting up SSL", e)
        }
    }

    override fun serve(session: IHTTPSession): Response {
        return when (session.method) {
            Method.POST -> {
                // POST isteği verilerini işle
                val postData = handlePostData(session)
                // Loglara yazdır
                Log.d("HttpsServer", "Received POST data: $postData")
                // HTML formatında yanıt oluştur
                val responseText = "<html><body><h1>Received POST data:</h1><pre>$postData</pre></body></html>"
                newFixedLengthResponse(Response.Status.OK, "text/html", responseText)
            }
            else -> {
                // Diğer HTTP metodları için yanıt
                newFixedLengthResponse(Response.Status.OK, "text/plain", "Android HTTPS Server Çalışıyor!")
            }
        }
    }

    private fun handlePostData(session: IHTTPSession): String {
        val postData: Map<String, String> = mutableMapOf()
        try {
            // POST verilerini ayrıştır
            session.parseBody(postData)
            // 'postData' map'ini string olarak döndür
            return postData.toString()
        } catch (e: Exception) {
            Log.e("HttpsServer", "Error while parsing POST data", e)
            return "{\"status\": \"error\", \"message\": \"Failed to parse POST data\"}"
        }
    }

    private fun createSSLServerSocketFactory(): SSLServerSocketFactory {
        var keystoreFile: InputStream? = null
        try {
            // Load the keystore file
            keystoreFile = context.resources.openRawResource(R.raw.server)
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(keystoreFile, "123456".toCharArray())  // Keystore password

            // Log available aliases in keystore
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val foundAlias = aliases.nextElement()
                Log.d("HttpsServer", "Keystore contains alias: $foundAlias")
            }

            val alias = "1"  // Alias for the key
            if (!keyStore.containsAlias(alias)) {
                Log.e("HttpsServer", "Alias '$alias' not found in keystore!")
                throw RuntimeException("Alias not found in keystore")
            }

            // Extract the private key and certificate from the keystore
            val privateKey = keyStore.getKey(alias, "123456".toCharArray()) as? PrivateKey
            if (privateKey == null) {
                Log.e("HttpsServer", "Private key is null for alias: $alias")
                throw RuntimeException("Private key not found")
            }

            val certificate = keyStore.getCertificate(alias)
            val chain = keyStore.getCertificateChain(alias)

            // Initialize the KeyManagerFactory
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "123456".toCharArray())

            // Initialize SSLContext
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)

            Log.d("HttpsServer", "SSL setup complete")

            return sslContext.serverSocketFactory as SSLServerSocketFactory

        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL loading failed!", e)
            throw RuntimeException("Failed to load SSL configuration", e)
        } finally {
            try {
                keystoreFile?.close()
            } catch (e: Exception) {
                Log.e("HttpsServer", "Failed to close keystore file", e)
            }
        }
    }
}

