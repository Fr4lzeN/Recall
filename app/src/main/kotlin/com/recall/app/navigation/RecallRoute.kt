package com.recall.app.navigation

object RecallRoute {
    const val SEARCH = "search"
    const val TIMELINE = "timeline"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val DIRECTORY_EXCLUSIONS = "directory-exclusions"
    const val DETAIL = "detail/{mediaId}"
    const val MEDIA_ID_ARG = "mediaId"

    fun detailRoute(mediaId: String): String = "detail/$mediaId"
}
