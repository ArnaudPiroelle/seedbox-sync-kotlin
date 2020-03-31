package app.task

import app.downloader.Downloader
import app.model.Folder
import app.model.Torrent
import app.model.TorrentFile
import app.notifier.Notifier
import app.provider.Transmission
import okio.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class SyncTask(
    private val downloader: Downloader,
    private val provider: Transmission,
    private val folders: List<Folder>,
    private val notifier: Notifier
) : Task {

    override fun execute() {
        notifier.startSynchro()

        folders.forEach { folder ->
            try {
                notifier.startFolder(folder)
                synchronize(provider, downloader, folder)
                notifier.endFolder(folder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        notifier.endSynchro()
    }

    private fun synchronize(provider: Transmission, downloader: Downloader, folder: Folder) {
        val torrents = provider.getTorrents()

        torrents.filter { torrent -> torrent.downloadDir == folder.remoteCompletePath }
            .filter { torrent -> torrent.percentDone == 1f }
            .forEach { torrent ->
                try {
                    downloadTorrent(folder, torrent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    private fun downloadTorrent(folder: Folder, torrent: Torrent) {
        notifier.startTorrent(torrent)
        var hasError = false
        torrent.files
            .filter { file -> file.isComplete() }
            .forEach { file ->
                notifier.startFile(file)

                try {
                    downloadFile(folder, file)
                } catch (e: Exception) {
                    hasError = true
                }

                notifier.endFile(file, !hasError)
            }

        if (!hasError) {
            provider.setLocation(torrent, folder.remoteSharePath)
            torrent.files.forEach { file -> moveFile(folder, file) }
        }

        notifier.endTorrent(torrent)
    }

    private fun downloadFile(folder: Folder, file: TorrentFile) {
        val root: String = downloader.getRoot()
        val remotePath = folder.remoteCompletePath.replace(root, "")
        val remoteFile = remotePath + "/" + file.name
        val localFile = File(folder.localTempPath, file.name)

        val localSize = getLocalSize(file, folder.localTempPath)
        if (localSize == 0L) {
            localFile.parentFile.mkdirs()
            localFile.createNewFile()
        }
        val remoteSize = downloader.getRemoteSize(file, folder.remoteCompletePath)
        if (localSize < remoteSize) {
            downloader.getFile(remoteFile, localSize).use { inputStream ->
                FileOutputStream(localFile, true).use { fos ->
                    fos.sink().buffer().use { bufferedSink ->
                        inputStream.toSourceWithProgress { bytesRead, totalRead ->
                            val totalBytesRead = localSize + totalRead
                            notifier.progressFile(file, bytesRead, totalBytesRead)
                        }.use { source ->
                            bufferedSink.writeAll(source)
                            bufferedSink.flush()
                        }
                    }
                    fos.flush()
                }
            }
        }
    }

    private fun getLocalSize(file: TorrentFile, localTempPath: String): Long {
        val localFile = File(localTempPath, file.name)

        return if (localFile.exists()) {
            localFile.length()
        } else {
            0
        }
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

    private fun Source.toSourceWithProgress(function: (Long, Long) -> Unit): Source {
        return object : ForwardingSource(this) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                function(byteCount, totalBytesRead)
                return bytesRead
            }
        }
    }

}