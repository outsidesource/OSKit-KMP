package com.outsidesource.oskitkmp.lib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class KmpUrlTest {
    @Test
    fun testKmpUrl() {
        assertFails { KmpUrl.fromString("%%") }
        assertNull(KmpUrl.fromStringOrNull("%%"))

        val testHttp = KmpUrl.fromString("https://example.com/one/two/three?test1=Hello%20World!&test2=two#fragmentTest")
        assertEquals("https", testHttp.scheme)
        assertEquals("example.com", testHttp.host)
        assertEquals("/one/two/three", testHttp.path)
        assertEquals("test1=Hello%20World!&test2=two", testHttp.query)
        assertEquals("fragmentTest", testHttp.fragment)
        assertEquals(mapOf("test1" to "Hello World!", "test2" to "two"), testHttp.queryParameters)
        assertEquals(null, testHttp.port)

        val testSsh = KmpUrl.fromString("ssh://root:foo@example.com:21")
        assertEquals("ssh", testSsh.scheme)
        assertEquals("root", testSsh.username)
        assertEquals("foo", testSsh.password)
        assertEquals("example.com", testSsh.host)
        assertEquals("", testSsh.path)
        assertEquals(21, testSsh.port)
    }
}