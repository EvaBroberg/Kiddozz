package fi.kidozz.app.navigation

object Routes {
    const val ROLE_SELECTION = "role_selection"
    const val ACCEPT_INVITE = "accept_invite"
    const val INVITE_REQUIRED = "invite_required"
    const val EDU_GRAPH = "educator_graph"
    const val KIDS_OVERVIEW = "kids_overview"
    const val CALENDAR = "calendar"
    const val KID_DETAIL = "kid_detail"
    const val KID_DETAIL_ROUTE = "kid_detail/{kidId}"
    const val MENU = "menu"
    const val PROFILE = "profile"
    
    fun kidDetail(kidId: String) = "kid_detail/$kidId"
}
