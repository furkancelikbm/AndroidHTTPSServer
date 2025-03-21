import android.content.Context
import android.util.Log
import com.example.mobilserver.R
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.SSLServerSocketFactory
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class HttpsServer(private val context: Context) : NanoHTTPD(8443) {

    private var lastReceivedData: String = "Henüz veri alınmadı."

    init {
        try {
            Log.d("HttpsServer", "SSL yapılandırması başlatılıyor...")
            makeSecure(createSSLServerSocketFactory(), null)  // Make the server secure with SSL
            Log.d("HttpsServer", "HTTPS server başarıyla başlatıldı! (Port: 8443)")
        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL yapılandırma hatası!", e)
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val clientIp = session.remoteIpAddress
        try {
            Log.d("HttpsServer", "Yeni bir HTTP isteği alındı: ${session.method} ${session.uri} - İstemci IP: $clientIp")

            // Log the status before responding
            Log.d("HttpsServer", "Cevap gönderilmeye çalışılıyor...")

            // Handle POST or GET request
            return when (session.method) {
                Method.POST -> {
                    Log.d("HttpsServer", "POST isteği alındı, veriler işleniyor...")
                    lastReceivedData = handlePostData(session)
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
        } catch (e: java.net.SocketException) {
            Log.e("HttpsServer", "Socket hatası! Bağlantı kapalı.", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", "Sunucu hatası")
        } catch (e: Exception) {
            Log.e("HttpsServer", "Bir hata oluştu!", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", "Sunucu hatası")
        }
    }

    private fun isClientCertificateValid(session: IHTTPSession): Boolean {
        // Direct client certificate validation may not be feasible in this context.
        // This placeholder can be used for further validation if needed.
        return true // Returning true as a placeholder for now.
    }

    private fun createSSLServerSocketFactory(): SSLServerSocketFactory {
        var keystoreFile: InputStream? = null
        var rootCertFile: InputStream? = null
        var intermediateCertFile: InputStream? = null
        try {
            // Load the server keystore (private key and server certificate)
            keystoreFile = context.resources.openRawResource(R.raw.server) // Server.p12
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(keystoreFile, "123456".toCharArray())  // Keystore password

            // Load intermediate and root certificates
            intermediateCertFile = context.resources.openRawResource(R.raw.intermediateca)  // Intermediate CA certificate
            rootCertFile = context.resources.openRawResource(R.raw.rootca)  // Root CA certificate

            val certificateFactory = CertificateFactory.getInstance("X.509")
            val rootCert = certificateFactory.generateCertificate(rootCertFile) as X509Certificate
            val intermediateCert = certificateFactory.generateCertificate(intermediateCertFile) as X509Certificate

            // Create a TrustStore and add certificates
            val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
            trustStore.load(null, null)
            trustStore.setCertificateEntry("root", rootCert)
            trustStore.setCertificateEntry("intermediate", intermediateCert)

            // Initialize TrustManagerFactory with the TrustStore
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(trustStore)

            // Initialize KeyManagerFactory with the server keystore
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "123456".toCharArray())  // Keystore password

            // Create SSLContext with both KeyManager and TrustManager
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(
                keyManagerFactory.keyManagers,
                trustManagerFactory.trustManagers,
                null
            )

            // Create the SSLServerSocketFactory
            val serverSocketFactory = sslContext.serverSocketFactory

            // Return the factory with client authentication enabled
            return serverSocketFactory as SSLServerSocketFactory

        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL yapılandırma hatası!", e)
            throw RuntimeException("SSL yapılandırması yüklenemedi", e)
        } finally {
            try {
                keystoreFile?.close()
                rootCertFile?.close()
                intermediateCertFile?.close()
                Log.d("HttpsServer", "Keystore ve sertifikalar kapatıldı.")
            } catch (e: Exception) {
                Log.e("HttpsServer", "Dosyalar kapatılamadı!", e)
            }
        }
    }

    private fun handlePostData(session: IHTTPSession): String {
        val postData: MutableMap<String, String> = mutableMapOf()
        return try {
            Log.d("HttpsServer", "POST verileri ayrıştırılıyor...")
            session.parseBody(postData)

            // Extract JSON string
            val jsonString = postData["postData"] ?: "{}"
            Log.d("HttpsServer", "Ayrıştırılan POST verisi: $jsonString")

            // Return formatted JSON
            val jsonObject = JSONObject(jsonString)
            jsonObject.toString(4)  // Format the JSON with indentation

        } catch (e: Exception) {
            Log.e("HttpsServer", "POST verisi ayrıştırma hatası!", e)
            "status=error\nmessage=Failed to parse POST data"
        }
    }
}
