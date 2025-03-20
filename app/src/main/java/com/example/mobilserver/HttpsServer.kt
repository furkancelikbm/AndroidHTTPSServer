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
import javax.net.ssl.X509TrustManager
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
        Log.d("HttpsServer", "Yeni bir HTTP isteği alındı: ${session.method} ${session.uri} - İstemci IP: $clientIp")

        // Handle GET or POST request
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
    }

    private fun createSSLServerSocketFactory(): SSLServerSocketFactory {
        var keystoreFile: InputStream? = null
        var rootCertFile: InputStream? = null
        var subRootCertFile: InputStream? = null
        try {
            // Load the server keystore (private key and server certificate)
            keystoreFile = context.resources.openRawResource(R.raw.server)
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(keystoreFile, "123456".toCharArray())  // Keystore password

            // Load root certificate and sub-root certificate
            rootCertFile = context.resources.openRawResource(R.raw.ca)  // Root certificate
            subRootCertFile = context.resources.openRawResource(R.raw.sub_ca)  // Sub-root certificate

            // Create a CertificateFactory for X.509 certificates
            val certificateFactory = CertificateFactory.getInstance("X.509")

            // Load root and sub-root certificates
            val rootCert = certificateFactory.generateCertificate(rootCertFile) as X509Certificate
            val subRootCert = certificateFactory.generateCertificate(subRootCertFile) as X509Certificate

            // Create a KeyStore to hold the trusted certificates (root + sub-root)
            val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
            trustStore.load(null, null)
            trustStore.setCertificateEntry("root", rootCert)
            trustStore.setCertificateEntry("subRoot", subRootCert)

            // Initialize the TrustManagerFactory with the truststore
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(trustStore)

            // Create the SSLContext with both KeyManager and TrustManager
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
                    init(keyStore, "123456".toCharArray())  // Keystore password
                }.keyManagers,
                trustManagerFactory.trustManagers,
                null
            )

            // Return the correct SSLServerSocketFactory (for server-side SSL connections)
            return sslContext.serverSocketFactory as SSLServerSocketFactory

        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL yapılandırma hatası!", e)
            throw RuntimeException("SSL yapılandırması yüklenemedi", e)
        } finally {
            try {
                keystoreFile?.close()
                rootCertFile?.close()
                subRootCertFile?.close()
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
