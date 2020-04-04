package app.notifier

import app.core.HookManager
import app.model.Folder
import app.model.Torrent
import app.model.TorrentFile

class HookNotifier(private val hookManager: HookManager) : Notifier {
    override fun startSynchro() {
        hookManager.execute(HookManager.Type.SyncPre)
    }

    override fun endSynchro() {
        hookManager.execute(HookManager.Type.SyncPost)
    }

    override fun startFolder(folder: Folder) {
        hookManager.execute(HookManager.Type.FolderPre)
    }

    override fun endFolder(folder: Folder) {
        hookManager.execute(HookManager.Type.FolderPost)
    }

    override fun startTorrent(torrent: Torrent) {
        hookManager.execute(HookManager.Type.DownloadPre)
    }

    override fun endTorrent(torrent: Torrent) {
        hookManager.execute(HookManager.Type.DownloadPost)
    }

    override fun startFile(file: TorrentFile) {
    }

    override fun progressFile(file: TorrentFile, bytesRead: Long, totalBytesRead: Long) {
    }

    override fun endFile(file: TorrentFile, success: Boolean) {
    }
}