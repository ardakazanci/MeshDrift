package com.ardakazanci.mesh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ardakazanci.mesh.ui.theme.MeshTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshTheme(
                darkTheme = true,
                dynamicColor = false
            ) {
                MeshShaderScreen()
            }
        }
    }
}
