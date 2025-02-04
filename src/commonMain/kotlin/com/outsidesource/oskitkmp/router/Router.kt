package com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

/**
 * [Router] the primary implementation of IRouter
 */
class Router(
    initialRoute: IRoute,
    internal val defaultTransition: IRouteTransition = DefaultTransition,
) : IRouter {
    override val routeFlow: MutableStateFlow<RouteStackEntry>
    override val current get() = _routeStack.value.last()
    override val routeStack: List<RouteStackEntry> get() = _routeStack.value

    private val _routeStack: AtomicRef<List<RouteStackEntry>>
    private val routeLifecycleListeners = atomic(mapOf<Int, List<IRouteLifecycleListener>>())
    private var transitionStatus: RouteTransitionStatus by atomic(RouteTransitionStatus.Idle)
    private val onRouteDestroyedTransitionCompletedCallbacks = atomic<List<() -> Unit>>(emptyList())
    private val routeResults = atomic(mapOf<Int, CompletableDeferred<Any>>())

    internal var routerListener: IRouterListener? = null

    companion object {
        fun buildDeepLinks(builder: IRouterDeepLinkTrieBuilder.() -> Unit) = RouterDeepLinkTrie(builder)
    }

    init {
        val initialStackEntry = RouteStackEntry(initialRoute)
        _routeStack = atomic(listOf(initialStackEntry))
        routeFlow = MutableStateFlow(initialStackEntry)
        initForPlatform(this)
    }

    override fun push(route: IRoute, transition: IRouteTransition?, ignoreTransitionLock: Boolean) =
        transaction(ignoreTransitionLock) { push(route, transition) }

    override fun replace(route: IRoute, transition: IRouteTransition?, ignoreTransitionLock: Boolean) =
        transaction(ignoreTransitionLock) { replace(route, transition) }

    internal fun push(route: RouteStackEntry, ignoreTransitionLock: Boolean) =
        transaction(ignoreTransitionLock) { (this as? RouterTransactionScope)?.push(route) }

    override fun pop(ignoreTransitionLock: Boolean, popFunc: RoutePopFunc) =
        transaction(ignoreTransitionLock) { pop(popFunc) }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> transactionWithResult(
        resultType: KClass<T>,
        ignoreTransitionLock: Boolean,
        transaction: IRouterTransactionScope.() -> Unit,
    ): Outcome<T, RouteResultError> {
        val result = CompletableDeferred<Any>()

        transaction(ignoreTransitionLock) {
            transaction()

            if (this !is RouterTransactionScope) return@transaction
            routeResults.update { it.toMutableMap().apply { this[routeStack.last().id] = result } }
        }

        return try {
            val result = result.await()
            when {
                result::class == resultType -> Outcome.Ok(result as T)
                result is RouteResultError -> Outcome.Error(result)
                else -> Outcome.Error(RouteResultError.UnexpectedResultType(result))
            }
        } catch (t: Throwable) {
            Outcome.Error(RouteResultError.Unknown(t))
        }
    }

    override fun transaction(ignoreTransitionLock: Boolean, transaction: IRouterTransactionScope.() -> Unit) {
        if (transitionStatus == RouteTransitionStatus.Running && !ignoreTransitionLock) return

        val scope = RouterTransactionScope(this).apply { transaction() }
        _routeStack.value = scope.routeStack

        val top = _routeStack.value.last()
        routeFlow.value = top
        routeLifecycleListeners.value[top.id]?.forEach { it.onRouteStarted() }
    }

    override fun hasBackStack(): Boolean = _routeStack.value.size > 1

    override fun markTransitionStatus(status: RouteTransitionStatus) {
        val previousStatus = transitionStatus
        transitionStatus = status

        if (status == RouteTransitionStatus.Idle && previousStatus == RouteTransitionStatus.Running) {
            val routeDestroyedTransitionCompleteCallbacks = onRouteDestroyedTransitionCompletedCallbacks.value
            onRouteDestroyedTransitionCompletedCallbacks.value = emptyList()

            for (callback in routeDestroyedTransitionCompleteCallbacks) {
                callback()
            }

            val top = _routeStack.value.last()
            routeLifecycleListeners.value[top.id]?.forEach { it.onRouteEnterTransitionComplete() }
        }
    }

    override fun addRouteLifecycleListener(listener: IRouteLifecycleListener) {
        val route = current
        listener.onRouteCreated()
        listener.onRouteStarted()
        routeLifecycleListeners.update {
            it.toMutableMap().apply {
                put(route.id, (this[route.id] ?: emptyList()) + listener)
            }
        }
    }

    override fun tearDown() = tearDownForPlatform(this)

    internal fun onRouteStopped(route: RouteStackEntry) {
        routeLifecycleListeners.value[route.id]?.forEach { listener -> listener.onRouteStopped() }
    }

    internal fun onRouteDestroyed(route: RouteStackEntry) {
        val result = routeResults.value[route.id]
        if (result != null && !result.isCompleted) routeResults.value[route.id]?.complete(RouteResultError.Cancelled)
        routeResults.update { it.toMutableMap().apply { remove(route.id) } }

        routeLifecycleListeners.value[route.id]?.forEach { listener ->
            onRouteDestroyedTransitionCompletedCallbacks.update { it + listener::onRouteDestroyedTransitionComplete }
            listener.onRouteDestroyed()
        }
        routeLifecycleListeners.update { it.toMutableMap().apply { remove(route.id) } }
    }

    internal fun onRouteResult(route: RouteStackEntry, result: Any) {
        routeResults.value[route.id]?.complete(result)
    }
}

