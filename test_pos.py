import unittest
from decimal import Decimal

from pos import Operator, PaymentState, PosCore, SaleLine


class PosCoreTests(unittest.TestCase):
    def setUp(self):
        self.pos = PosCore(b"a" * 32)
        self.cashier = Operator("c-1", "cashier")
        self.supervisor = Operator("s-1", "supervisor")
        self.sale = self.pos.create_sale(self.cashier, [SaleLine("SKU-1", 2, Decimal("5.25"))], "sale-1")

    def test_authorize_capture_refund_lifecycle(self):
        self.pos.authorize(self.cashier, "sale-1", 1050, "auth-1")
        with self.assertRaises(PermissionError):
            self.pos.capture(self.cashier, "sale-1", "capture-1")
        self.pos.capture(self.supervisor, "sale-1", "capture-1")
        self.pos.refund(self.supervisor, "sale-1", 400, "refund-1")
        self.assertEqual(self.sale.state, PaymentState.REFUNDED)
        self.assertEqual(self.sale.refunded_cents, 400)
        self.assertTrue(self.pos.verify_audit())

    def test_idempotency_prevents_duplicate_capture(self):
        self.pos.authorize(self.cashier, "sale-1", 1050, "auth-1")
        self.pos.capture(self.supervisor, "sale-1", "capture-1")
        self.pos.capture(self.supervisor, "sale-1", "capture-1")
        self.assertEqual(self.sale.captured_cents, 1050)

    def test_refund_cannot_exceed_captured_amount(self):
        self.pos.authorize(self.cashier, "sale-1", 1050, "auth-1")
        self.pos.capture(self.supervisor, "sale-1", "capture-1")
        with self.assertRaises(ValueError):
            self.pos.refund(self.supervisor, "sale-1", 1100, "refund-1")

    def test_audit_chain_detects_mutation(self):
        self.pos.authorize(self.cashier, "sale-1", 1050, "auth-1")
        self.pos.audit_events[0]["action"] = "tampered"
        self.assertFalse(self.pos.verify_audit())


if __name__ == "__main__":
    unittest.main()
