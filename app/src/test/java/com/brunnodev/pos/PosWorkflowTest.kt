package com.brunnodev.pos

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PosWorkflowTest {
    @Test fun sandboxProviderApprovesPositiveAmount() = runBlocking {
        val result = SandboxPaymentProvider().authorize(
            PaymentRequest("sale-1", 2500, "positive")
        )

        assertEquals(ProviderResult(approved = true, reference = "sandbox-positive"), result)
    }

    @Test fun sandboxProviderDeclinesZeroAndNegativeAmounts() = runBlocking {
        val provider = SandboxPaymentProvider()

        assertFalse(provider.authorize(PaymentRequest("sale-zero", 0, "zero")).approved)
        assertFalse(provider.authorize(PaymentRequest("sale-negative", -1, "negative")).approved)
    }

    @Test fun sandboxProviderReturnsStableReferenceForRepeatedRequest() = runBlocking {
        val provider = SandboxPaymentProvider()
        val request = PaymentRequest("sale-1", 2500, "same-key")

        val first = provider.authorize(request)
        val second = provider.authorize(request)

        assertEquals(first, second)
        assertEquals("sandbox-same-key", first.reference)
    }
}
