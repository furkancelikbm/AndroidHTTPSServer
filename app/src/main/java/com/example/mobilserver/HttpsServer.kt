package com.example.mobilserver

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory

class HttpsServer(private val context: Context) : NanoHTTPD(8443) {

    init {
        // Attempt to configure SSL for the server
        try {
            makeSecure(createSSLServerSocketFactory(), null)
        } catch (e: Exception) {
            Log.e("HttpsServer", "Error while setting up SSL", e)
        }
    }

    override fun serve(session: IHTTPSession): Response {
        // Respond with a simple message for testing the server
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "Android HTTPS Server Çalışıyor!")
    }

    private fun createSSLServerSocketFactory(): SSLServerSocketFactory {
        var keystoreFile: InputStream? = null
        try {
            // Load the keystore file
            keystoreFile = context.resources.openRawResource(R.raw.myhttpsserver)
            val keyStore = KeyStore.getInstance("PKCS12")  // Use PKCS12 format
            keyStore.load(keystoreFile, "123456".toCharArray())  // Keystore password

            // Extract the private key and certificate from the keystore
            val alias = "myhttpsserver"  // Alias for the key
            val privateKey = keyStore.getKey(alias, "123456".toCharArray()) as PrivateKey
            val certificate = keyStore.getCertificate(alias)  // Public key certificate
            val chain = keyStore.getCertificateChain(alias)

            // Initialize the KeyManagerFactory
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "123456".toCharArray())  // Keystore password

            // Initialize SSLContext with the key manager
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)

            // Return SSLServerSocketFactory from the SSLContext
            return sslContext.serverSocketFactory as SSLServerSocketFactory

        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL loading failed!", e)
            throw RuntimeException("Failed to load SSL configuration", e)
        } finally {
            // Close the keystore file stream
            try {
                keystoreFile?.close()
            } catch (e: Exception) {
                Log.e("HttpsServer", "Failed to close keystore file", e)
            }
        }
    }
}
