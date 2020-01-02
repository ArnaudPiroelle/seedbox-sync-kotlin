package app.downloader

import app.model.DownloaderConfig
import app.model.TorrentFile
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.FileOutputStream


class SFTPClient(private val downloader: DownloaderConfig): Downloader {

    private val session by lazy {
        val jsch = JSch()
        jsch.setKnownHosts("${System.getProperty("user.home")}/.ssh/known_hosts")
        val jschSession = jsch.getSession(downloader.username, downloader.host, downloader.port)
        jschSession.setPassword(downloader.password)
        jschSession
    }
    private val sftpClient by lazy {
        session.openChannel("sftp") as ChannelSftp
    }

    override fun connect() {
        session.connect()
        sftpClient.connect()
        return
    }

    override fun disconnect() {
        session.disconnect()
    }

    override suspend fun download(file: TorrentFile, remoteCompletePath: String, localTempPath: String) = withContext(IO) {
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

        val stat = sftpClient.stat(remoteFile)
        val remoteSize = stat.size
        println(file.name)
        if (localSize >= remoteSize) {
            return@withContext
        }

        val monitor = object : SftpProgressMonitor {
            val progress = ProgressBar("Progress", remoteSize, ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
            var totalBytesRead = 0L
            override fun count(count: Long): Boolean {
                totalBytesRead += count
                progress.stepTo(totalBytesRead)
                return true
            }

            override fun end() {
                progress.close()
            }

            override fun init(op: Int, src: String?, dest: String?, max: Long) {
                totalBytesRead = localSize
            }
        }

        sftpClient.get(remoteFile, monitor, localSize).use { retrieveFileStream ->
            FileOutputStream(localFile, true).use { out ->
                out.sink().buffer().use { buffer ->
                    buffer.writeAll(retrieveFileStream.source())
                    buffer.flush()
                }
            }
        }

    }
}