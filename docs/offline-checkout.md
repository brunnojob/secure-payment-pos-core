# Offline checkout

O checkout foi desenhado para continuar funcionando sem rede:

1. catálogo e estoque são lidos do Room;
2. a reserva de estoque e a venda são gravadas na mesma transação;
3. a autorização usa a interface local do provedor;
4. eventos ficam na outbox até a conectividade retornar.

A sincronização é eventual. O aplicativo deve informar falhas de envio sem apagar eventos pendentes.
