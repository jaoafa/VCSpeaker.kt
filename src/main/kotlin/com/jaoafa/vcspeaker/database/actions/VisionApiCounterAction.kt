package com.jaoafa.vcspeaker.database.actions

import com.jaoafa.vcspeaker.database.onSuccess
import com.jaoafa.vcspeaker.database.transactionResulting
import com.jaoafa.vcspeaker.database.unwrap
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import kotlin.time.Clock
import com.jaoafa.vcspeaker.database.tables.VisionAPICounterEntity as Entity
import com.jaoafa.vcspeaker.database.tables.VisionAPICounterTable as Table

object VisionApiCounterAction {
    private val logger = KotlinLogging.logger { }

    const val VISION_API_LIMIT = 950

    private fun getIdOf(year: Int, month: Int) = CompositeID {
        it[Table.year] = year
        it[Table.month] = month
    }

    private fun fetchEntity(year: Int, month: Int) = transaction {
        val id = getIdOf(year, month)
        return@transaction Entity.findById(id)
    }

    fun fetch(year: Int, month: Int) = fetchEntity(year, month)?.fetchSnapshot()


    fun fetchCurrent() = with(LocalDate.now()) {
        fetch(year, monthValue)
    }

    fun increment() = transactionResulting {
        val today = LocalDate.now()
        val entity = fetchEntity(today.year, today.monthValue) ?: run {
            Entity.new(getIdOf(today.year, today.monthValue)) {
                count = 0
            }
        }

        val currentCount = entity.count

        if ((currentCount + 1) >= VISION_API_LIMIT) {
            entity.limitReachedAt = Clock.System.now()
        }

        entity.count += 1

        commit()

        entity.fetchSnapshot()
    }.onSuccess {
        logger.info { "Vision API Counter Incremented: ${it.describe()}" }
    }.unwrap()
}
