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
* An easy-to-use, concurrent, reactive, state management system
* Platform independent routing
* Platform independent non-sandboxed file handling
* A typed result type for better railway oriented programming
* Some general helpers and extensions we have found helpful over time

## Supported Platforms
Currently supported platforms include:
* Android
* JVM (MacOS/Windows/Linux)
* iOS

## Installation
```
implementation("com.outsidesource:oskit-kmp:4.3.0")
```

## Example App
<https://github.com/outsidesource/OSKit-Example-App-KMP>

## Changelog

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
* KMPStorage
* Double.toFixed()
* List extensions
#### Breaking Changes
* renamed `Outcome.unwrapOrElse` to `Outcome.unwrapOrReturn` 

### 4.3.0 - 2024-02-16
#### Added
* `createInteractor()`