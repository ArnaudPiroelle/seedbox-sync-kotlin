package app

import app.task.SchedulerTask
import app.task.SyncTask
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime


@ExperimentalTime
fun main(argv: Array<String>) = runBlocking {
    val syncCommand = SyncCommand()
    val schedulerCommand = SchedulerCommand()
    val jc = JCommander.newBuilder()
        .addCommand("sync", syncCommand)
        .addCommand("scheduler", schedulerCommand)
        .build()
    jc.parse(*argv)

    val command = when (jc.parsedCommand) {
        "sync" -> syncCommand
        "scheduler" -> schedulerCommand
        else -> null
    }

    when (command) {
        is SyncCommand -> SyncTask(command.configFile)
        is SchedulerCommand -> SchedulerTask(command.configFile)
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

@Parameters(commandDescription = "Start sync scheduler")
class SchedulerCommand : BaseCommand() {

}