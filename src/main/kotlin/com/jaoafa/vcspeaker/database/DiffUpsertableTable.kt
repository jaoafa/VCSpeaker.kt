package com.jaoafa.vcspeaker.database

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

/**
 * 拡張関数 diffUpsert() を使用できるテーブル
 */
interface DiffUpsertableTable<T : TypedRow> {
    /**
     * UNIQUE 制約を持つ Column のリスト (プライマリキー以外)
     */
    val uniqueColumns: List<Column<*>>

    /**
     * 与えられた [values] を INSERT する際に UNIQUE 制約に違反する行の条件を返します。
     */
    fun getConflictOp(values: Map<Column<*>, Any?>): Op<Boolean>
}

/**
 * 与えられた値をテーブルに UPSERT し、変更前の変更後の行を返します。
 */
inline fun <reified R : TypedRow, T> T.diffUpsert(
    onUpdateExclude: List<Column<*>>? = null,
    where: Op<Boolean>? = null,
    noinline body: T.(UpdateBuilder<*>) -> Unit,
): Pair<R?, R> where T : Table, T : DiffUpsertableTable<R>, T : VersionedTable {
    val upsertStatement = UpsertStatement<Long>(
        this,
        *uniqueColumns.toTypedArray(),
        onUpdateExclude = onUpdateExclude,
        where = where
    )
    body(upsertStatement) // Applies value changes
    val arguments =
        upsertStatement.arguments ?: throw IllegalStateException("No arguments captured in upsert statement.")

    val old = this
        .selectAll()
        .where { getConflictOp(arguments.first().associate { it.first to it.second }) }
        .singleOrNull()
        ?.toTyped<R>()

    val new = upsert(
        *uniqueColumns.toTypedArray(),
        onUpdate = {
            body(this@diffUpsert, it)
            if (old != null) it[version] = version + 1
        },
        body = {
            body(this, it)
        }
    ).resultedValues?.singleOrNull()?.toTyped<R>()
        ?: throw IllegalStateException("Failed to upsert or retrieve record.")

    return old to new
}