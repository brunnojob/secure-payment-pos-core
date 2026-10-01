# Contributing

Obrigado por contribuir com o Secure Payment POS Core.

## Fluxo

1. Crie uma branch curta a partir de `main`.
2. Mantenha cada pull request focado em uma mudança revisável.
3. Adicione ou atualize testes quando alterar comportamento.
4. Não inclua credenciais, dados de cartão ou chaves de produção.
5. Descreva no PR o risco, a validação executada e qualquer migração necessária.

## Validação local

Execute os testes do módulo Android antes de abrir o PR:

```bash
./gradlew :app:test
```
