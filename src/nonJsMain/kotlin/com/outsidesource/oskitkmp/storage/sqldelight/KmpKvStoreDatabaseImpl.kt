package com.outsidesource.oskitkmp.storage.sqldelight

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<KmpKvStoreDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
    get() = KmpKvStoreDatabaseImpl.Schema

internal fun KClass<KmpKvStoreDatabase>.newInstance(driver: SqlDriver): KmpKvStoreDatabase =
    KmpKvStoreDatabaseImpl(driver)

private class KmpKvStoreDatabaseImpl(
    driver: SqlDriver,
) : TransacterImpl(driver), KmpKvStoreDatabase {
    override val kmpKvStoreDatabaseQueries: KmpKvStoreDatabaseQueries =
        KmpKvStoreDatabaseQueries(driver)

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
