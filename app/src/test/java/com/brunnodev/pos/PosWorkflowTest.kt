package com.brunnodev.pos

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PosWorkflowTest {
    @Test fun sandboxProviderIsDeterministicForRequest() = runBlocking {
        val provider = SandboxPaymentProvider()
        val request = PaymentRequest("sale-1", 2500, "same-key")
        assertEquals(provider.authorize(request), provider.authorize(request))
    }

    @Test fun invalidAmountIsDeclined() = runBlocking {
        assertFalse(SandboxPaymentProvider().authorize(PaymentRequest("sale-1", 0, "key")).approved)
    }
}
