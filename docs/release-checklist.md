# Release checklist

Antes de publicar uma versão de teste:

- executar `./gradlew :app:test`;
- confirmar que nenhuma credencial aparece no código ou no APK;
- validar migrações Room e exportação de schema;
- revisar estados de pagamento e retry da outbox;
- confirmar que o endpoint de sincronização usa HTTPS;
- registrar o commit e a versão do aplicativo.
