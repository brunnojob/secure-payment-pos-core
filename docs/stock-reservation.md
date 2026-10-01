# Stock reservation

A reserva usa uma atualização condicional: o estoque só é decrementado quando a quantidade disponível é suficiente. Se qualquer linha falhar, a transação inteira é revertida.

Essa regra evita estoque negativo e mantém linhas, venda e evento de criação consistentes.
