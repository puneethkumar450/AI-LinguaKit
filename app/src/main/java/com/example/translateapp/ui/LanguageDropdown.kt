package com.example.translateapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.translateapp.ui.theme.TranslateAppTheme
import com.example.translateapp.viewmodel.LanguageOption
import com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES

@Composable
fun LanguageDropdown(
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier,
    options: List<LanguageOption> = SUPPORTED_LANGUAGES
) {
    var expanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 0.6f

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart).padding(horizontal = 4.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = ButtonDefaults.TextButtonContentPadding
        ) {
            Text(selected.displayName, style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = maxHeight)
        ) {
            options.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName) },
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
