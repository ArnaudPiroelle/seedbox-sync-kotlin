package app.downloader

import app.model.TorrentFile

interface Downloader {
    fun connect()
    fun disconnect()
    suspend fun download(file: TorrentFile, remoteCompletePath: String, localTempPath: String)
}