package com.outsidesource.oskitkmp.lib

import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

internal object ObjCJna {
    private val oskitRunnerTasks = ConcurrentHashMap<Long, Runnable>()
    private val objc = NativeLibrary.getInstance("objc")

    private val objc_msgSend = func("objc_msgSend")
    private val class_addMethod = func("class_addMethod")
    private val objc_getClass = func("objc_getClass")
    private val sel_registerName = func("sel_registerName")
    private val objc_allocateClassPair = func("objc_allocateClassPair")
    private val objc_registerClassPair = func("objc_registerClassPair")

    private val NSObject = cls("NSObject")
    private val NSAutoreleasePool = cls("NSAutoreleasePool")
    private val NSString = cls("NSString")
    private val NSArray = cls("NSArray")
    private val NSURL = cls("NSURL")
    private val NSNumber = cls("NSNumber")
    private val OSKitRunner: Pointer by lazy {
        val name = "OSKitRunner_${System.identityHashCode(this)}"
        val runnerClass = objc_allocateClassPair.invokePointer(arrayOf(NSObject, name, 0))!!
        val callback = ObjcJNACallback { self, _, _ ->
            try {
                oskitRunnerTasks.remove(Pointer.nativeValue(self))?.run()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        class_addMethod.invokeInt(arrayOf(runnerClass, run, callback, "v@:@"))
        objc_registerClassPair.invokeVoid(arrayOf(runnerClass))
        runnerClass
    }

    private val performOnMain = sel("performSelectorOnMainThread:withObject:waitUntilDone:")
    private val new = sel("new")
    private val run = sel("run:")
    private val alloc = sel("alloc")
    private val init = sel("init")
    private val drain = sel("drain")
    private val stringWithUTF8String = sel("stringWithUTF8String:")
    private val arrayWithObjects_count = sel("arrayWithObjects:count:")
    private val fileURLWithPath = sel("fileURLWithPath:")
    private val numberWithInt = sel("numberWithInt:")
    private val numberWithLong = sel("numberWithLong:")
    private val intValue = sel("intValue")
    private val longValue = sel("longValue")

    private fun interface ObjcJNACallback : Callback {
        fun invoke(self: Pointer?, cmd: Pointer?, arg: Pointer?)
    }

    fun <T> runOnMainThread(block: () -> T): T {
        val runner = OSKitRunner.invokePtr(new)!!
        var result: T? = null
        oskitRunnerTasks[Pointer.nativeValue(runner)] = Runnable { result = block() }
        runner.invokeVoid(performOnMain, run, Pointer.NULL, 1)
        return result!!
    }

    fun <T> withAutoReleasePool(block: () -> T): T {
        val pool = NSAutoreleasePool.invokePtr(alloc)!!.invokePtr(init)!!
        return try {
            block()
        } finally {
            pool.invokeVoid(drain)
        }
    }

    fun func(name: String): Function = objc.getFunction(name)
    fun sel(name: String): Pointer = sel_registerName.invokePointer(arrayOf(name))!!
    fun cls(name: String): Pointer = objc_getClass.invokePointer(arrayOf(name))!!

    fun Pointer.invokePtr(sel: Pointer, vararg args: Any?): Pointer? =
        objc_msgSend.invokePointer(arrayOf(this, sel, *args))
    fun Pointer.invokeLong(sel: Pointer, vararg args: Any?): Long =
        objc_msgSend.invokeLong(arrayOf(this, sel, *args))
    fun Pointer.invokeVoid(sel: Pointer, vararg args: Any?): Unit =
        objc_msgSend.invokeVoid(arrayOf(this, sel, *args))

    fun nsStringFromUtf8(s: String): Pointer {
        val bytes = Native.toByteArray(s, StandardCharsets.UTF_8.name())
        return NSString.invokePtr(stringWithUTF8String, bytes)!!
    }

    fun nsStringArrayFromUtf8Array(items: List<String>): Pointer {
        val nsStrings = items.map { nsStringFromUtf8(it) }.toTypedArray()
        return NSArray.invokePtr(arrayWithObjects_count, nsStrings, nsStrings.size)!!
    }

    fun nsUrlFileURLFromPath(path: String): Pointer {
        val nsPath = nsStringFromUtf8(path)
        return NSURL.invokePtr(fileURLWithPath, nsPath)!!
    }

    fun nsArrayOf(vararg obj: Pointer): Pointer = NSArray.invokePtr(arrayWithObjects_count, obj, obj.size)!!
    fun nsNumberFromInt(value: Int) = NSNumber.invokePtr(numberWithInt, value)
    fun nsNumberFromLong(value: Long) = NSNumber.invokePtr(numberWithLong, value)
    fun nsNumberToInt(value: Pointer): Int = value.invokeLong(intValue).toInt()
    fun nsNumberToLong(value: Pointer): Long = value.invokeLong(longValue)
}
