package de.skyslycer.sysctl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class Status(
    val ip: String,
    val port: Int,
    val debug: Map<String, @Contextual Any>,
    val players: Players,
    val motd: Motd,
    val version: String,
    val online: Boolean,
    val protocol: Int,
    val icon: String,
    val software: String
) {
    companion object {
        private const val BASE_URL = "https://api.mcsrvstat.us/2/"
        private val client = HttpClient(CIO)

        suspend fun request(): Status? {
            val response = client.get("$BASE_URL$serverIp")
            if (response.status != HttpStatusCode.OK) return null
            val body = response.body() as String
            return Json.decodeFromString(body)
        }
    }
}

@Serializable
data class Motd(val raw: List<String>, val clean: List<String>, val html: List<String>)

@Serializable
data class Players(val online: Int, val max: Int)