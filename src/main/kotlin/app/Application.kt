package app

import app.task.SyncTask
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import kotlinx.coroutines.runBlocking


fun main(argv: Array<String>) = runBlocking {
    val syncCommand = SyncCommand()
    val jc = JCommander.newBuilder()
        .addCommand("sync", syncCommand)
        .build()
    jc.parse(*argv)

    val configFile = syncCommand.configFile
    when (jc.parsedCommand) {
        "sync" -> SyncTask(configFile)
        else -> jc.usage()
    }
}

@Parameters(commandDescription = "Sync seedbox with local folder")
class SyncCommand {
    @Parameter(names = ["-c", "--config"])
    var configFile = "/config/config.json"
}