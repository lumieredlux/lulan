package com.lulan.app.di

import android.content.Context
import com.lulan.app.signaling.NsdHelper
import com.lulan.app.signaling.WebSocketSignalingServer
import com.lulan.app.util.LanOnlyFilter
import com.lulan.app.webrtc.WebRtcManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideLanOnlyFilter(@ApplicationContext ctx: Context) = LanOnlyFilter(ctx)

    @Provides @Singleton
    fun provideNsd(@ApplicationContext ctx: Context) = NsdHelper(ctx)

    @Provides @Singleton
    fun provideSignaling(@ApplicationContext ctx: Context, filter: LanOnlyFilter) =
        WebSocketSignalingServer(ctx, filter)

    @Provides @Singleton
    fun provideWebRtc(@ApplicationContext ctx: Context, filter: LanOnlyFilter) =
        WebRtcManager(ctx, filter)
}
