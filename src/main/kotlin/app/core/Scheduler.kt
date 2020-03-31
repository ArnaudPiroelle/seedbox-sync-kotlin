package app.core

import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

private val executor = Executors.newCachedThreadPool()

fun schedule(expression: String, block: () -> Unit): Future<*> {
    return executor.submit {
        val cronSequenceGenerator = CronSequenceGenerator(expression)
        while (true) {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val now = Date()
            val next = cronSequenceGenerator.next(now)
            val sleepTime = next.time - now.time
            if (sleepTime > 0) {
                Thread.sleep(sleepTime)
            }
        }
    }
}