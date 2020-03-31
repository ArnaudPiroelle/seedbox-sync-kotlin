package app.notifier

import app.model.Folder
import app.model.Torrent
import app.model.TorrentFile

class LoggerNotifier : Notifier {
    override fun startSynchro() {

    }

    override fun endSynchro() {
    }

    override fun startFolder(folder: Folder) {
    }

    override fun endFolder(folder: Folder) {
    }

    override fun startTorrent(torrent: Torrent) {
    }

    override fun endTorrent(torrent: Torrent) {
    }

    override fun startFile(file: TorrentFile) {
    }

    override fun progressFile(file: TorrentFile, bytesRead: Long, totalBytesRead: Long) {
    }

    override fun endFile(file: TorrentFile, success: Boolean) {
    }
}