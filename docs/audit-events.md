# Audit event vocabulary

Eventos de venda devem usar nomes estáveis:

- `sale.created` quando a venda é persistida;
- `payment.approved` quando o provedor aprova;
- `payment.declined` quando o provedor recusa.

Consumidores devem tratar eventos desconhecidos de forma compatível e não inferir valores monetários a partir de texto livre.
