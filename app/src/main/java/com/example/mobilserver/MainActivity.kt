package com.example.mobilserver

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var server: HttpsServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ServerScreen()
        }

        // HTTPS Sunucusunu Başlat
        server = HttpsServer(this)
        try {
            server?.start()
            Toast.makeText(this, "Sunucu Başlatıldı!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("HttpsServer", "SSL yüklenirken hata oluştu!", e)
            throw RuntimeException("SSL bağlantısı oluşturulamadı", e)
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()  // Sunucuyu durdurma
    }
}

@Composable
fun ServerScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Android HTTPS Server Başlatıldı!")
        Spacer(modifier = Modifier.height(20.dp))
        Text("Adres: https://192.168.50.65:8443")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ServerScreen()
}
