package app.downloader

import app.model.DownloaderConfig
import app.model.TorrentFile
import okio.ForwardingSource
import okio.Source
import okio.source
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPSClient
import java.io.File


class FTPClient(private val downloader: DownloaderConfig) : Downloader {

    private val ftp = FTPSClient()

    override fun connect() {
        ftp.controlEncoding = "UTF-8";
        ftp.autodetectUTF8 = true;
        ftp.connect(downloader.host, downloader.port)
        ftp.login(downloader.username, downloader.password)
        ftp.setFileType(FTP.BINARY_FILE_TYPE)
        ftp.enterLocalPassiveMode()
    }

    override fun disconnect() {
        ftp.logout()
        ftp.disconnect()
    }

    override fun getFile(file: String, resumeAt: Long): Source {
        ftp.restartOffset = resumeAt

        val inputStream = ftp.retrieveFileStream(file)

        return object : ForwardingSource(inputStream.source()) {
            var closed = false
            override fun close() {
                if (!closed){
                    val completePendingCommand = ftp.completePendingCommand()
                    if (!completePendingCommand) {
                        IllegalStateException("Transfer failed :(").printStackTrace()
                    }
                    closed = true
                }
                super.close()
            }
        }
    }

    override fun getLocalSize(file: TorrentFile, localTempPath: String): Long {
        val localFile = File(localTempPath, file.name)

        return if (localFile.exists()) {
            localFile.length()
        } else {
            0
        }
    }

    override fun getRemoteSize(file: TorrentFile, remoteCompletePath: String): Long {
        val root = downloader.root
        val remotePath = remoteCompletePath.replace(root, "")
        val remoteFile = remotePath + "/" + file.name
        return ftp.mlistFile(remoteFile).size ?: 0
    }


    override fun getRoot(): String = downloader.root


}