package com.example.mobilserver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.SocketTimeoutException
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("javax.net.debug", "all")
        startHttpsServer()
    }

    private fun startHttpsServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sslContext = setupSSLContext()
                val serverSocket = (sslContext.serverSocketFactory.createServerSocket(8443) as SSLServerSocket).apply {
                    useClientMode = false
                    needClientAuth = true
                    soTimeout = 5000
                }
                println("HTTPS server started on https://localhost:8443")

                while (true) {
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
            init(keyStore, getKeystorePassword())
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }
        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, tmf.trustManagers, SecureRandom())
            defaultSSLParameters.protocols = arrayOf("TLSv1.2", "TLSv1.3")  // ✅ Güçlü protokoller
        }
    }

    private fun loadKeyStore(fileName: String): KeyStore {
        return KeyStore.getInstance("PKCS12").apply {
            load(applicationContext.assets.open(fileName), getKeystorePassword())
        }
    }

    private fun getKeystorePassword(): CharArray {
        // 🚨 Sabit şifre yerine güvenli bir kaynaktan oku (örn: EncryptedSharedPreferences, Android Keystore)
        return "123456".toCharArray()
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
        println("Received POST request: ${postData?.concatToString() ?: "No data"}")
        serve(sslSocket, postData?.concatToString() ?: "No data in POST body.")
    }

    private fun serve(socket: SSLSocket, response: String) {
        OutputStreamWriter(socket.outputStream).use { writer ->
            writer.write(
                """HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n""" + // ✅ HTTP standardına uygun
                        """<html><head><title>Mobil Server Response</title></head><body><h1>Response</h1><p>$response</p></body></html>"""
            )
            writer.flush()
        }
    }
}
