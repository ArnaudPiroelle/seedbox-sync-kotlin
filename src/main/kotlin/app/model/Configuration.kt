package app.model

data class Configuration(
    val provider: Provider,
    val downloader: DownloaderConfig,
    val folders: List<Folder>,
    val hooks: List<Hook>
)

data class Provider(val url: String, val username: String, val password: String)
data class DownloaderConfig(
    val type: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val root: String
)