# Secure Payment POS Core

A local point-of-sale workflow simulator with operator permissions, idempotent payments, state transitions and signed audit events.

## Run

```bash
python pos.py
python -m unittest
```

No card data is collected or stored. Payment processing is simulated and does not contact a bank or payment network.
