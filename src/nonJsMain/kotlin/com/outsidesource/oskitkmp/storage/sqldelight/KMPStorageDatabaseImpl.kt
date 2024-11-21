package com.outsidesource.oskitkmp.storage.sqldelight

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<KMPStorageDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
    get() = KMPStorageDatabaseImpl.Schema

internal fun KClass<KMPStorageDatabase>.newInstance(driver: SqlDriver): KMPStorageDatabase =
    KMPStorageDatabaseImpl(driver)

private class KMPStorageDatabaseImpl(
    driver: SqlDriver,
) : TransacterImpl(driver), KMPStorageDatabase {
    override val kMPStorageDatabaseQueries: KMPStorageDatabaseQueries =
        KMPStorageDatabaseQueries(driver)

    public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long
            get() = 1

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(
                null,
                """
          |CREATE TABLE IF NOT EXISTS kmp_storage (
          |    key TEXT PRIMARY KEY NOT NULL,
          |    value BLOB NOT NULL
          |)
                """.trimMargin(),
                0,
            )
            return QueryResult.Unit
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ): QueryResult.Value<Unit> = QueryResult.Unit
    }
}
