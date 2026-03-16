package com.jaoafa.vcspeaker.database

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.full.primaryConstructor

/**
 * 型安全な ResultRow を表す抽象クラス。
 *
 * @property row ResultRow
 */
abstract class TypedRow(val row: ResultRow, val table: Table) {
    protected fun <T> column(col: Column<T>) = ReadOnlyProperty<TypedRow, T> { _, _ -> row[col] }
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