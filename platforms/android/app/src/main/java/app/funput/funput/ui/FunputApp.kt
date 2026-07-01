package app.funput.funput.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.R
import app.funput.funput.ui.theme.FunputTheme

@Composable
fun FunputApp() {
    FunputTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FunputAppPreview() {
    FunputApp()
}
