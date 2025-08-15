# OSKit KMP
An opinionated architecture/library for Kotlin Multiplatform development with an implementation of the [VISCE architecture](https://ryanmitchener.notion.site/VISCE-va-s-Architecture-d0878313b4154d2999bf3bf36cb072ff)

## Abstract
OSKit is primarily a tool for us here at Outside Source. That being said, feel free to use this library in your own code. 
We strive to adhere to semantic versioning.

## Contributions
Contributions are appreciated and welcome, but we are a small team and make no guarantees that your changes will be
implemented.

## Documentation
<https://outsidesource.github.io/OSKit-KMP/>

## Features
* `Interactor` An easy-to-use, concurrent, and reactive state management system
* `Router` Platform independent routing with backstack management, deep link, and transition support
* `KmpFs` Platform independent library for sandboxed and non-sandboxed file system interactions
* `KmpCapabilities` Platform independent permissions and service enablement for common capabilities (Bluetooth, Location)
* `Outcome` A result type with helpers for better railway oriented programming
* Some general helpers and extensions we have found helpful over time

## Supported Platforms
Currently supported platforms include:
* Android
* JVM (MacOS/Windows/Linux)
* iOS
* WASM (browser primarily)

## Installation
```
implementation("com.outsidesource:oskit-kmp:5.0.0")
```

## Example App
<https://github.com/outsidesource/OSKit-Example-App-KMP>

## Changelog
### 5.1.0 - 2025-08-15
#### Added
* Kotlin 2.2.0 support
* RcCoroutine
* KmpFs no longer depends on lwjgl for file pickers on the JVM
* ByteArray.find
* Collections.mapValuesNotNull
* EnumSerializer for kotlinx.serialization allowing for deserialization into a default enum value
* measureTimePrinted
* ValueCache
* Outcome.guard allows for syntactic sugar for unwrapOrReturn. `val result = outcome guard { return it }`
* String.parseFloatOrNull()
#### Breaking Changes
* Renamed NumberFormatter to KmpNumberFormatter

### 5.0.0 - 2025-02-07
#### Added
* Kotlin 2.1.0 support
* Support for the WASM target
  * All existing and new OsKit feature support all platforms unless explicitly said otherwise in documentation
  * Added some `Promise` helpers
  * Added some `JsInterop` helpers
* `KmpCapabilities` for testing for permissions and enablement of some common platform services (Bluetooth, Location)
* `KmpFs` Supports internal (sandboxed) and external (non-sandboxed) filesystem interactions
* `IKmpIoSource` and `IKmpIoSink` interfaces and implementations for cross-platform asynchronous file interactions.
* `KmpScreenWakeLock` Allows preventing a user's screen from sleeping
* `KmpDispatchers` for a common `IO` dispatcher
* `Deferred<t>.awaitOutcome()`
* `Queue` for creating a queue of sequentially executing coroutines
* `BytesExt` Allows converting common data types to and from byte arrays
* `LocalDateTime.kmpFormat()` For a cross-platform date-time formatter
* `KmpUrl` A multiplatform URL parser
* `Any?.printed()` and `printAll` helpers
* Updated `Platform` to add `WebBrowser`
* `Router` has been reworked and added a new transactional API for more flexibility and to clean up a lot of rarely used API surface
  * Added `IWebRoute` for handling path changes in the browser
  * Added deep link support
#### Breaking Changes
* Adopted Upper Camel Case for all acronym prefixes on class and function names (i.e. `KMP` changed to `Kmp`)
* Reworked/renamed `KMPStorage` to `KmpKvStore`
* `Coordinator` has been reworked to fit the new Transaction API implemented in Router.
* `KmpDeepLink` was deemed unnecessary and was removed
* `KMPFileHandler` was renamed/repurposed into `KmpFs`
* `KMPFileRef` was renamed/repurposed into `KmpFsRef`
* `LazyComputed` was changed to use the invoke operator instead of `value()` function
* `Outcome.unwrapOrReturn()` was changed to pass a parameter instead of `this` for the error
* Reworked the `Router` API
* `KMPStorage` was renamed/reworked into `KmpKvStore`
* `FileUtil` was renamed to `FsUtil`

### 4.6.0 - 2024-05-21
#### Added
* InMemoryKmpStorageNode
* withTimeoutOrOutcome
* Deferrable code helpers
  * Deferrer/SuspendDeferrer
  * withDefer/withSuspendDefer
  * coroutineScopeWithDefer/coroutineScopeWithSuspendDefer
  * flowWithDefer/flowWithSuspendDefer
  * channelFlowWithDefer/channelFlowWithSuspendDefer
#### Updated
* Default parameters in Coordinator
#### Fixed
* KmpFileRef being broken after reboot on iOS
* Large iOS KmpFileRef sink() writes 
* Issue when resolving KmpFileRef on desktop
* Issue with resolving KmpFileRef directory on Linux
* Issue with file pickers on Linux with Plasma

### 4.5.0 - 2024-03-23
#### Updated
* Outcome.runOnOk and Outcome.runOnError now return the outcome specified 

### 4.4.2 - 2024-03-18
#### Fixed
* SemVer.fromString() only taking the first digit
#### Updated
* Made dependencies and computed optional in createInteractor()

### 4.4.0 - 2024-03-06
#### Added
* KmpStorage
* Double.toFixed()
* List extensions
#### Breaking Changes
* renamed `Outcome.unwrapOrElse` to `Outcome.unwrapOrReturn` 

### 4.3.0 - 2024-02-16
#### Added
* `createInteractor()`