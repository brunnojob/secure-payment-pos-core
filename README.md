# POS Core

Aplicativo Android para catálogo, estoque, registro de vendas recebidas em dinheiro, eventos assinados no Keystore e fila de sincronização.

## Executar

Requisitos: Android SDK, Kotlin e Jetpack Compose.

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

## Funcionamento

O operador cadastra os produtos e confirma o recebimento antes de registrar a venda. Preços vêm do catálogo persistido; reservas e eventos usam transações Room. Recusas do provedor devolvem o estoque. A interface usa registro de dinheiro recebido; cartão e Pix exigem implementar `PaymentProvider` com fornecedor real. A assinatura local não é validada pela API de arquivo.

## Persistência de resultados

O arquivo de operações está em [vercel-home-telemetry-api.vercel.app](https://vercel-home-telemetry-api.vercel.app/laboratory.html?project=secure-payment-pos-core). As migrações Supabase estão no [repositório da API](https://github.com/brunnojob/vercel-home-telemetry-api/tree/main/supabase/migrations).

```sh
python cloud/sync.py enqueue resultado.json --project secure-payment-pos-core
python cloud/sync.py sync
```

Defina `BRUNNODEV_ACCESS_TOKEN` com sua sessão. A fila SQLite conserva os relatórios até confirmação do servidor; o mesmo conteúdo não gera registros duplicados. Tokens não são gravados no código.
