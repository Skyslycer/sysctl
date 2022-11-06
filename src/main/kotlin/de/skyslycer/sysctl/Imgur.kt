package de.skyslycer.sysctl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.json.JSONObject

class Imgur {
    companion object {
        private const val BASE_URL = "https://api.imgur.com/3/image"
        private val client = HttpClient(CIO)

        suspend fun upload(image: String): String? {
            val response = client.post {
                url {
                    host = BASE_URL
                }
                headers {
                    append(HttpHeaders.Authorization, "Client-ID $imgurClient")
                }
                setBody("image=$image")
            }
            if (response.status != HttpStatusCode.OK) return null
            val json = JSONObject(response.body() as String)
            return json.getJSONObject("data").getString("link")
        }
    }
}