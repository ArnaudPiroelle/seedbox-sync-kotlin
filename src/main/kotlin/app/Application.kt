package app

import app.task.ServerTask
import app.task.SyncTask
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime


@ExperimentalTime
fun main(argv: Array<String>) = runBlocking {
    val syncCommand = SyncCommand()
    val serverCommand = ServerCommand()
    val jc = JCommander.newBuilder()
        .addCommand("sync", syncCommand)
        .addCommand("server", serverCommand)
        .build()
    jc.parse(*argv)

    val command = when (jc.parsedCommand) {
        "sync" -> syncCommand
        "server" -> serverCommand
        else -> null
    }

    when (command) {
        is SyncCommand -> SyncTask(command.configFile)
        is ServerCommand -> ServerTask(command.configFile)
        else -> jc.usage()
    }
}

abstract class BaseCommand {
    @Parameter(names = ["-c", "--config"])
    var configFile = "/config/config.json"
}

@Parameters(commandDescription = "Sync seedbox with local folder")
class SyncCommand : BaseCommand() {

}

@Parameters(commandDescription = "Start sync server")
class ServerCommand : BaseCommand() {

}