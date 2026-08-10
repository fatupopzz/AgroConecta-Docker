package com.uvg.agroconecta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.uvg.agroconecta.ui.navigation.AgroConectaNavHost
import com.uvg.agroconecta.ui.theme.AgroConectaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val initialTrackingOrderId = intent.getTrackingOrderId()

        setContent {
            AgroConectaTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AgroConectaNavHost(
                        navController = navController,
                        initialTrackingOrderId = initialTrackingOrderId
                    )
                }
            }
        }
    }

    private fun Intent.getTrackingOrderId(): Int? {
        val extraOrderId = getIntExtra("orderId", -1)
            .takeIf { it > 0 }
            ?: getIntExtra("order_id", -1).takeIf { it > 0 }

        return extraOrderId ?: data?.lastPathSegment?.toIntOrNull()
    }
}
