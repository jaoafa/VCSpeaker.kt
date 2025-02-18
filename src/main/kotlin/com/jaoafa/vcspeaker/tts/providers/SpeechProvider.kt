package com.jaoafa.vcspeaker.tts.providers

import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiContext
import com.jaoafa.vcspeaker.tts.providers.soundmoji.SoundmojiProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextProvider
import com.jaoafa.vcspeaker.tts.providers.voicetext.VoiceTextContext

/**
 * 音声データを提供するプロバイダーを表すインターフェースです。
 *
 * @param T [ProviderContext] の型
 * @property id プロバイダー ID; 一意である必要があります。
 * @property format 音声データのフォーマット
 */
interface SpeechProvider<T : ProviderContext> {
    val id: String
    val format: String

    /**
     * 与えられた [ProviderContext] に対応する音声データを提供します。
     *
     * @param context [ProviderContext] のインスタンス
     * @return 音声データ
     */
    suspend fun provide(context: T): ByteArray
}

/**
 * 音声データの生成・取得に必要な情報を表すインターフェースです。
 * ProviderContext によって取得される音声は、必ず一意である必要があります。(Cache に使用するため)
 */
interface ProviderContext {
    /**
     * このコンテキストの説明を返します。
     *
     * @return 説明
     */
    fun describe(): String

    /**
     * このコンテキストの MD5 ハッシュを返します。
     *
     * @return ハッシュ
     */
    fun hash(): String
}

/**
 * 与えられた [ProviderContext] に対応する [SpeechProvider] を取得します。
 *
 * @param context [ProviderContext] のインスタンス
 * @return [SpeechProvider] のインスタンス
 * @throws IllegalArgumentException [ProviderContext] に対応する [SpeechProvider] が見つからない場合
 */
fun <T : ProviderContext> providerOf(context: T): SpeechProvider<T>? {
    return when (context) {
        is SoundmojiContext -> SoundmojiProvider as SpeechProvider<T>
        is VoiceTextContext -> VoiceTextProvider as SpeechProvider<T>
        else -> null
    }
}

/**
 * 与えられた ID に対応する [SpeechProvider] を取得します。
 *
 * @param id [SpeechProvider] の ID
 * @return [SpeechProvider] のインスタンス
 */
fun getProvider(id: String): SpeechProvider<*>? {
    return when (id) {
        SoundmojiProvider.id -> SoundmojiProvider
        VoiceTextProvider.id -> VoiceTextProvider
        else -> null
    }
}