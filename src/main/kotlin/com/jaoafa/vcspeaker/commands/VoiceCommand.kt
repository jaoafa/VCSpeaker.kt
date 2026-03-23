package com.jaoafa.vcspeaker.commands

import com.jaoafa.vcspeaker.database.committingTransaction
import com.jaoafa.vcspeaker.database.tables.UserEntity
import com.jaoafa.vcspeaker.database.tables.VoiceEntity
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.EmotionLevelOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.EmotionOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.PitchOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.SpeakerOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.SpeedOption
import com.jaoafa.vcspeaker.features.Voice.CommandOptions.VolumeOption
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.authorOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.respondEmbed
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.successColor
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.voiceParameterFieldsOf
import com.jaoafa.vcspeaker.tools.discord.DiscordExtensions.warningColor
import com.jaoafa.vcspeaker.tools.discord.DiscordLoggingExtension.log
import com.jaoafa.vcspeaker.tools.discord.Options
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSlashCommand
import com.jaoafa.vcspeaker.tools.discord.SlashCommandExtensions.publicSubCommand
import com.jaoafa.vcspeaker.tools.discord.VoiceOptions
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.converters.impl.optionalInt
import dev.kordex.core.extensions.Extension
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class VoiceCommand : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger {}

    class VoiceSetOptions : Options(), VoiceOptions {
        override val speaker by optionalStringChoice(SpeakerOption)
        override val emotion by optionalStringChoice(EmotionOption)
        override val emotionLevel by optionalInt(EmotionLevelOption)
        override val pitch by optionalInt(PitchOption)
        override val speed by optionalInt(SpeedOption)
        override val volume by optionalInt(VolumeOption)
    }

    override suspend fun setup() {
        publicSlashCommand("voice", "自分の声を設定します。") {
            check { anyGuild() }
            publicSubCommand("set", "自分の声を設定します。", ::VoiceSetOptions) {
                action {
                    val userEntity = transaction {
                        UserEntity.findById(user.id) ?: run {
                            UserEntity.new(user.id) {
                                voiceEntity = VoiceEntity.new { }
                            }
                        }
                    }

                    val modified = committingTransaction {
                        userEntity.voiceEntity.modifyByOptions(arguments)
                    }

                    val new = transaction {
                        userEntity.voiceEntity.getRow()
                    }

                    respondEmbed(
                        if (!modified) ":loudspeaker: Current Voice"
                        else ":arrows_counterclockwise: Voice Updated"
                    ) {
                        authorOf(user)
                        voiceParameterFieldsOf(new)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Voice Set: @${user.username} set their voice"
                    }
                }
            }

            publicSubCommand("reset", "自分の声を初期化します。") {
                action {
                    val userEntity = transaction {
                        UserEntity.findById(user.id)
                    } ?: run {
                        respondEmbed(
                            ":question: Voice Not Found",
                            "声がまだ設定されていません。`/voice set` で設定することができます。"
                        ) {
                            authorOf(user)
                            warningColor()
                        }

                        return@action
                    }

                    committingTransaction {
                        val voiceEntity = userEntity.voiceEntity
                        userEntity.voiceEntity = VoiceEntity.new { }
                        voiceEntity.delete()
                    }

                    respondEmbed(":broom: Voice Reset", "声を初期化しました。") {
                        authorOf(user)
                        successColor()
                    }

                    log(logger) { guild, user ->
                        "[${guild.name}] Voice Reset: @${user.username} reset their voice"
                    }
                }
            }
        }
    }
}