package com.macrobite.app.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.macrobite.app.data.local.AppDatabase
import com.macrobite.app.data.local.ChatDao
import com.macrobite.app.data.local.CustomFoodDao
import com.macrobite.app.data.local.MealDao
import com.macrobite.app.data.local.WeightDao
import com.macrobite.app.data.preferences.UserPreferencesRepositoryImpl
import com.macrobite.app.data.repository.ChatRepositoryImpl
import com.macrobite.app.data.repository.CustomFoodRepositoryImpl
import com.macrobite.app.data.repository.MealRepositoryImpl
import com.macrobite.app.data.repository.WeightRepositoryImpl
import com.macrobite.app.domain.repository.ChatRepository
import com.macrobite.app.domain.repository.CustomFoodRepository
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "macrobite_db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_1_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMealDao(database: AppDatabase): MealDao = database.mealDao()

    @Provides
    fun provideWeightDao(database: AppDatabase): WeightDao = database.weightDao()

    @Provides
    fun provideCustomFoodDao(database: AppDatabase): CustomFoodDao = database.customFoodDao()

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository

    @Binds
    @Singleton
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    @Singleton
    abstract fun bindCustomFoodRepository(impl: CustomFoodRepositoryImpl): CustomFoodRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
