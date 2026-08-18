package dev.hawk0f.chess.server

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private const val ANDROID_PACKAGE = "dev.hawk0f.chess"
private val androidFingerprints = (System.getenv("ANDROID_CERT_FINGERPRINTS")
    ?: "0D:80:6E:09:29:B8:3B:D3:26:5A:F5:4D:DE:AE:AC:6D:30:67:A4:E8:4E:C9:94:C1:51:82:E1:63:E9:F2:FC:EA")
    .split(",")
    .map { it.trim() }

private val appleTeamId = System.getenv("APPLE_TEAM_ID") ?: "TEAMID_PLACEHOLDER"

fun Route.wellKnownRoutes() {
    get("/.well-known/assetlinks.json") {
        val fingerprints = androidFingerprints.joinToString(",") { "\"$it\"" }
        call.respondText(
            """
            [{
              "relation": ["delegate_permission/common.handle_all_urls"],
              "target": {
                "namespace": "android_app",
                "package_name": "$ANDROID_PACKAGE",
                "sha256_cert_fingerprints": [$fingerprints]
              }
            }]
            """.trimIndent(),
            ContentType.Application.Json
        )
    }

    get("/.well-known/apple-app-site-association") {
        call.respondText(
            """
            {
              "applinks": {
                "details": [{
                  "appIDs": ["$appleTeamId.$ANDROID_PACKAGE"],
                  "components": [{"/": "/game/*"}]
                }]
              }
            }
            """.trimIndent(),
            ContentType.Application.Json
        )
    }
}
