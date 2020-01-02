package app.downloader

import app.model.DownloaderConfig
import app.model.TorrentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import okio.*
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPSClient
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class FTPClient(private val downloader: DownloaderConfig): Downloader {

    private val ftp = FTPSClient()

    override fun connect() {
        ftp.connect(downloader.host, downloader.port)
        ftp.login(downloader.username, downloader.password)
        ftp.setFileType(FTP.BINARY_FILE_TYPE)
        ftp.enterLocalPassiveMode()
    }

    override fun disconnect() {
        ftp.logout()
        ftp.disconnect()
    }

    override suspend fun download(file: TorrentFile, remoteCompletePath: String, localTempPath: String) =
        withContext(Dispatchers.IO) {
            val root = downloader.root
            val remotePath = remoteCompletePath.replace(root, "")
            val remoteFile = remotePath + "/" + file.name

            val localFile = File(localTempPath, file.name)
            var localSize = 0L
            if (localFile.exists()) {
                localSize = localFile.length()
            } else {
                localFile.parentFile.mkdirs()
                localFile.createNewFile()
            }
            val remoteSize = ftp.mlistFile(remoteFile).size ?: 0

            println(file.name)
            if (localSize >= remoteSize) {
                return@withContext
            }

            ftp.restartOffset = localSize

            val progress = ProgressBar("Progress", remoteSize, ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
            FileOutputStream(localFile, true).use { out ->
                out.sink().buffer().use { buffer ->
                    ftp.retrieveFileStream(remoteFile).use { retrieveFileStream ->
                        val source = retrieveFileStream.toSourceWithProgress { totalRead ->
                            val read = localSize + totalRead
                            progress.stepTo(read)
                        }
                        buffer.writeAll(source)
                        buffer.flush()
                    }
                }
                out.flush()
            }
            progress.close()

            val completePendingCommand = ftp.completePendingCommand()
            if (!completePendingCommand) {
                throw IllegalStateException("Transfer failed :(")
            }

        }

    private fun InputStream.toSourceWithProgress(function: (Long) -> Unit): Source {
        return object : ForwardingSource(source()) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                function(totalBytesRead)
                return bytesRead
            }
        }
    }
}