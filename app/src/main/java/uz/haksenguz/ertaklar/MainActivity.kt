package uz.haksenguz.ertaklar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import uz.haksenguz.ertaklar.navigation.NavGraph
import uz.haksenguz.ertaklar.ui.theme.ErtaklarTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ErtaklarTheme {
                NavGraph()
            }
        }
    }
}
