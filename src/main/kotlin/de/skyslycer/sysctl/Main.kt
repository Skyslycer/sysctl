package de.skyslycer.sysctl

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import dev.kord.rest.builder.message.modify.embed
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.flow.map

val dotenv = dotenv {
    ignoreIfMalformed = true
    ignoreIfMissing = true
}

val botToken: String = System.getenv()["BOT_TOKEN"] ?: dotenv["BOT_TOKEN"]
val serverId: String = System.getenv()["SERVER_ID"] ?: dotenv["SERVER_ID"]
val logChannelId: String = System.getenv()["LOG_CHANNEL_ID"] ?: dotenv["LOG_CHANNEL_ID"]
val voucherRoleId: String = System.getenv()["VOUCHER_ROLE_ID"] ?: dotenv["VOUCHER_ROLE_ID"]
val serverIp: String = System.getenv()["SERVER_IP"] ?: dotenv["SERVER_IP"]
val imgurClient: String = System.getenv("IMGUR_CLIENT_ID") ?: dotenv["IMGUR_CLIENT_ID"]

var faviconSave = Pair("", "")
var statusSave: Pair<Status?, Long> = Pair(null, System.currentTimeMillis())

const val CORRECT = "\uD83D\uDFE9"
const val WRONG = "\uD83D\uDFE5"

suspend fun main() {
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

    kord.createGuildChatInputCommand(
        Snowflake(serverId.toLong()),
        "check",
        "Check if the provided IP is the correct one."
    ) {
        string("ip", "What IP do you want to check?") {
            required = true
        }
    }

    kord.createGuildChatInputCommand(
        Snowflake(serverId.toLong()),
        "status",
        "Get the status of the LiveOverflow Minecraft server."
    )

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        when (interaction.command.rootName) {
            "vouch" -> vouch(this, server, logChannel)
            "check" -> check(this, logChannel)
            "status" -> status(this, server)
        }
    }

    kord.login()
}

suspend fun status(event: GuildChatInputCommandInteractionCreateEvent, server: Guild) {
    println("1")
    val response = event.interaction.deferEphemeralResponse()
    var hasRole = false
    server.getMember(event.interaction.user.id).roles.map { it.id }.collect {
        if (it == Snowflake(
                voucherRoleId
            )
        ) hasRole = true
    }
    if (!hasRole) {
        response.respond {
            content = "$WRONG You need to have the `rw` role to get the status of the server!"
        }
        return
    }
    val status = if (statusSave.first == null || System.currentTimeMillis() - statusSave.second >= 1000 * 60 * 5) Status.request() else statusSave.first!!
    if (status == null) {
        statusError(response, "Status returned bad code")
        return
    }
    statusSave = Pair(status, System.currentTimeMillis())
    val imageLink = if (faviconSave.first != status.icon) Imgur.upload(status.icon.replace("data:image/png;base64,", "")) else faviconSave.second
    if (imageLink == null) {
        statusError(response, "Image upload returned bad code")
        return
    }
    faviconSave = Pair(status.icon, imageLink)
    response.respond {
        embed {
            title = "LiveOverflow Server Status"
            description = status.motd.clean[0]
            thumbnail {
                url = imageLink
            }
            field {
                name = "Players"
                value = "${status.players.online}/${status.players.max}"
            }
            field {
                name = "Version"
                value = """
                    ${status.version}: ${status.protocol}
                    ${status.software}
                """.trimIndent()
            }
        }
    }
}

suspend fun statusError(response: DeferredEphemeralMessageInteractionResponseBehavior, error: String) {
    response.respond {
        content = "$WRONG An error occurred whilst trying generate the status: $error"
    }
}

suspend fun check(
    event: GuildChatInputCommandInteractionCreateEvent,
    logChannel: GuildMessageChannel
) {
    val response = event.interaction.deferEphemeralResponse()
    val command = event.interaction.command
    val ip = command.strings["ip"]!!
    if (ip == serverIp || ip == serverIp.split(":")[0]) {
        response.respond {
            content =  "$CORRECT **Correct IP!** Congratulations, you have found the correct IP address!"
        }
        logChannel.createMessage("User ${event.interaction.user.tag} found the correct IP address!")
    } else {
        response.respond {
            content =  "$WRONG **Sorry!** This isn't the correct IP address, keep searching!"
        }
    }
}

suspend fun vouch(event: GuildChatInputCommandInteractionCreateEvent, server: Guild, logChannel: GuildMessageChannel) {
    val response = event.interaction.deferEphemeralResponse()
    val command = event.interaction.command
    val user = command.users["user"]!!
    var hasRole = false
    server.getMember(event.interaction.user.id).roles.map { it.id }.collect {
        if (it == Snowflake(
                voucherRoleId
            )
        ) hasRole = true
    }
    if (!hasRole) {
        response.respond {
            content = "$WRONG You need to have the `rw` role to vouch for other users!"
        }
        return
    }
    response.respond {
        content = "$CORRECT Successfully vouched for the user ${user.tag}!"
    }
    server.getMember(user.id).addRole(Snowflake(voucherRoleId.toLong()), "Vouching system")
    logChannel.createMessage("User ${event.interaction.user.tag} vouched for ${user.tag}!")
}