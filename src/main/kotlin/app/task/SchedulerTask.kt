package app.task

import app.core.CronSequenceGenerator
import app.model.Configuration
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.FileReader
import java.util.*


object SchedulerTask {

    suspend operator fun invoke(configFile: String) = withContext(Dispatchers.IO) {
        val gson = Gson()

        val configuration = gson.fromJson(FileReader(configFile), Configuration::class.java)
        val cronSequenceGenerator = CronSequenceGenerator(configuration.scheduler.cron)

        while (isActive) {
            try {
                SyncTask(configFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val now = Date()
            val next = cronSequenceGenerator.next(now)
            val sleepTime = next.time - now.time
            if (sleepTime > 0) {
                delay(sleepTime)
            }
        }
    }
}