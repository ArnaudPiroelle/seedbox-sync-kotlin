package app.task

import app.core.HookManager
import app.downloader.Downloader
import app.downloader.FTPClient
import app.downloader.SFTPClient
import app.model.Configuration
import app.model.Folder
import app.model.Torrent
import app.model.TorrentFile
import app.provider.Transmission
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object SyncTask {
    suspend operator fun invoke(configFile: String) = withContext(Dispatchers.IO) {
        println("Synchronization started...")

        // region Configuration
        val gson = Gson()
        val configuration = gson.fromJson(FileReader(configFile), Configuration::class.java)

        val hookManager = HookManager(configuration.hooks)
        val provider = Transmission(configuration.provider, gson)
        val downloader = when (configuration.downloader.type) {
            "ftp" -> FTPClient(configuration.downloader)
            "sftp" -> SFTPClient(configuration.downloader)
            else -> throw IllegalStateException("Downloader not implemented yet")
        }
        val folders = configuration.folders
        // endregion

        // region Process
        hookManager.execute(HookManager.Type.SyncPre)
        downloader.connect()
        folders.forEach { folder ->
            try {
                hookManager.execute(HookManager.Type.FolderPre)
                synchronize(provider, downloader, folder)
                hookManager.execute(HookManager.Type.FolderPost)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        downloader.disconnect()
        hookManager.execute(HookManager.Type.SyncPost)
        // endregion

        // region Clean
        provider.clean()
        hookManager.clean()

        println("Synchronization ended")
        //endregion
    }


    private suspend fun synchronize(provider: Transmission, downloader: Downloader, folder: Folder) {
        val torrents = provider.getTorrents()

        torrents.filter { torrent -> torrent.downloadDir == folder.remoteCompletePath }
            .filter { torrent -> torrent.percentDone == 1f }
            .forEach { torrent ->
                try {
                    downloadTorrent(provider, downloader, folder, torrent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    private suspend fun downloadTorrent(
        provider: Transmission,
        downloader: Downloader,
        folder: Folder,
        torrent: Torrent
    ) {
        println(torrent.name)

        torrent.files
            .filter { file -> file.bytesCompleted == file.length }
            .forEach { file -> downloader.download(file, folder.remoteCompletePath, folder.localTempPath) }

        provider.setLocation(torrent, folder.remoteSharePath)

        torrent.files.forEach { file -> moveFile(folder, file) }
    }

    private fun moveFile(folder: Folder, file: TorrentFile) {
        val oldName = folder.localTempPath + '/' + file.name
        val newName = folder.localPostProcessingPath + '/' + file.name

        File(newName).parentFile.mkdirs()

        Files.move(Path.of(oldName), Path.of(newName), StandardCopyOption.REPLACE_EXISTING)

        val oldDirname = Path.of(oldName).parent.toFile()
        if (oldDirname.path != folder.localTempPath && oldDirname.listFiles()?.isEmpty() == true) {
            oldDirname.deleteRecursively()
        }
    }
}

