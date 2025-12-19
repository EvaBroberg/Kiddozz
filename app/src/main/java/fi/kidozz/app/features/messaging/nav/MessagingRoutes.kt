package fi.kidozz.app.features.messaging.nav

object MessagingRoutes {
    const val MESSAGES_GRAPH = "messages_graph"
    const val MESSAGES_LIST = "messages_list"
    const val CONVERSATION = "conversation"
    const val CONVERSATION_ROUTE = "$CONVERSATION/{conversationId}"
    
    fun conversation(conversationId: String) = "$CONVERSATION/$conversationId"
}