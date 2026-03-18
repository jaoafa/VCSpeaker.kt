package com.jaoafa.vcspeaker.database

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import kotlin.reflect.full.primaryConstructor

/**
 * 型安全な ResultRow を表す抽象クラス。
 *
 * @property row ResultRow
 */
abstract class TypedRow(val row: ResultRow, val table: Table) {
    val columnMap = mutableMapOf<String, Any?>()

    protected fun <T> column(col: Column<T>): T {
        columnMap[col.name] = row[col]
        return row[col]
    }

    override fun toString() =
        table.tableName.uppercase() + "(${columnMap.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
}

/**
 * ResultRow から与えられた TypedRow を初期化します。
 *
 * @param T TypedRow を継承したテーブルに対応するクラス
 */
inline fun <reified T : TypedRow> ResultRow.toTyped(): T {
    val constructor = T::class.primaryConstructor
        ?: throw IllegalArgumentException("Class ${T::class} must have a primary constructor")
    return constructor.call(this)
}