from __future__ import annotations

import hashlib
import hmac
import json
import secrets
import time
from dataclasses import dataclass, field
from decimal import Decimal
from enum import Enum
from typing import Any


class PaymentState(str, Enum):
    CREATED = "created"
    AUTHORIZED = "authorized"
    CAPTURED = "captured"
    VOIDED = "voided"
    REFUNDED = "refunded"
    DECLINED = "declined"


@dataclass(frozen=True)
class Operator:
    operator_id: str
    role: str


@dataclass(frozen=True)
class SaleLine:
    sku: str
    quantity: int
    unit_price: Decimal


@dataclass
class Sale:
    sale_id: str
    lines: tuple[SaleLine, ...]
    state: PaymentState = PaymentState.CREATED
    authorized_cents: int = 0
    captured_cents: int = 0
    refunded_cents: int = 0
    idempotency: dict[str, tuple[str, str]] = field(default_factory=dict)

    @property
    def total_cents(self) -> int:
        return sum(int((line.unit_price * line.quantity * 100).quantize(Decimal("1"))) for line in self.lines)


class PosCore:
    def __init__(self, audit_key: bytes):
        if len(audit_key) < 32:
            raise ValueError("audit key must contain at least 32 bytes")
        self._audit_key = audit_key
        self.sales: dict[str, Sale] = {}
        self.audit_events: list[dict[str, Any]] = []
        self._last_hash = "0" * 64

    def create_sale(self, operator: Operator, lines: list[SaleLine], sale_id: str | None = None) -> Sale:
        self._require(operator, {"cashier", "supervisor", "admin"})
        if not lines or any(not line.sku or line.quantity < 1 or line.unit_price <= 0 for line in lines):
            raise ValueError("sale lines are invalid")
        identifier = sale_id or secrets.token_urlsafe(12)
        if identifier in self.sales:
            raise ValueError("sale identifier already exists")
        sale = Sale(identifier, tuple(lines))
        self.sales[identifier] = sale
        self._audit(operator, identifier, "sale_created", {"total_cents": sale.total_cents})
        return sale

    def authorize(self, operator: Operator, sale_id: str, amount_cents: int, idempotency_key: str) -> Sale:
        self._require(operator, {"cashier", "supervisor", "admin"})
        sale = self._sale(sale_id)
        fingerprint = f"authorize:{amount_cents}"
        prior = sale.idempotency.get(idempotency_key)
        if prior:
            if prior[0] != fingerprint:
                raise ValueError("idempotency key reused with different operation")
            return sale
        if sale.state != PaymentState.CREATED or amount_cents != sale.total_cents or amount_cents <= 0:
            raise ValueError("authorization does not match an open sale")
        sale.authorized_cents = amount_cents
        sale.state = PaymentState.AUTHORIZED
        sale.idempotency[idempotency_key] = (fingerprint, sale.state.value)
        self._audit(operator, sale_id, "payment_authorized", {"amount_cents": amount_cents})
        return sale

    def capture(self, operator: Operator, sale_id: str, idempotency_key: str) -> Sale:
        self._require(operator, {"supervisor", "admin"})
        sale = self._sale(sale_id)
        fingerprint = "capture"
        prior = sale.idempotency.get(idempotency_key)
        if prior:
            if prior[0] != fingerprint:
                raise ValueError("idempotency key reused with different operation")
            return sale
        if sale.state != PaymentState.AUTHORIZED:
            raise ValueError("only authorized payments can be captured")
        sale.captured_cents = sale.authorized_cents
        sale.state = PaymentState.CAPTURED
        sale.idempotency[idempotency_key] = (fingerprint, sale.state.value)
        self._audit(operator, sale_id, "payment_captured", {"amount_cents": sale.captured_cents})
        return sale

    def refund(self, operator: Operator, sale_id: str, amount_cents: int, idempotency_key: str) -> Sale:
        self._require(operator, {"supervisor", "admin"})
        sale = self._sale(sale_id)
        fingerprint = f"refund:{amount_cents}"
        prior = sale.idempotency.get(idempotency_key)
        if prior:
            if prior[0] != fingerprint:
                raise ValueError("idempotency key reused with different operation")
            return sale
        refundable = sale.captured_cents - sale.refunded_cents
        if sale.state not in {PaymentState.CAPTURED, PaymentState.REFUNDED} or not 0 < amount_cents <= refundable:
            raise ValueError("refund amount exceeds captured balance")
        sale.refunded_cents += amount_cents
        sale.state = PaymentState.REFUNDED
        sale.idempotency[idempotency_key] = (fingerprint, sale.state.value)
        self._audit(operator, sale_id, "payment_refunded", {"amount_cents": amount_cents})
        return sale

    def void(self, operator: Operator, sale_id: str) -> Sale:
        self._require(operator, {"supervisor", "admin"})
        sale = self._sale(sale_id)
        if sale.state != PaymentState.AUTHORIZED:
            raise ValueError("only uncaptured authorizations can be voided")
        sale.state = PaymentState.VOIDED
        self._audit(operator, sale_id, "payment_voided", {})
        return sale

    def verify_audit(self) -> bool:
        previous = "0" * 64
        for event in self.audit_events:
            unsigned = {key: value for key, value in event.items() if key != "signature"}
            if unsigned["previous_hash"] != previous:
                return False
            expected = hmac.new(self._audit_key, self._canonical(unsigned), hashlib.sha256).hexdigest()
            if not hmac.compare_digest(expected, event["signature"]):
                return False
            previous = event["signature"]
        return True

    def sign_webhook(self, payload: bytes) -> str:
        return hmac.new(self._audit_key, payload, hashlib.sha256).hexdigest()

    def _audit(self, operator: Operator, sale_id: str, action: str, details: dict[str, int]) -> None:
        unsigned = {
            "sequence": len(self.audit_events),
            "timestamp": int(time.time()),
            "operator_id": operator.operator_id,
            "role": operator.role,
            "sale_id": sale_id,
            "action": action,
            "details": details,
            "previous_hash": self._last_hash,
        }
        signature = hmac.new(self._audit_key, self._canonical(unsigned), hashlib.sha256).hexdigest()
        self.audit_events.append({**unsigned, "signature": signature})
        self._last_hash = signature

    def _sale(self, sale_id: str) -> Sale:
        try:
            return self.sales[sale_id]
        except KeyError as error:
            raise KeyError("sale not found") from error

    @staticmethod
    def _require(operator: Operator, allowed: set[str]) -> None:
        if operator.role not in allowed:
            raise PermissionError("operator role is not allowed")

    @staticmethod
    def _canonical(value: dict[str, Any]) -> bytes:
        return json.dumps(value, sort_keys=True, separators=(",", ":")).encode()


def main() -> None:
    terminal = PosCore(b"p" * 32)
    cashier = Operator("cashier-04", "cashier")
    supervisor = Operator("supervisor-01", "supervisor")
    sale = terminal.create_sale(cashier, [SaleLine("SKU-100", 2, Decimal("7.50"))], "sale-demo")
    terminal.authorize(cashier, sale.sale_id, sale.total_cents, "auth-demo")
    terminal.capture(supervisor, sale.sale_id, "capture-demo")
    print(json.dumps({
        "sale_id": sale.sale_id,
        "state": sale.state.value,
        "total_cents": sale.total_cents,
        "audit_valid": terminal.verify_audit(),
    }, indent=2))


if __name__ == "__main__":
    main()
