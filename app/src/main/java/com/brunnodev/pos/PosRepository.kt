package com.brunnodev.pos

import androidx.room.withTransaction
import java.util.UUID
import org.json.JSONObject

class PosRepository(private val database: PosDatabase, private val provider: PaymentProvider, private val signer: DeviceSigner, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao get() = database.posDao()
    suspend fun catalog() = dao.catalog()
    suspend fun putItem(item: CatalogItem) {
        require(item.sku.matches(Regex("[A-Za-z0-9_-]{1,64}")) && item.name.isNotBlank() && item.name.length <= 120 && item.unitPriceMinor in 1..1000000000 && item.stock in 0..1000000)
        database.withTransaction { dao.putItem(item) }
    }

    suspend fun checkout(operatorId: String, cart: List<CartLine>): SaleEntity {
        if (operatorId.isBlank() || cart.isEmpty() || cart.any { it.quantity < 1 || it.item.unitPriceMinor < 0 }) throw PosException("Invalid checkout")
        val saleId = UUID.randomUUID().toString()
        var total = 0L
        var lines = emptyList<SaleLineEntity>()
        val paymentKey = "pos-$saleId"
        database.withTransaction {
            lines = cart.groupBy { it.item.sku }.map { (sku, requested) ->
                val quantity = requested.fold(0) { sum, line -> Math.addExact(sum, line.quantity) }
                val item = dao.item(sku) ?: throw PosException("Unknown SKU $sku")
                if (quantity !in 1..10000 || item.unitPriceMinor !in 0..1000000000 || !item.active || dao.decrementStock(item.sku, quantity) != 1) throw PosException("Insufficient stock")
                total = Math.addExact(total, Math.multiplyExact(quantity.toLong(), item.unitPriceMinor))
                SaleLineEntity(saleId, item.sku, item.name, quantity, item.unitPriceMinor)
            }
            if (total <= 0) throw PosException("Positive total required")
            dao.insertSale(SaleEntity(saleId, operatorId, SaleState.PAYMENT_PENDING, total, clock()))
            dao.insertLines(lines)
            dao.insertOutbox(event(saleId, "sale.created", """{"saleId":"$saleId","totalMinor":$total}"""))
        }
        val result = provider.authorize(PaymentRequest(saleId, total, paymentKey))
        val next = if (result.approved) SaleState.PAID else SaleState.VOIDED
        database.withTransaction {
            if (dao.transitionSale(saleId, SaleState.PAYMENT_PENDING, next) != 1) throw PosException("Sale state changed")
            if (!result.approved) lines.forEach { dao.restoreStock(it.sku, it.quantity) }
            dao.insertPayment(PaymentEntity(saleId = saleId, providerReference = result.reference, amountMinor = total, state = if (result.approved) PaymentState.APPROVED else PaymentState.DECLINED, idempotencyKey = paymentKey, createdAt = clock()))
            dao.insertOutbox(event(saleId, if (result.approved) "payment.approved" else "payment.declined", """{"saleId":"$saleId","amountMinor":$total}"""))
        }
        return SaleEntity(saleId, operatorId, next, total, clock())
    }

    suspend fun outboxBatch(limit: Int = 100) = dao.pendingOutbox(limit)
    suspend fun acknowledge(ids: List<String>) { if (ids.isNotEmpty()) dao.removeOutbox(ids) }
    suspend fun retry(ids: List<String>) { if (ids.isNotEmpty()) dao.recordFailures(ids) }
    private fun event(id: String, type: String, payload: String): OutboxEntity {
        val signed = JSONObject().put("type", type).put("payload", JSONObject(payload)).put("signature", signer.sign(payload)).toString()
        return OutboxEntity(aggregateId = id, eventType = type, payload = signed, createdAt = clock())
    }
}
