package app

import app.Command.Companion.SCHEDULER_COMMAND
import app.Command.Companion.SYNC_COMMAND
import app.core.HookManager
import app.downloader.FTPClient
import app.model.Configuration
import app.notifier.ComposeNotifier
import app.notifier.ConsoleNotifier
import app.notifier.HookNotifier
import app.provider.Transmission
import app.task.SchedulerTask
import app.task.SyncTask
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.google.gson.Gson
import java.io.FileReader

fun main(argv: Array<String>) {
    val jc = JCommander.newBuilder()
        .addCommand(SYNC_COMMAND, Command.Sync)
        .addCommand(SCHEDULER_COMMAND, Command.Scheduler)
        .build()
    jc.parse(*argv)

    val command = when (jc.parsedCommand) {
        SYNC_COMMAND -> Command.Sync
        SCHEDULER_COMMAND -> Command.Scheduler
        else -> null
    }

    if (command == null) {
        jc.usage()
        return
    }

    val gson = Gson()
    val configuration = gson.fromJson(FileReader(command.configFile), Configuration::class.java)

    val hookManager = HookManager(configuration.hooks)
    val provider = Transmission(configuration.provider, gson)
    val downloader = when (configuration.downloader.type) {
        "ftp" -> FTPClient(configuration.downloader)
        //"sftp" -> SFTPClient(configuration.downloader)
        else -> throw IllegalStateException("Downloader not implemented yet")
    }
    val folders = configuration.folders


    val hookNotifier = HookNotifier(hookManager)
    val consoleNotifier = ConsoleNotifier()
    val notifier = ComposeNotifier(hookNotifier, consoleNotifier)

    val schedulerTask = SchedulerTask(downloader, provider, folders, notifier, configuration.scheduler)
    val syncTask = SyncTask(downloader, provider, folders, notifier)

    downloader.connect()
    when (command) {
        is Command.Sync -> syncTask.execute()
        is Command.Scheduler -> schedulerTask.execute()
    }
    downloader.disconnect()

    provider.clean()
    hookManager.clean()
}

sealed class Command {
    @Parameter(names = ["-c", "--config"])
    var configFile = "/config/config.json"

    @Parameters(commandDescription = "Sync seedbox with local folder")
    object Sync : Command()

    @Parameters(commandDescription = "Start sync scheduler")
    object Scheduler : Command()

    @Parameters(commandDescription = "Start sync server")
    object Server : Command()

    companion object {
        const val SYNC_COMMAND = "sync"
        const val SERVER_COMMAND = "server"
        const val SCHEDULER_COMMAND = "scheduler"
    }
}