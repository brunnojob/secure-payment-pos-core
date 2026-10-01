# Testing strategy

Os testes unitários devem cobrir regras determinísticas do domínio, especialmente:

- autorização aprovada e recusada;
- rejeição de valor inválido;
- idempotência do provedor;
- transições de estado;
- reserva atômica de estoque.

Testes de integração devem validar Room e WorkManager com dependências controladas, sem rede de pagamento real.
