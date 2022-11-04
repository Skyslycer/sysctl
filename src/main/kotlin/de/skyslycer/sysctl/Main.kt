package de.skyslycer.sysctl

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.user
import io.github.cdimascio.dotenv.dotenv

suspend fun main() {
    val dotenv = dotenv {
        ignoreIfMalformed = true
        ignoreIfMissing = true
    }

    val botToken = System.getenv()["BOT_TOKEN"] ?: dotenv["BOT_TOKEN"]
    val serverId = System.getenv()["SERVER_ID"] ?: dotenv["SERVER_ID"]
    val logChannelId = System.getenv()["LOG_CHANNEL_ID"] ?: dotenv["LOG_CHANNEL_ID"]
    val voucherRoleId = System.getenv()["VOUCHER_ROLE_ID"] ?: dotenv["VOUCHER_ROLE_ID"]
    val kord = Kord(botToken)
    val server = kord.getGuild(Snowflake(serverId.toLong()))!!
    val logChannel = server.getChannelOf<GuildMessageChannel>(Snowflake(logChannelId.toLong()))

    kord.createGuildChatInputCommand(
        Snowflake(serverId.toLong()),
        "vouch",
        "Vouch for a user to get access to the entirety of the server."
    ) {
        user("user", "Which user do you want to vouch for?") {
            required = true
        }
    }

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val response = interaction.deferEphemeralResponse()
        val command = interaction.command
        val user = command.users["user"]!!
        response.respond {
            content = "Successfully vouched for the user ${user.tag}!"
        }
        server.getMember(user.id).addRole(Snowflake(voucherRoleId.toLong()), "Vouching system")
        logChannel.createMessage("User ${interaction.user.tag} vouched for ${user.tag}!")
    }

    kord.login()
}

class SysCtlBot