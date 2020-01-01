package app.model

class Torrent(
    val id: Long,
    val name: String,
    val percentDone: Float,
    val files: List<TorrentFile>,
    val downloadDir: String
)

class TorrentFile(
    val name: String,
    val length: Long,
    val bytesCompleted: Long
) {

}