package com.jaoafa.vcspeaker.database

import org.h2.api.ErrorCode.DUPLICATE_KEY_1
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager

sealed class TransactionResult<out T> {
    data class Success<T>(val value: T) : TransactionResult<T>()
    data class Duplicate(val exception: ExposedSQLException) : TransactionResult<Nothing>()
    data class Failure(val exception: Exception) : TransactionResult<Nothing>()
}

inline fun <T> transactionResulting(
    db: Database? = null,
    transactionIsolation: Int? = db?.transactionManager?.defaultIsolationLevel,
    readOnly: Boolean? = db?.transactionManager?.defaultReadOnly,
    commit: Boolean = false,
    crossinline statement: JdbcTransaction.() -> T
): TransactionResult<T> {
    return try {
        transaction(db, transactionIsolation, readOnly) {
            val result = statement()
            if (commit) commit()
            TransactionResult.Success(result)
        }
    } catch (e: ExposedSQLException) {
        when (e.sqlState.toInt()) {
            DUPLICATE_KEY_1 -> {
                TransactionResult.Duplicate(e)
            }

            else -> {
                TransactionResult.Failure(e)
            }
        }
    }
}

suspend inline fun <T> suspendTransactionResulting(
    db: Database? = null,
    transactionIsolation: Int? = db?.transactionManager?.defaultIsolationLevel,
    readOnly: Boolean? = db?.transactionManager?.defaultReadOnly,
    commit: Boolean = false,
    crossinline statement: suspend JdbcTransaction.() -> T
): TransactionResult<T> {
    return try {
        suspendTransaction(db, transactionIsolation, readOnly) {
            val result = statement()
            if (commit) commit()
            TransactionResult.Success(result)
        }
    } catch (e: ExposedSQLException) {
        when (e.sqlState.toInt()) {
            DUPLICATE_KEY_1 -> {
                TransactionResult.Duplicate(e)
            }

            else -> {
                TransactionResult.Failure(e)
            }
        }
    }
}

inline fun <T> TransactionResult<T>.onSuccess(action: (T) -> Unit): TransactionResult<T> {
    if (this is TransactionResult.Success) {
        action(value)
    }
    return this
}

inline fun <T> TransactionResult<T>.onDuplicate(action: (ExposedSQLException) -> Unit): TransactionResult<T> {
    if (this is TransactionResult.Duplicate) {
        action(exception)
    }
    return this
}

inline fun <T> TransactionResult<T>.onFailure(action: (Exception) -> Unit): TransactionResult<T> {
    if (this is TransactionResult.Failure) {
        action(exception)
    }
    return this
}

fun <T> TransactionResult<T>.unwrapOrNull(): T? {
    return if (this is TransactionResult.Success) {
        this.value
    } else {
        null
    }
}

fun <T> TransactionResult<T>.unwrap(): T {
    when (this) {
        is TransactionResult.Success -> return this.value
        is TransactionResult.Duplicate -> throw this.exception
        is TransactionResult.Failure -> throw this.exception
    }
}