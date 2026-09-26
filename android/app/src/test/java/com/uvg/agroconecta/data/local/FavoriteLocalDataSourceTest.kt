package com.uvg.agroconecta.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteLocalDataSourceTest {

    private val userId = 25

    @Test
    fun `DataStore preferences retain favorite additions and removals`() = runTest {
        val source = DataStoreFavoriteLocalDataSource(InMemoryPreferencesDataStore())

        assertEquals(emptySet<Int>(), source.favoriteIds(userId).first())

        source.setFavorite(userId = userId, productId = 7, favorite = true)
        source.setFavorite(userId = userId, productId = 12, favorite = true)
        assertEquals(setOf(7, 12), source.favoriteIds(userId).first())

        source.setFavorite(userId = userId, productId = 7, favorite = false)
        assertEquals(setOf(12), source.favoriteIds(userId).first())
    }

    @Test
    fun `server refresh replaces stale IDs and discards invalid values`() = runTest {
        val source = DataStoreFavoriteLocalDataSource(InMemoryPreferencesDataStore())

        source.setFavorite(userId = userId, productId = 99, favorite = true)
        source.replaceFavoriteIds(userId = userId, productIds = setOf(-1, 4, 8))

        assertEquals(setOf(4, 8), source.favoriteIds(userId).first())
    }

    @Test
    fun `favorite IDs are isolated between users`() = runTest {
        val source = DataStoreFavoriteLocalDataSource(InMemoryPreferencesDataStore())

        source.setFavorite(userId = 10, productId = 7, favorite = true)
        source.setFavorite(userId = 20, productId = 12, favorite = true)

        assertEquals(setOf(7), source.favoriteIds(10).first())
        assertEquals(setOf(12), source.favoriteIds(20).first())
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val preferences = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = preferences

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences = transform(preferences.value).also { preferences.value = it }
}
