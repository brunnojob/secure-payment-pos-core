# Retry policy

Falhas de rede ou respostas fora de 2xx deixam os eventos na outbox e incrementam o contador de tentativas. O WorkManager pode executar novamente o worker.

O servidor deve ser idempotente porque um evento pode ser enviado mais de uma vez. A política de retry não deve remover eventos apenas porque uma tentativa falhou.
