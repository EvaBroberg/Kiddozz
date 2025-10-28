package fi.kidozz.app.features.messaging.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MessagingDatabase : RoomDatabase() {
    abstract fun messagingDao(): MessagingDao

    companion object {
        @Volatile
        private var INSTANCE: MessagingDatabase? = null

        fun getDatabase(context: Context): MessagingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MessagingDatabase::class.java,
                    "messaging_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
