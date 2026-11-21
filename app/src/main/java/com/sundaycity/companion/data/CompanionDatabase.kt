package com.sundaycity.companion.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "garage_vehicles")
data class GarageVehicle(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val category: String = "Auto", val notes: String = "")

@Entity(tableName = "player_goals")
data class PlayerGoal(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val completed: Boolean = false)

@Entity(tableName = "house_rooms")
data class HouseRoom(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val style: String = "Moderno")

@Entity(tableName = "furniture_items")
data class FurnitureItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long, val name: String, val category: String, val emoji: String,
    val x: Int = 1, val y: Int = 1
)

@Dao
interface CompanionDao {
    @Query("SELECT * FROM garage_vehicles ORDER BY name COLLATE NOCASE") fun observeVehicles(): Flow<List<GarageVehicle>>
    @Insert suspend fun addVehicle(vehicle: GarageVehicle)
    @Delete suspend fun deleteVehicle(vehicle: GarageVehicle)

    @Query("SELECT * FROM player_goals ORDER BY id DESC") fun observeGoals(): Flow<List<PlayerGoal>>
    @Insert suspend fun addGoal(goal: PlayerGoal)
    @Query("UPDATE player_goals SET completed = :completed WHERE id = :id") suspend fun setGoalCompleted(id: Long, completed: Boolean)
    @Delete suspend fun deleteGoal(goal: PlayerGoal)

    @Query("SELECT * FROM house_rooms ORDER BY id") fun observeRooms(): Flow<List<HouseRoom>>
    @Insert suspend fun addRoom(room: HouseRoom): Long
    @Delete suspend fun deleteRoom(room: HouseRoom)
    @Query("DELETE FROM furniture_items WHERE roomId = :roomId") suspend fun deleteFurnitureForRoom(roomId: Long)
    @Transaction suspend fun deleteRoomAndFurniture(room: HouseRoom) {
        deleteFurnitureForRoom(room.id)
        deleteRoom(room)
    }
    @Query("SELECT * FROM furniture_items WHERE roomId = :roomId ORDER BY id DESC") fun observeFurniture(roomId: Long): Flow<List<FurnitureItem>>
    @Insert suspend fun addFurniture(item: FurnitureItem)
    @Delete suspend fun deleteFurniture(item: FurnitureItem)
    @Query("UPDATE furniture_items SET x = :x, y = :y WHERE id = :id") suspend fun moveFurniture(id: Long, x: Int, y: Int)
}

@Database(entities = [GarageVehicle::class, PlayerGoal::class, HouseRoom::class, FurnitureItem::class], version = 2, exportSchema = false)
abstract class CompanionDatabase : RoomDatabase() {
    abstract fun companionDao(): CompanionDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS house_rooms (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, style TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS furniture_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, roomId INTEGER NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, emoji TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL)")
            }
        }
        @Volatile private var INSTANCE: CompanionDatabase? = null
        fun get(context: Context): CompanionDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, CompanionDatabase::class.java, "sunday_city_companion.db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
