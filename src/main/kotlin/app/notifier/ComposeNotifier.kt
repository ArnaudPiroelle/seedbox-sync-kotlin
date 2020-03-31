package app.notifier

import app.model.Folder
import app.model.Torrent
import app.model.TorrentFile

class ComposeNotifier(vararg notifiers: Notifier) : Notifier {
    private val notifiers = notifiers.toList()

    override fun startSynchro() = notifiers.forEach {
        it.startSynchro()
    }

    override fun endSynchro() = notifiers.forEach {
        it.endSynchro()
    }

    override fun startFolder(folder: Folder) = notifiers.forEach {
        it.startFolder(folder)
    }

    override fun endFolder(folder: Folder) = notifiers.forEach {
        it.endFolder(folder)
    }

    override fun startTorrent(torrent: Torrent) = notifiers.forEach {
        it.startTorrent(torrent)
    }

    override fun endTorrent(torrent: Torrent)  = notifiers.forEach {
        it.endTorrent(torrent)
    }

    override fun startFile(file: TorrentFile)  = notifiers.forEach {
        it.startFile(file)
    }

    override fun progressFile(file: TorrentFile, bytesRead: Long, totalBytesRead: Long)  = notifiers.forEach {
        it.progressFile(file, bytesRead, totalBytesRead)
    }

    override fun endFile(file: TorrentFile, success: Boolean) = notifiers.forEach {
        it.endFile(file, success)
    }

}