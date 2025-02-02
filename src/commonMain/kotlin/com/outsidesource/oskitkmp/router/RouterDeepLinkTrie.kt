package com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.lib.KmpUrl

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
