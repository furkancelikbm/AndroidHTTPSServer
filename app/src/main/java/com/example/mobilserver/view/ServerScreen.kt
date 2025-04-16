package com.example.mobilserver.view

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilserver.R
import com.example.mobilserver.model.Product
import com.example.mobilserver.viewmodel.ServerViewModel
import kotlinx.coroutines.delay
import java.text.DecimalFormat

val fakeReceiptFont = FontFamily(Font(R.font.fakereceipt)) // Fontu tanımladık

@Composable
fun ServerScreen(viewModel: ServerViewModel) {
    val productList by viewModel.productList.collectAsState()
    val receiptNumber by viewModel.receiptNumber.collectAsState()

    var animateUp by remember { mutableStateOf(false) }

    LaunchedEffect(productList) {
        if (productList.isNotEmpty()) {
            animateUp = false
            delay(1100)
            animateUp = true
            println("baslatildi")
        }
    }

    val offsetY by animateDpAsState(
        targetValue = if (animateUp) 0.dp else 800.dp, // Fişin başlangıç noktası (aşağıda)
        animationSpec = tween(
            durationMillis = 1000, // Daha yavaş bir geçiş
            easing = EaseInOutQuad // Daha akıcı bir geçiş için easing fonksiyonu
        )
    )



    Scaffold(topBar = {}) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()  // Ekranı tamamen kaplamak için
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (productList.isNotEmpty()) {
                // Kart görünümü ile fiş tasarımı
                Card(
                    modifier = Modifier
                        .fillMaxSize()  // Ekranı tamamen kaplamak
                        .offset(y = offsetY)  // Fişi yukarı kaydırma animasyonu
                        .padding(16.dp)  // Kenarlardan boşluk bırakmak için
                        .shadow(8.dp, shape = MaterialTheme.shapes.medium), // Hafif gölge efekti
                    shape = MaterialTheme.shapes.medium, // Kenarları yuvarlatmak için
                    colors = CardDefaults.cardColors( // Arka plan rengini burada belirliyoruz
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)  // Arka plan rengi
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 55.dp)
                    ) {
                        BusinessHeader(receiptNumber = receiptNumber)

                        Spacer(modifier = Modifier.height(16.dp))

                        ReceiptList(products = productList)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Toplam Fiyat ve KDV Hesaplama
                        TotalAndVatSection(products = productList)

                        BusinessFooter()
                    }
                }
            }
        }
    }
}


@Composable
fun TotalAndVatSection(products: List<Product>) {
    val totalAmount = calculateTotalAmount(products)
    val totalVat = calculateTotalVat(products)

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))

        // Toplam KDV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Toplam KDV: ",
                fontSize = 16.sp,
                style = TextStyle(fontFamily = fakeReceiptFont),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Text(
                "₺${totalVat}",
                fontSize = 16.sp,
                style = TextStyle(fontFamily = fakeReceiptFont),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Toplam Fiyat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Toplam Tutar: ",
                fontSize = 16.sp,
                style = TextStyle(fontFamily = fakeReceiptFont),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Text(
                "₺${totalAmount}",
                fontSize = 16.sp,
                style = TextStyle(fontFamily = fakeReceiptFont),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}


// Toplam fiyatı hesaplayan fonksiyon
fun calculateTotalAmount(products: List<Product>): String {
    val totalAmount = products.sumOf { it.price * it.count }
    return DecimalFormat("#,##0.00").format(totalAmount)
}

// Toplam KDV'yi hesaplayan fonksiyon
fun calculateTotalVat(products: List<Product>): String {
    val totalVat = products.sumOf { it.price * it.count * (it.kdv / 100) }
    return DecimalFormat("#,##0.00").format(totalVat)
}

@Composable
fun BusinessHeader(receiptNumber: Int) {
    val currentDateTime = remember { java.time.LocalDateTime.now() }
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

    val formattedDate = currentDateTime.format(dateFormatter)
    val formattedTime = currentDateTime.format(timeFormatter)

    val fakeReceiptFont = FontFamily(Font(R.font.fakereceipt)) // Fontu tanımladık

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "ANONİM LTD.",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fakeReceiptFont),  // Fontu burada uyguladık
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            "TEST FİŞİDİR",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fakeReceiptFont),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            "Anonim Adres No:7",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fakeReceiptFont),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Teşekkür ederiz yazısını buraya ekliyoruz
        Text(
            "ANONİM VERGİ DAİRESİ",
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(fontFamily = fakeReceiptFont)  // Fontu burada uyguladık
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tarih ve saat ile fiş numarasını hizalıyoruz
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tarih ve saati sol tarafa yerleştiriyoruz
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Start),
                    style = TextStyle(fontFamily = fakeReceiptFont)  // Fontu burada uyguladık
                )
                Row(modifier = Modifier.align(Alignment.Start)) {
                    Text(
                        text = "Saat: ",
                        fontSize = 14.sp,
                        style = TextStyle(fontFamily = fakeReceiptFont)  // Fontu burada uyguladık
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 14.sp,
                        style = TextStyle(fontFamily = fakeReceiptFont)  // Fontu burada uyguladık
                    )
                }
            }

            // Fiş numarasını sağ tarafa yerleştiriyoruz
            Text(
                text = "Fiş No: #$receiptNumber",
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterVertically),
                style = TextStyle(fontFamily = fakeReceiptFont)  // Fontu burada uyguladık
            )
        }
    }
}

@Composable
fun ReceiptList(products: List<Product>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ürün", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
            Text("Fiyat", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
            Text("KDV", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
            Text("Adet", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
        }

        Spacer(modifier = Modifier.height(8.dp))

        products.forEach { product ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(product.name, fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
                Text("₺${product.price}", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
                Text("%${product.kdv.toInt()}", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
                Text("x${product.count}", fontSize = 16.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = TextStyle(fontFamily = fakeReceiptFont))  // Fontu burada uyguladık
            }
        }
    }
}

@Composable
fun BusinessFooter() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Teşekkür ederiz",
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = TextStyle(fontFamily = fakeReceiptFont)  // Fontu burada uyguladık
        )
    }
}
