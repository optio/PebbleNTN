package com.pebblentn.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * M0 smoke test: proves the JVM unit-test toolchain (JUnit + Gradle `test` task) is wired.
 * Real behavior tests arrive with the domain model in M1.
 */
class BuildSmokeTest {
    @Test
    fun jvmTestToolchainRuns() {
        assertEquals(4, 2 + 2)
    }
}
