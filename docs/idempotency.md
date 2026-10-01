# Idempotency

Cada checkout cria uma chave `pos-<saleId>`. A chave acompanha o `PaymentRequest` e é persistida junto ao pagamento.

Adaptadores de pagamento devem retornar o mesmo resultado para reenvios da mesma chave, evitando cobrança duplicada quando o aplicativo ou a rede repetir uma operação.
