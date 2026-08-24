package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.protocol.CrashReportItem
import dev.hawk0f.checkmates.shared.protocol.CrashReportRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CrashRepository(private val database: Database) {

    suspend fun record(request: CrashReportRequest, nowMillis: Long = System.currentTimeMillis()): Long = dbQuery {
        CrashReports.insert {
            it[platform] = request.platform.take(16)
            it[appVersion] = request.appVersion.take(32)
            it[osVersion] = request.osVersion.take(64)
            it[stackTrace] = request.stackTrace.take(MAX_TRACE_CHARS)
            it[occurredAtMillis] = request.occurredAtMillis
            it[receivedAtMillis] = nowMillis
        } get CrashReports.id
    }

    suspend fun recent(limit: Int = 50): List<CrashReportItem> = dbQuery {
        CrashReports.selectAll()
            .orderBy(CrashReports.receivedAtMillis, SortOrder.DESC)
            .limit(limit.coerceIn(1, 200))
            .map { row ->
                CrashReportItem(
                    id = row[CrashReports.id],
                    platform = row[CrashReports.platform],
                    appVersion = row[CrashReports.appVersion],
                    osVersion = row[CrashReports.osVersion],
                    stackTrace = row[CrashReports.stackTrace],
                    occurredAtMillis = row[CrashReports.occurredAtMillis],
                    receivedAtMillis = row[CrashReports.receivedAtMillis]
                )
            }
    }

    suspend fun count(): Long = dbQuery { CrashReports.selectAll().count() }

    suspend fun purgeOlderThan(nowMillis: Long = System.currentTimeMillis()): Int = dbQuery {
        CrashReports.deleteWhere { receivedAtMillis lessEq nowMillis - RETENTION_MILLIS }
    }

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }

    companion object {
        const val MAX_TRACE_CHARS = 8_000
        const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
