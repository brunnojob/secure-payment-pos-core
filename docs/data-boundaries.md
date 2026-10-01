# Sensitive data boundaries

O núcleo armazena identificadores de venda, valores em unidade mínima, estados e referências do provedor. Ele não deve armazenar PAN, CVV, PIN ou credenciais de adquirência.

Logs e eventos devem seguir a mesma regra e remover dados sensíveis antes de qualquer exportação ou sincronização.
