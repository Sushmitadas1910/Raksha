package com.example.raksha

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// ─── Entity ───────────────────────────────────────────────────────────────────

/**
 * Local copy of an emergency contact stored in Room.
 * Mirrors the Firestore document structure exactly so we can sync both ways.
 * The [firestoreId] is the Firestore document ID — used as the primary key
 * so there are no duplicates when we re-sync.
 */
@Entity(tableName = "emergency_contacts")
data class LocalContact(
    @PrimaryKey val firestoreId: String,
    val name: String,
    val phone: String
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface ContactDao {

    /** Returns all locally cached contacts. Never null — returns empty list if none. */
    @Query("SELECT * FROM emergency_contacts")
    fun getAll(): List<LocalContact>

    /**
     * Upsert: insert or replace if the firestoreId already exists.
     * Called whenever Firestore snapshot arrives (online) or on first sync.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(contacts: List<LocalContact>)

    /** Delete a contact by its Firestore document ID. */
    @Query("DELETE FROM emergency_contacts WHERE firestoreId = :id")
    fun deleteById(id: String)

    /** Wipe everything — called on logout. */
    @Query("DELETE FROM emergency_contacts")
    fun deleteAll()
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [LocalContact::class], version = 1, exportSchema = false)
abstract class RakshaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: RakshaDatabase? = null

        fun get(context: Context): RakshaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    RakshaDatabase::class.java,
                    "raksha_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}