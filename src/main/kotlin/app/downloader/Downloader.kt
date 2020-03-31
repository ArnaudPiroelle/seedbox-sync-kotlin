package app.downloader

import app.model.TorrentFile
import okio.Source

interface Downloader {
    fun connect()
    fun disconnect()
    fun getFile(file: String, resumeAt: Long): Source
    fun getRemoteSize(file: TorrentFile, remoteCompletePath: String): Long
    fun getLocalSize(file: TorrentFile, localTempPath: String): Long
    fun getRoot(): String
}