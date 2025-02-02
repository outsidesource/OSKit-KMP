package com.outsidesource.oskitkmp.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouterDeepLinkTrieTest {
    @Test
    fun testDeepLinkTrie() {
        val deepLinks = RouterDeepLinkTrie {
            "/static/one/two/three" routesTo { _, _ -> Route.Static }
            "/dynamic/:one/:two/:three" routesTo { args, _ -> Route.Dynamic1(args[0], args[1], args[2]) }
            "/dynamic/:one/two/:three" routesTo { args, _ -> Route.Dynamic2(args[0], args[1]) }
        }

        val static = deepLinks.matchRoute("/static/one/two/three")
        assertEquals(Route.Static, static)

        val staticTrailingSlash = deepLinks.matchRoute("/static/one/two/three/")
        assertEquals(Route.Static, staticTrailingSlash, "Trailing Slash")

        val dynamic1 = deepLinks.matchRoute("/dynamic/1/2/3")
        assertEquals(Route.Dynamic1("1", "2", "3"), dynamic1)

        val dynamic1TrailingSlash = deepLinks.matchRoute("/dynamic/1/2/3/")
        assertEquals(Route.Dynamic1("1", "2", "3"), dynamic1TrailingSlash)

        val dynamic2 = deepLinks.matchRoute("/dynamic/1/two/3")
        assertEquals(Route.Dynamic2("1", "3"), dynamic2)

        assertNull(deepLinks.matchRoute(""))
        assertNull(deepLinks.matchRoute("/"))
        assertNull(deepLinks.matchRoute("/static/one/two/"))
        assertNull(deepLinks.matchRoute("/dynamic"))
        assertNull(deepLinks.matchRoute("/dynamic/1/2"))
        assertNull(deepLinks.matchRoute("/dynamic/1/two"))
    }

    private sealed class Route: IRoute {
        data object Static : Route()
        data class Dynamic1(val one: String, val two: String, val three: String) : Route()
        data class Dynamic2(val one: String, val two: String) : Route()
    }
}