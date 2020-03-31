package app.notifier

import app.model.Folder
import app.model.Torrent
import app.model.TorrentFile
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import kotlin.properties.Delegates

class ConsoleNotifier : Notifier {

    private var progress: ProgressBar? = null

    override fun startSynchro() {
        println("Synchronization started")
    }

    override fun endSynchro() {
        println("Synchronization ended")
    }

    override fun startFolder(folder: Folder) {
        println("Start folder")
    }

    override fun endFolder(folder: Folder) {

    }

    override fun startTorrent(torrent: Torrent) {
        println(torrent.name)
    }

    override fun endTorrent(torrent: Torrent) {

    }

    override fun startFile(file: TorrentFile) {
        println(file.name)
    }

    override fun progressFile(file: TorrentFile, bytesRead: Long, totalBytesRead: Long) {
        progress = progress ?: ProgressBar("Download", file.length, ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
        progress?.stepTo(totalBytesRead)
        if (totalBytesRead >=  file.length){
            progress?.close()
            progress = null
        }
    }

    override fun endFile(file: TorrentFile, success: Boolean) {

    }
}