package com.brunnodev.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PosScreen() } }
    }
}

@Composable
fun PosScreen() {
    val cart = remember { mutableStateMapOf<String, Int>() }
    val products = listOf("RF Offshore Kit" to 12990, "Safety Tag" to 2490, "Service Pack" to 4990)
    val total = cart.entries.sumOf { row -> products.first { it.first == row.key }.second.toLong() * row.value }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("RF POS", style = MaterialTheme.typography.headlineMedium)
        Text("Operator terminal · sandbox payments")
        LazyColumn(Modifier.weight(1f)) {
            items(products) { product ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(product.first); Text("R$ %.2f".format(product.second / 100.0)) }
                        Button(onClick = { cart[product.first] = (cart[product.first] ?: 0) + 1 }) { Text("Add") }
                    }
                }
            }
        }
        Text("Total: R$ %.2f".format(total / 100.0), style = MaterialTheme.typography.titleLarge)
        Button(modifier = Modifier.fillMaxWidth(), enabled = cart.isNotEmpty(), onClick = { cart.clear() }) { Text("Complete sandbox checkout") }
    }
}
