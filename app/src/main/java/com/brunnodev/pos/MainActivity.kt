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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.room.Room
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val database by lazy { Room.databaseBuilder(applicationContext, PosDatabase::class.java, "pos.db").build() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cash = object : PaymentProvider {
            override suspend fun authorize(request: PaymentRequest) = ProviderResult(true, "cash-${request.idempotencyKey}")
        }
        setContent { MaterialTheme { PosScreen(PosRepository(database, cash, DeviceSigner())) } }
    }
}

@Composable
fun PosScreen(repository: PosRepository) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf(emptyList<CatalogItem>()) }
    val cart = remember { mutableStateMapOf<String, Int>() }
    var sku by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }; var stock by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }; var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }; var pending by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { products = repository.catalog(); pending = repository.outboxBatch().size }
    val total = cart.entries.sumOf { row -> products.first { it.sku == row.key }.unitPriceMinor * row.value }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("POS · Registro de vendas", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f))
            OutlinedTextField(name, { name = it }, label = { Text("Produto") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(price, { price = it }, label = { Text("Preço em centavos") }, modifier = Modifier.weight(1f))
            OutlinedTextField(stock, { stock = it }, label = { Text("Estoque") }, modifier = Modifier.weight(1f))
        }
        Button(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try { repository.putItem(CatalogItem(sku, name, price.toLong(), stock.toInt())); products = repository.catalog(); cart.clear(); confirmed = false; status = "Catálogo salvo." }
                catch (error: Exception) { status = error.message ?: "Dados inválidos" }
                finally { busy = false }
            }
        }) { Text("Salvar produto") }
        LazyColumn(Modifier.weight(1f)) {
            items(products) { product -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${product.name} · ${product.unitPriceMinor} centavos · estoque ${product.stock}", modifier = Modifier.weight(1f))
                Button(enabled = !busy && (cart[product.sku] ?: 0) < product.stock, onClick = { cart[product.sku] = (cart[product.sku] ?: 0) + 1; confirmed = false }) { Text("+ ${cart[product.sku] ?: 0}") }
            } }
        }
        OutlinedTextField(operator, { operator = it }, label = { Text("Operador") }, modifier = Modifier.fillMaxWidth())
        Row { Checkbox(confirmed, { confirmed = it }); Text("Confirmei o recebimento em dinheiro: $total centavos") }
        Button(enabled = !busy && confirmed && total > 0 && operator.isNotBlank(), onClick = {
            busy = true
            scope.launch {
                try {
                    val sale = repository.checkout(operator, cart.map { CartLine(products.first { p -> p.sku == it.key }, it.value) })
                    cart.clear(); confirmed = false; products = repository.catalog(); pending = repository.outboxBatch().size
                    status = "Venda ${sale.id} registrada."
                } catch (error: Exception) { status = error.message ?: "Falha ao registrar" }
                finally { busy = false }
            }
        }) { Text("Registrar venda recebida") }
        OutlinedTextField(token, { token = it }, label = { Text("Token Supabase") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && token.isNotBlank() && pending > 0, onClick = {
            busy = true
            scope.launch {
                try {
                    val delivery = ArchiveDelivery(token)
                    for (row in repository.outboxBatch()) { check(delivery.send(row.id, row.eventType, row.payload)); repository.acknowledge(listOf(row.id)) }
                    status = "Eventos sincronizados."
                } catch (error: Exception) { status = error.message ?: "Falha de conexão" }
                finally { pending = repository.outboxBatch().size; busy = false }
            }
        }) { Text("Sincronizar $pending eventos") }
        Text(status)
    }
}
