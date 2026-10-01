# Payment provider contract

`PaymentProvider` é a fronteira entre o checkout local e um adaptador de pagamento.

- `PaymentRequest` carrega apenas o identificador da venda, valor em centavos e chave de idempotência.
- `ProviderResult.approved` determina se a venda avança para `PAID` ou `VOIDED`.
- `reference` deve ser tratado como identificador do provedor, nunca como dado de cartão.
- Adaptadores reais devem ser idempotentes para a mesma chave e nunca persistir dados sensíveis.

O `SandboxPaymentProvider` permanece determinístico e não faz chamadas de rede.
