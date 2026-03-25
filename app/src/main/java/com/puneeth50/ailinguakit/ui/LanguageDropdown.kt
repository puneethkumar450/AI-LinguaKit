package com.puneeth50.ailinguakit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.puneeth50.ailinguakit.ui.theme.TranslateAppTheme
import com.puneeth50.ailinguakit.viewmodel.LanguageOption
import com.puneeth50.ailinguakit.viewmodel.SUPPORTED_LANGUAGES

@Composable
fun LanguageDropdown(
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier,
    options: List<LanguageOption> = SUPPORTED_LANGUAGES
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 0.6f

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart).padding(horizontal = 4.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = ButtonDefaults.TextButtonContentPadding
        ) {
            Text(selected.displayName(context), style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = maxHeight)
        ) {
            options.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName(context)) },
                    onClick = {
                        onSelect(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageDropdownPreview() {
    TranslateAppTheme {
        LanguageDropdown(
            selected = SUPPORTED_LANGUAGES[0],
            onSelect = {}
        )
    }
}
