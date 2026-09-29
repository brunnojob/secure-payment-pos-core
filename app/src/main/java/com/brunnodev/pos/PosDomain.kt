package com.brunnodev.pos

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SaleState { OPEN, PAYMENT_PENDING, PAID, VOIDED, REFUNDED }
enum class PaymentState { REQUESTED, APPROVED, DECLINED, REVERSED }

@Entity(tableName = "catalog")
data class CatalogItem(@PrimaryKey val sku: String, val name: String, val unitPriceMinor: Long, val stock: Int, val active: Boolean = true)
@Entity(tableName = "sales")
data class SaleEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val operatorId: String, val state: SaleState, val totalMinor: Long, val createdAt: Long)
@Entity(tableName = "sale_lines", primaryKeys = ["saleId", "sku"])
data class SaleLineEntity(val saleId: String, val sku: String, val label: String, val quantity: Int, val unitPriceMinor: Long)
@Entity(tableName = "payments")
data class PaymentEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val saleId: String, val providerReference: String?, val amountMinor: Long, val state: PaymentState, val idempotencyKey: String, val createdAt: Long)
@Entity(tableName = "sync_outbox")
data class OutboxEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val aggregateId: String, val eventType: String, val payload: String, val createdAt: Long, val attempts: Int = 0)

data class CartLine(val item: CatalogItem, val quantity: Int)
data class PaymentRequest(val saleId: String, val amountMinor: Long, val idempotencyKey: String)
data class ProviderResult(val approved: Boolean, val reference: String?)
interface PaymentProvider { suspend fun authorize(request: PaymentRequest): ProviderResult }
class SandboxPaymentProvider : PaymentProvider {
    override suspend fun authorize(request: PaymentRequest) =
        ProviderResult(request.amountMinor > 0, "sandbox-${request.idempotencyKey}")
}
class PosException(message: String) : IllegalStateException(message)
