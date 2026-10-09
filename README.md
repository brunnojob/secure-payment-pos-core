# POS Core

An Android application for catalog management, stock, received cash sales, Keystore-signed events, and queued synchronization.

## Run

Requirements: Android SDK, Kotlin, and Jetpack Compose.

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

## Behavior

The operator creates products and confirms receipt of payment before recording a sale. Prices come from the persisted catalog; reservations and events use Room transactions. Provider rejections release reserved stock. Card and Pix payments require a real implementation of `PaymentProvider`. The archive API does not verify the local signature.

## Optional report archive

Use the [shared operations archive client](https://github.com/brunnojob/vercel-home-telemetry-api/tree/main/cloud) to queue `result.json` under project `secure-payment-pos-core`. The client uses `BRUNNODEV_ACCESS_TOKEN` and retains unacknowledged reports locally.

## License

Original source and documentation are MIT licensed; see [LICENSE](LICENSE). Third-party dependencies and media retain their respective terms. Maintained by [Brunno Dev](https://brunnodev.store).

## Implementation update

The Kotlin archive transport validates HTTPS destinations, token characters, idempotency keys, event kinds and payload sizes. A queue item is confirmed only when the receipt contains `persisted: true` and the matching client key.

Contribution trailer: `Co-authored-by: nyctophile <33561761+ineedfoundmyway@users.noreply.github.com>`.
