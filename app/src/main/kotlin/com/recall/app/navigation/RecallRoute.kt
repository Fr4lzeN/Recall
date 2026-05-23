package com.recall.app.navigation

object RecallRoute {
    const val SEARCH = "search"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "albums/{albumIndex}"
    const val ALBUM_INDEX_ARG = "albumIndex"
    const val TIMELINE = "timeline"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val DIRECTORY_EXCLUSIONS = "directory-exclusions"
    const val DETAIL = "detail/{mediaId}"
    const val MEDIA_ID_ARG = "mediaId"

    fun detailRoute(mediaId: String): String = "detail/$mediaId"
}
