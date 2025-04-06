package com.example.mobilserver.view

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilserver.R
import com.example.mobilserver.model.Product
import com.example.mobilserver.viewmodel.ServerViewModel
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearOutSlowInEasing

val fakeReceiptFont = FontFamily(Font(R.font.fakereceipt)) // Fontu tanımladık


@Composable
fun ServerScreen(viewModel: ServerViewModel) {
    val productList by viewModel.productList.collectAsState()
    val receiptNumber by viewModel.receiptNumber.collectAsState()

    var animateUp by remember { mutableStateOf(false) }

    LaunchedEffect(productList.map { it.hashCode() }) {
        if (productList.isNotEmpty()) {
            animateUp = false
            delay(100)
            animateUp = true
            println("tetiklendi")
        }
    }

    val offsetY by animateDpAsState(
        targetValue = if (animateUp) 0.dp else 800.dp, // Fişin başlangıç noktası (aşağıda)
        animationSpec = tween(
            durationMillis = 3000, // Daha yavaş bir geçiş
            easing = EaseInOutQuad // Daha akıcı bir geçiş için easing fonksiyonu
        )
    )


    Scaffold(topBar = {}) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Sadece veri varsa göster
            if (productList.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .offset(y = offsetY)
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .shadow(8.dp, shape = MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 55.dp)
                    ) {
                        BusinessHeader(receiptNumber = receiptNumber)

                        Spacer(modifier = Modifier.height(16.dp))

                        ReceiptList(products = productList)

                        Spacer(modifier = Modifier.height(24.dp))

                        BusinessFooter()
                    }
                }
            }
        }
    }
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
            "FURKAN LTD.",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fakeReceiptFont),  // Fontu burada uyguladık
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            "Furkan Çelik",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fakeReceiptFont),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            "Bağcılar Adres No:7",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fakeReceiptFont),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Teşekkür ederiz yazısını buraya ekliyoruz
        Text(
            "BAĞCILAR VERGİ DAİRESİ",
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

@Preview(showSystemUi = true)
@Composable
fun PreviewServerScreen() {
    val mockProducts = listOf(
        Product(name = "Elma", price = 5.0, kdv = 8.0, count = 3),
        Product(name = "Süt", price = 15.0, kdv = 1.0, count = 2),
        Product(name = "Çikolata", price = 10.0, kdv = 18.0, count = 1)
    )

    val dummyReceipt = 42

    Column(modifier = Modifier.padding(16.dp)) {
        BusinessHeader(receiptNumber = dummyReceipt)
        ReceiptList(products = mockProducts)
        BusinessFooter()
    }
}
