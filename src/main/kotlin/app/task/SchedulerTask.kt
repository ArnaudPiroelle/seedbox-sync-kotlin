package app.task

import app.core.schedule
import app.downloader.Downloader
import app.model.Folder
import app.model.SchedulerConfig
import app.notifier.Notifier
import app.provider.Transmission

class SchedulerTask(
    private val downloader: Downloader,
    private val provider: Transmission,
    private val folders: List<Folder>,
    private val notifier: Notifier,
    private val schedulerConfig: SchedulerConfig
) : Task {
    private val syncTask = SyncTask(downloader, provider, folders, notifier)

    override fun execute()  {
        val job = schedule(schedulerConfig.cron) {
            syncTask.execute()
        }

        job.get()
    }

}
