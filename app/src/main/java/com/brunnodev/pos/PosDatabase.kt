package com.brunnodev.pos

import androidx.room.*

@Dao
interface PosDao {
    @Query("SELECT * FROM catalog WHERE active = 1 ORDER BY name") suspend fun catalog(): List<CatalogItem>
    @Query("SELECT * FROM catalog WHERE sku = :sku") suspend fun item(sku: String): CatalogItem?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putItem(item: CatalogItem)
    @Insert suspend fun insertSale(sale: SaleEntity)
    @Insert suspend fun insertLines(lines: List<SaleLineEntity>)
    @Insert suspend fun insertPayment(payment: PaymentEntity)
    @Insert suspend fun insertOutbox(event: OutboxEntity)
    @Query("UPDATE catalog SET stock = stock - :quantity WHERE sku = :sku AND stock >= :quantity") suspend fun decrementStock(sku: String, quantity: Int): Int
    @Query("UPDATE sales SET state = :state WHERE id = :saleId AND state = :expected") suspend fun transitionSale(saleId: String, expected: SaleState, state: SaleState): Int
    @Query("SELECT * FROM sync_outbox ORDER BY createdAt LIMIT :limit") suspend fun pendingOutbox(limit: Int): List<OutboxEntity>
    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)") suspend fun removeOutbox(ids: List<String>)
    @Query("UPDATE sync_outbox SET attempts = attempts + 1 WHERE id IN (:ids)") suspend fun recordFailures(ids: List<String>)
}

@Database(entities = [CatalogItem::class, SaleEntity::class, SaleLineEntity::class, PaymentEntity::class, OutboxEntity::class], version = 1, exportSchema = true)
abstract class PosDatabase : RoomDatabase() { abstract fun posDao(): PosDao }
