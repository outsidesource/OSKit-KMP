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

    fun test(): Outcome<Int, Throwable> {
        return try {
            Outcome.Ok(1)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }
}