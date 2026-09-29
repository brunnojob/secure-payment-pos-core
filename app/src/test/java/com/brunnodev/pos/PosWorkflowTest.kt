package com.brunnodev.pos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PosWorkflowTest {
    @Test fun sandboxProviderIsDeterministicForRequest() {
        val provider = SandboxPaymentProvider()
        val request = PaymentRequest("sale-1", 2500, "same-key")
        assertEquals(provider.authorize(request), provider.authorize(request))
    }
    @Test fun invalidAmountIsDeclined() {
        assertFalse(SandboxPaymentProvider().authorize(PaymentRequest("sale-1", 0, "key")).approved)
    }
}
