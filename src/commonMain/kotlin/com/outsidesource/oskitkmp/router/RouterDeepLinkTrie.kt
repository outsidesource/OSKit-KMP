package com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.lib.KmpUrl

/**
 * A Trie for resolving deep-linked URLs to IRoutes
 *
 * Example Declaration:
 * ```
 * sealed class Route: IRoute {
 *     data object Home: Route()
 *     data object StaticDeepLinkTest: Route()
 *     data class DynamicDeepLinkTest(val dynamicValue: String): Route()
 *
 *     companion object {
 *         val deepLinks = Router.buildDeepLinks {
 *             "/static-deep-link" routesTo { _, _ -> StaticDeepLinkTest }
 *             "/dynamic-deep-link/:value" routesTo { args, _ -> DynamicDeepLinkTest(args[0]) }
 *         }
 *     }
 * }
 * ```
 *
 * Example Usage:
 * ```
 * val deepLink = Route.deepLinks.matchRoute(deepLinkUrlFromPlatform)
 * val router = Router(initialRoute = deepLink ?: Route.Home)
 * ```
 */
class RouterDeepLinkTrie(builder: IRouterDeepLinkTrieBuilder.() -> Unit) {

    private val PLACEHOLDER = "__VAR__"
    private val staticDeepLinks: MutableMap<String, RouterDeepLinkMapper> = mutableMapOf()
    private var dynamicLinkTrie: DeepLinkTrieNode = DeepLinkTrieNode()

    init {
        val builderScope = object : IRouterDeepLinkTrieBuilder {
            val routeList = mutableMapOf<String, RouterDeepLinkMapper>()
            override fun String.routesTo(mapper: RouterDeepLinkMapper) { routeList[this] = mapper }
        }

        builderScope.builder()
        builderScope.routeList.forEach { deepLink -> addDeepLink(deepLink.key, deepLink.value) }
    }

    /**
     * Adds a deeplink to the Trie
     *
     * @param pattern The path the deep link should match. Any dynamic path segment can be prefixed with `:` to match
     * any value. i.e. `/devices/:deviceId`. The value for `:deviceId` will be passed in the `args` parameter of
     * [mapper].
     *
     * @param mapper A function that passes in the arguments for any dynamic path segment and a [KmpUrl] and returns an
     * [IRoute].
     */
    fun addDeepLink(pattern: String, mapper: RouterDeepLinkMapper) {
        val path = pattern.trim('/')
        if (path.indexOf(':') == -1) {
            staticDeepLinks[path] = mapper
            return
        }

        val segments = path.split('/')
        var node = dynamicLinkTrie

        for (i in segments.indices) {
            val segment = if (segments[i].first() == ':') PLACEHOLDER else segments[i]

            if (node.children[segment] != null) {
                node = node.children[segment]!!
            } else {
                node.children[segment] = DeepLinkTrieNode(segment = segment)
                node = node.children[segment]!!
            }

            if (i == segments.size - 1) node.mapper = mapper
        }
    }

    /**
     * Returns an [IRoute] if the passed in [url] matches a deep link stored in the trie
     *
     * @param url The URL to match against
     */
    fun matchRoute(url: String): IRoute? {
        val kmpUrl = KmpUrl.fromString(url)
        val path = kmpUrl.path.trim('/')
        staticDeepLinks[path]?.let { return it(emptyList(), kmpUrl) }

        val segments = path.split('/')
        var node = dynamicLinkTrie
        val args = mutableListOf<String>()

        for (i in segments.indices) {
            val segment = segments[i]

            if (node.children[segment] != null) {
                node = node.children[segment]!!
            } else if (node.children[PLACEHOLDER] != null) {
                node = node.children[PLACEHOLDER]!!
                args.add(segment)
            } else {
                return null
            }

            if (i == segments.size - 1) return node.mapper?.invoke(args, kmpUrl)
        }

        return null
    }
}

interface IRouterDeepLinkTrieBuilder {
    infix fun String.routesTo(mapper: RouterDeepLinkMapper)
}

typealias RouterDeepLinkMapper = (pathArgs: List<String>, url: KmpUrl) -> IRoute

private class DeepLinkTrieNode(
    val segment: String = "",
    var mapper: RouterDeepLinkMapper? = null,
    val children: MutableMap<String, DeepLinkTrieNode> = mutableMapOf(),
)
