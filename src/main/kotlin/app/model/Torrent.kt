package app.model

data class Torrent(
    val id: Long,
    val name: String,
    val percentDone: Float,
    val files: List<TorrentFile>,
    val downloadDir: String
)

data class TorrentFile(
    val name: String,
    val length: Long,
    val bytesCompleted: Long
) {
    fun isComplete(): Boolean = bytesCompleted == length
}