# Secure Payment POS Core

Native Kotlin Android POS foundation for operator checkout, offline catalog and inventory, role-aware payment workflows, an outbox sync path and device-keystore event signing.

## Run

Open the project in Android Studio and run the `app` configuration. The sandbox payment adapter is deterministic and does not contact a bank, Stone, Sunmi or a payment network.

## Architecture

Room stores catalog, sales, line items, payments and pending sync events. Checkout reserves stock in a database transaction, authorizes through a provider interface, records the result with an idempotency key and queues signed events for retryable synchronization. The Compose screen is an operator-flow demo.

No cardholder data is collected or stored. Provider credentials and production payment certification are outside this lab.
