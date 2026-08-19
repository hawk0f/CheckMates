package dev.hawk0f.checkmates.server

import io.ktor.server.html.respondHtml
import io.ktor.server.application.ApplicationCall
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.lang
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

suspend fun ApplicationCall.respondLandingPage(code: String, hostName: String?) {
    respondHtml {
        lang = "ru"
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            title("Chess — приглашение в игру")
            style {
                unsafe {
                    raw(
                        """
                        body { font-family: -apple-system, system-ui, sans-serif; text-align: center;
                               background: #f7f1f7; color: #222; margin: 0; padding: 48px 16px; }
                        .code { font-size: 56px; letter-spacing: 8px; font-weight: 700; margin: 24px 0; }
                        .card { max-width: 420px; margin: 0 auto; background: white; border-radius: 16px;
                                padding: 32px; box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
                        .hint { color: #666; font-size: 15px; line-height: 1.5; }
                        """.trimIndent()
                    )
                }
            }
        }
        body {
            div(classes = "card") {
                h1 { +"♟ Chess" }
                if (hostName != null) {
                    h2 { +"$hostName приглашает сыграть" }
                }
                p(classes = "hint") { +"Код игры:" }
                div(classes = "code") { +code }
                p(classes = "hint") {
                    +"Откройте эту ссылку на телефоне с установленным приложением Chess — игра откроется сама. Или введите код вручную на экране Join."
                }
            }
        }
    }
}
