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

    @Test
    fun `DataStore preferences retain favorite additions and removals`() = runTest {
        val source = DataStoreFavoriteLocalDataSource(InMemoryPreferencesDataStore())

        assertEquals(emptySet<Int>(), source.favoriteIds.first())

        source.setFavorite(productId = 7, favorite = true)
        source.setFavorite(productId = 12, favorite = true)
        assertEquals(setOf(7, 12), source.favoriteIds.first())

        source.setFavorite(productId = 7, favorite = false)
        assertEquals(setOf(12), source.favoriteIds.first())
    }

    @Test
    fun `server refresh replaces stale IDs and discards invalid values`() = runTest {
        val source = DataStoreFavoriteLocalDataSource(InMemoryPreferencesDataStore())

        source.setFavorite(productId = 99, favorite = true)
        source.replaceFavoriteIds(setOf(-1, 4, 8))

        assertEquals(setOf(4, 8), source.favoriteIds.first())
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val preferences = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = preferences

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences = transform(preferences.value).also { preferences.value = it }
}
