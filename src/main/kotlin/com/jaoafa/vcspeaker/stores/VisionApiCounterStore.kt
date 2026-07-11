package com.jaoafa.vcspeaker.stores

import com.jaoafa.vcspeaker.VCSpeaker
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class VisionApiCounterData(
    /** 年月 (yyyy/MM) */
    val yearMonth: String,
    /** リクエスト数 */
    val count: Int,
    /** リミット到達日時 */
    val limitReachedAt: Long? = null
)

object VisionApiCounterStore : StoreStruct<VisionApiCounterData>(
    VCSpeaker.Files.visionApiCounter.path,
    VisionApiCounterData.serializer(),
    { Json.decodeFromString(this) },

    version = 1,
    migrators = mapOf(
        1 to { file ->
            val list = Json.decodeFromString<List<VisionApiCounterData>>(file.readText())
            file.writeText(
                Json.encodeToString(
                    TypedStore.serializer(VisionApiCounterData.serializer()),
                    TypedStore(1, list)
                )
            )
        }
    )
) {
    // 月当たり 1000 units だけど余裕をもって
    const val VISION_API_LIMIT = 950

    /**
     * 今月のリクエスト数を取得する
     */
    suspend fun get() = get(getNowYearMonth())

    /**
     * 年/月ごとのリクエスト数を取得する
     */
    suspend fun get(yearMonth: String) = withData { data.find { it.yearMonth == yearMonth } }

    /** 今月のリクエスト数が上限に達しているか */
    suspend fun isLimitExceeded() = (get()?.count ?: 0) >= VISION_API_LIMIT

    /** リクエスト数をインクリメントする */
    suspend fun increment() = withData {
        val yearMonth = getNowYearMonth()
        val index = data.indexOfFirst { it.yearMonth == yearMonth }

        val counter = if (index != -1) {
            data[index] = data[index].copy(count = data[index].count + 1)
            data[index]
        } else {
            VisionApiCounterData(yearMonth, 1).also { data.add(it) }
        }

        // リクエスト数を超えていたらリミット到達日時を記録する
        if (counter.count >= VISION_API_LIMIT) {
            val limitIndex = data.indexOfFirst { it.yearMonth == yearMonth }
            data[limitIndex] = data[limitIndex].copy(limitReachedAt = System.currentTimeMillis())
        }

        writeLocked()
    }

    /** 今月の年/月を取得する */
    private fun getNowYearMonth() = SimpleDateFormat("yyyy/MM").format(Date())
}

