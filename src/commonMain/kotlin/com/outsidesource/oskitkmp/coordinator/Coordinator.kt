package com.outsidesource.oskitkmp.coordinator

import com.outsidesource.oskitkmp.router.*

abstract class Coordinator(
    initialRoute: IRoute,
    defaultTransition: IRouteTransition = object : IRouteTransition {}
) : IRouter by Router(initialRoute, defaultTransition)
