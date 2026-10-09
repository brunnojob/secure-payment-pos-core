# POS Core

An Android application for catalog management, stock, received cash sales, Keystore-signed events, and queued synchronization.

## Run

Requirements: Android SDK, Kotlin, and Jetpack Compose.

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

## Behavior

The operator creates products and confirms receipt of payment before recording a sale. Prices come from the persisted catalog; reservations and events use Room transactions. Provider rejections release reserved stock. Card and Pix payments require a real implementation of `PaymentProvider`. The archive API does not verify the local signature.

## Result synchronization

The [operations archive](https://vercel-home-telemetry-api.vercel.app/laboratory.html?project=secure-payment-pos-core) stores execution results. Supabase migrations are in the [API repository](https://github.com/brunnojob/vercel-home-telemetry-api/tree/main/supabase/migrations).

```sh
python cloud/sync.py enqueue result.json --project secure-payment-pos-core
python cloud/sync.py sync
```

Set `BRUNNODEV_ACCESS_TOKEN` to your session token. The SQLite outbox retains reports until the server confirms persistence; identical content does not create duplicate records. Tokens are not stored in source code. To run the synchronization tests:

```sh
python -m unittest discover -s cloud
```
