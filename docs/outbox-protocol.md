# Signed outbox protocol

Cada evento de domínio é persistido em `sync_outbox` antes da sincronização. O payload contém tipo, dados e assinatura HMAC produzida pelo keystore do dispositivo.

O worker envia eventos em lote via HTTP POST. Respostas 2xx confirmam o lote e removem os itens; falhas incrementam `attempts` e permitem nova tentativa.

O endpoint remoto deve validar a assinatura, aceitar reenvio seguro e deduplicar eventos pelo identificador da outbox.
