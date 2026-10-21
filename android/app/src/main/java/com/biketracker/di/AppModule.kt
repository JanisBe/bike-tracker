package com.biketracker.di

import com.biketracker.data.repository.AppUpdateRepository
import com.biketracker.data.repository.AppUpdateRepositoryImpl
import com.biketracker.data.repository.AuthRepository
import com.biketracker.data.repository.AuthRepositoryImpl
import com.biketracker.data.repository.MapPreferencesRepository
import com.biketracker.data.repository.MapPreferencesRepositoryImpl
import com.biketracker.data.repository.PowerSaveRepository
import com.biketracker.data.repository.PowerSaveRepositoryImpl
import com.biketracker.data.repository.RideRepository
import com.biketracker.data.repository.RideRepositoryImpl
import com.biketracker.data.repository.UserRepository
import com.biketracker.data.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRideRepository(impl: RideRepositoryImpl): RideRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindMapPreferencesRepository(impl: MapPreferencesRepositoryImpl): MapPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindPowerSaveRepository(impl: PowerSaveRepositoryImpl): PowerSaveRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: AppUpdateRepositoryImpl): AppUpdateRepository
}
