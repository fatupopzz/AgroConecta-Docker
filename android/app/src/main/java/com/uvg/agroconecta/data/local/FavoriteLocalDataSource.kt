package com.uvg.agroconecta.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FavoriteLocalDataSource {
    fun favoriteIds(userId: Int): Flow<Set<Int>>

    suspend fun setFavorite(userId: Int, productId: Int, favorite: Boolean)

    suspend fun replaceFavoriteIds(userId: Int, productIds: Set<Int>)
}

class DataStoreFavoriteLocalDataSource internal constructor(
    private val dataStore: DataStore<Preferences>
) : FavoriteLocalDataSource {

    override fun favoriteIds(userId: Int): Flow<Set<Int>> {
        require(userId > 0) { "userId debe ser positivo" }
        val key = favoriteIdsKey(userId)
        return dataStore.data.map { preferences ->
            preferences[key]
                .orEmpty()
                .mapNotNull(String::toIntOrNull)
                .filterTo(mutableSetOf()) { it > 0 }
        }
    }

    override suspend fun setFavorite(userId: Int, productId: Int, favorite: Boolean) {
        require(userId > 0) { "userId debe ser positivo" }
        require(productId > 0) { "productId debe ser positivo" }
        val key = favoriteIdsKey(userId)

        dataStore.edit { preferences ->
            val ids = preferences[key]
                .orEmpty()
                .toMutableSet()
            if (favorite) {
                ids += productId.toString()
            } else {
                ids -= productId.toString()
            }
            preferences[key] = ids
        }
    }

    override suspend fun replaceFavoriteIds(userId: Int, productIds: Set<Int>) {
        require(userId > 0) { "userId debe ser positivo" }
        val key = favoriteIdsKey(userId)

        dataStore.edit { preferences ->
            preferences[key] = productIds
                .asSequence()
                .filter { it > 0 }
                .map(Int::toString)
                .toSet()
        }
    }

    private companion object {
        fun favoriteIdsKey(userId: Int) =
            stringSetPreferencesKey("favorite_product_ids_$userId")
    }
}
