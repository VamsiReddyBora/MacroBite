package com.macrobite.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.macrobite.app.data.local.entity.ChatMessageEntity
import com.macrobite.app.data.local.entity.CustomFoodEntity
import com.macrobite.app.data.local.entity.MealEntity
import com.macrobite.app.data.local.entity.WeightEntity

@Database(
    entities = [MealEntity::class, WeightEntity::class, CustomFoodEntity::class, ChatMessageEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun weightDao(): WeightDao
    abstract fun customFoodDao(): CustomFoodDao
    abstract fun chatDao(): ChatDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_foods` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `portion` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `protein` INTEGER NOT NULL,
                        `carbs` INTEGER NOT NULL,
                        `fats` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Migrate meals table: change protein, carbs, fats to REAL
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meals_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `foodName` TEXT NOT NULL,
                        `portion` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `protein` REAL NOT NULL,
                        `carbs` REAL NOT NULL,
                        `fats` REAL NOT NULL,
                        `photoUri` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `meals_new` (`id`, `date`, `category`, `foodName`, `portion`, `calories`, `protein`, `carbs`, `fats`, `photoUri`, `timestamp`)
                    SELECT `id`, `date`, `category`, `foodName`, `portion`, `calories`, CAST(`protein` AS REAL), CAST(`carbs` AS REAL), CAST(`fats` AS REAL), `photoUri`, `timestamp`
                    FROM `meals`
                """.trimIndent())

                db.execSQL("DROP TABLE `meals`")
                db.execSQL("ALTER TABLE `meals_new` RENAME TO `meals`")

                // 2. Migrate custom_foods table: change protein, carbs, fats to REAL
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_foods_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `portion` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `protein` REAL NOT NULL,
                        `carbs` REAL NOT NULL,
                        `fats` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `custom_foods_new` (`id`, `name`, `portion`, `calories`, `protein`, `carbs`, `fats`, `timestamp`)
                    SELECT `id`, `name`, `portion`, `calories`, CAST(`protein` AS REAL), CAST(`carbs` AS REAL), CAST(`fats` AS REAL), `timestamp`
                    FROM `custom_foods`
                """.trimIndent())

                db.execSQL("DROP TABLE `custom_foods`")
                db.execSQL("ALTER TABLE `custom_foods_new` RENAME TO `custom_foods`")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migrate meals table to REAL
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meals_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `foodName` TEXT NOT NULL,
                        `portion` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `protein` REAL NOT NULL,
                        `carbs` REAL NOT NULL,
                        `fats` REAL NOT NULL,
                        `photoUri` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `meals_new` (`id`, `date`, `category`, `foodName`, `portion`, `calories`, `protein`, `carbs`, `fats`, `photoUri`, `timestamp`)
                    SELECT `id`, `date`, `category`, `foodName`, `portion`, `calories`, CAST(`protein` AS REAL), CAST(`carbs` AS REAL), CAST(`fats` AS REAL), `photoUri`, `timestamp`
                    FROM `meals`
                """.trimIndent())

                db.execSQL("DROP TABLE `meals`")
                db.execSQL("ALTER TABLE `meals_new` RENAME TO `meals`")

                // Create custom_foods with REAL directly
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_foods` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `portion` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `protein` REAL NOT NULL,
                        `carbs` REAL NOT NULL,
                        `fats` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `fiber` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `sugar` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `sodium` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `saturatedFat` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `potassium` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `cholesterol` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `meals` ADD COLUMN `vitaminsAndMinerals` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `chatDate` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `isUser` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `foodPayloadJson` TEXT,
                        `isLogged` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_chatDate` ON `chat_messages` (`chatDate`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `imageUri` TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `isWebSearch` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `actionPayloadJson` TEXT")
            }
        }
    }
}
