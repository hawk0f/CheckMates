package dev.hawk0f.checkmates.server

import io.ktor.server.application.Application
import java.nio.file.Files

internal fun Application.testModule() {
    val directory = Files.createTempDirectory("chess-test")
    module(
        dbPath = directory.resolve("test.db").toString(),
        backupDirectory = directory.resolve("backups").toString()
    )
}
