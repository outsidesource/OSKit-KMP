package com.outsidesource.oskitkmp.outcome

import kotlin.test.Test
import kotlin.test.assertTrue

class OutcomeTest {
    @Test
    fun testOutcome() {
        val outcome: Outcome<Int, Any> = Outcome.Ok(1)
        val outcome2: Outcome<Int, Any> = Outcome.Error(1)

        assertTrue { outcome is Outcome.Ok }
        assertTrue { outcome2 is Outcome.Error }
        assertTrue { outcome2.unwrapOrDefault(3) == 3 }
        assertTrue { outcome2.unwrapOrNull() == null }
    }

    @Test
    fun testTryBlock() {
        val ok = Outcome.tryBlock<Int, Exception> {
            1
        }
        val error = Outcome.tryBlock<Int, Exception> {
            throw Exception("")
        }

        assertTrue { error is Outcome.Error }
        assertTrue { ok is Outcome.Ok && ok.value == 1 }
    }

    @Test
    fun testRunOnOk() {
        val test1 = testOk()
        var didRunOk = false
        var didRunError = false
        test1.runOnOk { didRunOk = true }
        test1.runOnError { didRunError = true }
        assertTrue { didRunOk && !didRunError }
    }

    @Test
    fun testRunOnError() {
        val test1 = testError()
        var didRunOk = false
        var didRunError = false
        test1.runOnOk { didRunOk = true }
        test1.runOnError { didRunError = true }
        assertTrue { !didRunOk && didRunError }
    }

    fun testOk(): Outcome<Int, Throwable> {
        return Outcome.Ok(1)
    }

    fun testError(): Outcome<Int, Throwable> {
        return Outcome.Error(Exception())
    }
}