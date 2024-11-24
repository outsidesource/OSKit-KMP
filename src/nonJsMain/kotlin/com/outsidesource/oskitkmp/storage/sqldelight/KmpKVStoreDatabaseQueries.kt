package com.outsidesource.oskitkmp.storage.sqldelight

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.ByteArray
import kotlin.Long
import kotlin.String

public class KmpKVStoreDatabaseQueries(
    driver: SqlDriver,
) : TransacterImpl(driver) {
    public fun getKeys(): Query<String> = Query(
        937_429_086,
        arrayOf("kmp_storage"),
        driver,
        "KmpKVStoreDatabase.sq",
        "getKeys",
        "SELECT key FROM kmp_storage",
    ) { cursor ->
        cursor.getString(0)!!
    }

    public fun getKeyCount(): Query<Long> = Query(
        -1_151_829_158,
        arrayOf("kmp_storage"),
        driver,
        "KmpKVStoreDatabase.sq",
        "getKeyCount",
        "SELECT COUNT(*) FROM kmp_storage",
    ) { cursor ->
        cursor.getLong(0)!!
    }

    public fun exists(key: String): Query<String> = ExistsQuery(key) { cursor ->
        cursor.getString(0)!!
    }

    public fun `get`(key: String): Query<ByteArray> = GetQuery(key) { cursor ->
        cursor.getBytes(0)!!
    }

    public fun put(key: String, value_: ByteArray) {
        driver.execute(
            -1_498_464_509,
            """INSERT OR REPLACE INTO kmp_storage(key, value) VALUES (?, ?)""",
            2,
        ) {
            bindString(0, key)
            bindBytes(1, value_)
        }
        notifyQueries(-1_498_464_509) { emit ->
            emit("kmp_storage")
        }
    }

    public fun remove(key: String) {
        driver.execute(1_176_270_864, """DELETE FROM kmp_storage WHERE key = ?""", 1) {
            bindString(0, key)
        }
        notifyQueries(1_176_270_864) { emit ->
            emit("kmp_storage")
        }
    }

    public fun clear() {
        driver.execute(-1_222_634_175, """DELETE FROM kmp_storage""", 0)
        notifyQueries(-1_222_634_175) { emit ->
            emit("kmp_storage")
        }
    }

    public fun vacuum() {
        driver.execute(1_286_801_217, """VACUUM""", 0)
    }

    private inner class ExistsQuery<out T : Any>(
        public val key: String,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {
        override fun addListener(listener: Listener) {
            driver.addListener("kmp_storage", listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener("kmp_storage", listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
            driver.executeQuery(821_523_432, """SELECT key FROM kmp_storage WHERE key = ?""", mapper, 1) {
                bindString(0, key)
            }

        override fun toString(): String = "KmpKVStoreDatabase.sq:exists"
    }

    private inner class GetQuery<out T : Any>(
        public val key: String,
        mapper: (SqlCursor) -> T,
    ) : Query<T>(mapper) {
        override fun addListener(listener: Listener) {
            driver.addListener("kmp_storage", listener = listener)
        }

        override fun removeListener(listener: Listener) {
            driver.removeListener("kmp_storage", listener = listener)
        }

        override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
            driver.executeQuery(
                -1_498_473_654,
                """SELECT value FROM kmp_storage WHERE key = ?""",
                mapper,
                1,
            ) {
                bindString(0, key)
            }

        override fun toString(): String = "KmpKVStoreDatabase.sq:get"
    }
}
