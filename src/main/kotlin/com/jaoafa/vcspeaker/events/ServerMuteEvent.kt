package com.jaoafa.vcspeaker.events

import com.jaoafa.vcspeaker.tts.narrators.Narrator.Companion.announce
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.isNullOrBot
import dev.kord.common.entity.AuditLogEvent
import dev.kord.core.behavior.getAuditLogEntries
import dev.kord.core.event.user.VoiceStateUpdateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class ServerMuteEvent : Extension() {
    override val name = this::class.simpleName!!
    private val logger = KotlinLogging.logger { }

    override suspend fun setup() {
        event<VoiceStateUpdateEvent> {
            check {
                failIf(event.state.getMemberOrNull().isNullOrBot())
                failIf(event.state.isMuted == event.old?.isMuted)
            }

            action {
                val muted = event.state.isMuted
                val hadMuted = event.old?.isMuted ?: return@action

                val member = event.state.getMemberOrNull() ?: return@action
                val guild = event.state.getGuildOrNull() ?: return@action

                val auditLogEntry = guild.getAuditLogEntries {
                    action = AuditLogEvent.MemberUpdate
                }.filter {
                    it.targetId == member.id
                }.first()

                val muter = auditLogEntry.userId?.let { guild.getMember(it) } ?: return@action

                val memberName = member.effectiveName
                val userMention = "@" + member.username
                val muterMention = "@" + muter.username

                if (muted && !hadMuted) {
                    guild.announce(
                        voice = "$memberName がサーバーミュートされました。",
                        text = ":face_with_symbols_over_mouth: `$userMention` が `$muterMention` にサーバーミュートされました。"
                    )

                    logger.info { "[${guild.name}] Member muted: $userMention was muted by $muterMention" }
                } else if (!muted && hadMuted) {
                    guild.announce(
                        voice = "$memberName のサーバーミュートが解除されました。",
                        text = ":grinning: `$userMention` のサーバーミュートを `$muterMention` が解除しました。"
                    )

                    logger.info { "[${guild.name}] Member unmuted: $userMention was unmuted by $muterMention" }
                }
            }
        }
    }
}