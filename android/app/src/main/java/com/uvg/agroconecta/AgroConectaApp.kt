package com.uvg.agroconecta

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de Hilt. Genera el componente raiz (SingletonComponent)
 * del que cuelgan NetworkModule y los @HiltViewModel.
 */
@HiltAndroidApp
class AgroConectaApp : Application()
