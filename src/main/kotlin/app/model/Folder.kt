package app.model

data class Folder(
    val remoteSharePath: String,
    val localTempPath: String,
    val remoteCompletePath: String,
    val localPostProcessingPath: String
)