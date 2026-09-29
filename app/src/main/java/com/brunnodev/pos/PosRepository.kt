package com.brunnodev.pos

import androidx.room.withTransaction
import java.util.UUID

class PosRepository(private val database: PosDatabase, private val provider: PaymentProvider, private val signer: DeviceSigner, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao get() = database.posDao()
    suspend fun catalog() = dao.catalog()

    suspend fun checkout(operatorId: String, cart: List<CartLine>): SaleEntity {
        if (operatorId.isBlank() || cart.isEmpty() || cart.any { it.quantity < 1 || it.item.unitPriceMinor < 0 }) throw PosException("Invalid checkout")
        val saleId = UUID.randomUUID().toString()
        val total = cart.sumOf { it.quantity.toLong() * it.item.unitPriceMinor }
        val paymentKey = "pos-$saleId"
        database.withTransaction {
            cart.forEach { line ->
                val item = dao.item(line.item.sku) ?: throw PosException("Unknown SKU ${line.item.sku}")
                if (!item.active || dao.decrementStock(item.sku, line.quantity) != 1) throw PosException("Insufficient stock")
            }
            dao.insertSale(SaleEntity(saleId, operatorId, SaleState.PAYMENT_PENDING, total, clock()))
            dao.insertLines(cart.map { SaleLineEntity(saleId, it.item.sku, it.item.name, it.quantity, it.item.unitPriceMinor) })
            dao.insertOutbox(event(saleId, "sale.created", """{"saleId":"$saleId","totalMinor":$total}"""))
        }
        val result = provider.authorize(PaymentRequest(saleId, total, paymentKey))
        val next = if (result.approved) SaleState.PAID else SaleState.VOIDED
        database.withTransaction {
            if (dao.transitionSale(saleId, SaleState.PAYMENT_PENDING, next) != 1) throw PosException("Sale state changed")
            dao.insertPayment(PaymentEntity(saleId = saleId, providerReference = result.reference, amountMinor = total, state = if (result.approved) PaymentState.APPROVED else PaymentState.DECLINED, idempotencyKey = paymentKey, createdAt = clock()))
            dao.insertOutbox(event(saleId, if (result.approved) "payment.approved" else "payment.declined", """{"saleId":"$saleId","amountMinor":$total}"""))
        }
        return SaleEntity(saleId, operatorId, next, total, clock())
    }

    suspend fun outboxBatch(limit: Int = 100) = dao.pendingOutbox(limit)
    suspend fun acknowledge(ids: List<String>) { if (ids.isNotEmpty()) dao.removeOutbox(ids) }
    suspend fun retry(ids: List<String>) { if (ids.isNotEmpty()) dao.recordFailures(ids) }
    private fun event(id: String, type: String, payload: String): OutboxEntity {
        val signed = """{"type":"$type","payload":$payload,"signature":"${signer.sign(payload)}"}"""
        return OutboxEntity(aggregateId = id, eventType = type, payload = signed, createdAt = clock())
    }
}
