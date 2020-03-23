package app.task

import app.core.HookManager
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
import java.io.FileReader

object ServerTask {
    suspend operator fun invoke(configFile: String) = withContext(Dispatchers.IO) {
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
        val torrents = provider.getTorrents().filter { torrent -> torrent.percentDone == 1f }

        val downloads = torrents.flatMap { torrent ->
            folders.firstOrNull { folder -> folder.remoteCompletePath == torrent.downloadDir }
                ?.let { folder ->
                    torrent.files
                        .filter { file ->
                            file.bytesCompleted == file.length
                        }
                        .map { file -> Download(folder, torrent, file) }
                } ?: listOf()
        }

        println(downloads.size)

        // region Clean
        provider.clean()
        hookManager.clean()
        // endregion
    }

    data class Download(val folder: Folder, val torrent: Torrent, val file: TorrentFile)
}