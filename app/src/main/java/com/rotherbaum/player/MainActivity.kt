package com.rotherbaum.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rotherbaum.player.ui.RotherbaumRoot
import com.rotherbaum.player.ui.theme.RotherbaumTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Ergebnis fliesst ueber den mutableState unten in die UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        setContent {
            var granted by mutableStateOf(
                checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )

            RotherbaumTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (granted) {
                        RotherbaumRoot()
                    } else {
                        PermissionRequestScreen(
                            onRequest = {
                                permissionLauncher.launch(permission)
                                granted = checkSelfPermission(permission) ==
                                    android.content.pm.PackageManager.PERMISSION_GRANTED
                            }
                        )
                    }
                }
            }
        }

        if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }
}

@Composable
private fun PermissionRequestScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rotherbaum braucht Zugriff auf deine Musik.")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRequest) {
            Text("Zugriff erlauben")
        }
    }
}
