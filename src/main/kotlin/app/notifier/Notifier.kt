package app.notifier

import app.model.Folder
import app.model.Torrent
import app.model.TorrentFile

interface Notifier {
    fun startSynchro()
    fun endSynchro()
    fun startFolder(folder: Folder)
    fun endFolder(folder: Folder)
    fun startTorrent(torrent: Torrent)
    fun endTorrent(torrent: Torrent)
    fun startFile(file: TorrentFile)
    fun progressFile(file: TorrentFile, bytesRead: Long, totalBytesRead: Long)
    fun endFile(file: TorrentFile, success: Boolean)
}