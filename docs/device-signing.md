# Device signing

`DeviceSigner` mantém uma chave HMAC no Android Keystore e assina o payload antes do envio. A chave não deve ser exportada nem substituída por segredo armazenado em texto no banco ou no APK.

A assinatura oferece autenticidade do dispositivo dentro do modelo do laboratório; não substitui autenticação do operador, TLS ou controles do servidor.
