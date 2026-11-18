package io.github.demiterhugo.propertyhandover

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.demiterhugo.propertyhandover.data.LocalA2uiForms
import io.github.demiterhugo.propertyhandover.domain.Handover

@Module
@InstallIn(SingletonComponent::class)
object HandoverModule {
    @Provides
    fun provideLocalForms(): LocalA2uiForms = LocalA2uiForms()

    @Provides
    fun provideHandover(): Handover = Handover()
}
