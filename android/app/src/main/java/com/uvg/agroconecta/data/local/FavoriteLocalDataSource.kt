package com.uvg.agroconecta.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FavoriteLocalDataSource {
    val favoriteIds: Flow<Set<Int>>

    suspend fun setFavorite(productId: Int, favorite: Boolean)

    suspend fun replaceFavoriteIds(productIds: Set<Int>)
}

class DataStoreFavoriteLocalDataSource internal constructor(
    private val dataStore: DataStore<Preferences>
) : FavoriteLocalDataSource {

    override val favoriteIds: Flow<Set<Int>> = dataStore.data.map { preferences ->
        preferences[FAVORITE_PRODUCT_IDS]
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .filterTo(mutableSetOf()) { it > 0 }
    }

    override suspend fun setFavorite(productId: Int, favorite: Boolean) {
        require(productId > 0) { "productId debe ser positivo" }

        dataStore.edit { preferences ->
            val ids = preferences[FAVORITE_PRODUCT_IDS].orEmpty().toMutableSet()
            if (favorite) {
                ids += productId.toString()
            } else {
                ids -= productId.toString()
            }
            preferences[FAVORITE_PRODUCT_IDS] = ids
        }
    }

    override suspend fun replaceFavoriteIds(productIds: Set<Int>) {
        dataStore.edit { preferences ->
            preferences[FAVORITE_PRODUCT_IDS] = productIds
                .asSequence()
                .filter { it > 0 }
                .map(Int::toString)
                .toSet()
        }
    }

    private companion object {
        val FAVORITE_PRODUCT_IDS = stringSetPreferencesKey("favorite_product_ids")
    }
}