internal interface IRouterListener {
    fun onPush(entry: RouteStackEntry)
    fun onPop(entry: RouteStackEntry?)
}

private class RouterTransactionScope(private val router: Router) : IRouterTransactionScope {

    var routeStack: List<RouteStackEntry> = router.routeStack.toList()
    private val popScope = PopScope(router, this)
    private var hasStoppedTop = false

    override fun replace(route: IRoute, transition: IRouteTransition?) {
        destroyTopStackEntry()
        push(route, transition)
    }

    override fun push(route: IRoute, transition: IRouteTransition?) {
        stopTopRoute(routeStack.lastOrNull())
        val entry = RouteStackEntry(
            route = route,
            transition = transition ?: if (route is IAnimatedRoute) {
                route.animatedRouteTransition
            } else {
                router.defaultTransition
            },
        )
        routeStack += entry
        router.routerListener?.onPush(routeStack.last())
    }

    override fun pop(popFunc: RoutePopFunc) {
        val popPredicate = popScope.popFunc()
        while (routeStack.size > 1) {
            if (!popPredicate(routeStack.last().route)) return
            destroyTopStackEntry()
        }
    }

    fun push(entry: RouteStackEntry) {
        stopTopRoute(routeStack.last())
        routeStack += entry
    }

    private fun destroyTopStackEntry() {
        val top = routeStack.last()
        routeStack -= top
        stopTopRoute(top)
        router.onRouteDestroyed(top)
        router.routerListener?.onPop(routeStack.lastOrNull())
    }

    private fun stopTopRoute(route: RouteStackEntry?) {
        if (route == null) return
        if (hasStoppedTop) return
        hasStoppedTop = true
        router.onRouteStopped(route)
    }
}

private class PopScope(
    private val router: Router,
    private val transactionScope: RouterTransactionScope,
) : IRoutePopScope {

    override fun withResult(result: Any): (IRoute) -> Boolean {
        var breakNext = false

        return fun(_: IRoute): Boolean {
            if (!breakNext) {
                breakNext = true
                router.onRouteResult(transactionScope.routeStack.last(), result)
                return true
            }
            return false
        }
    }
}

internal expect fun initForPlatform(router: Router)
internal expect fun tearDownForPlatform(router: Router)

// Kotlin 2.1.0 has an issue with an anonymous object being created in a class constructor on iOS preventing compilation
// of any project using OSKit-KMP
internal val DefaultTransition = object : IRouteTransition {}
