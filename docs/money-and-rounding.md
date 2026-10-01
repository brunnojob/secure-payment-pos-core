# Money representation

Valores monetários são inteiros em unidade mínima, representados por `Long`. O código não deve usar `Double` ou `Float` para preços, totais ou pagamentos.

A soma deve ocorrer no servidor ou no domínio antes da persistência, e o valor enviado ao provedor deve ser exatamente o total calculado para a venda.
