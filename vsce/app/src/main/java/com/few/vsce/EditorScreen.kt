package com.few.vsce

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.IThemeSource
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var repoUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var localDir by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("Main.kt") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Siap. Clone repo terlebih dahulu.") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { initTextMate(context) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = repoUrl,
            onValueChange = { repoUrl = it },
            label = { Text("URL Repo GitHub") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Personal Access Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        status = "Mengkloning..."
                        try {
                            val dir = File(context.filesDir, "repo_${System.currentTimeMillis()}")
                            withContext(Dispatchers.IO) {
                                GitHelper.clone(repoUrl, dir, token)
                            }
                            localDir = dir
                            val firstFile = withContext(Dispatchers.IO) {
                                dir.walkTopDown().firstOrNull {
                                    it.isFile && it.extension in listOf("kt","java","py","js","ts","md")
                                }
                            }
                            if (firstFile != null) {
                                fileName = firstFile.name
                                code = withContext(Dispatchers.IO) { firstFile.readText() }
                                status = "Clone berhasil. Edit kode lalu commit & push."
                            } else {
                                status = "Clone berhasil, tetapi tidak ada file kode yang dikenali."
                            }
                        } catch (e: Exception) {
                            status = "Clone gagal: ${e.message}"
                        }
                    }
                },
                enabled = repoUrl.isNotBlank() && localDir == null
            ) { Text("Clone") }

            Button(
                onClick = {
                    val dir = localDir ?: return@Button
                    scope.launch {
                        status = "Menyimpan & commit..."
                        try {
                            withContext(Dispatchers.IO) {
                                File(dir, fileName).writeText(code)
                                GitHelper.commitAndPush(dir, "Update $fileName via VSCE", token)
                            }
                            status = "Push berhasil! Perubahan sudah di GitHub."
                        } catch (e: Exception) {
                            status = "Push gagal: ${e.message}"
                        }
                    }
                },
                enabled = localDir != null
            ) { Text("Commit & Push") }
        }

        Spacer(Modifier.height(6.dp))
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        CodeEditorView(
            code = code,
            onCodeChange = { code = it },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

@Composable
fun CodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editor = remember {
        CodeEditor(context).apply {
            setText(code)
            post {
                runCatching {
                    colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                    setEditorLanguage(TextMateLanguage.create("source.kotlin", true))
                }
            }
        }
    }

    LaunchedEffect(code) {
        if (editor.text.toString() != code) {
            editor.setText(code)
        }
    }

    DisposableEffect(editor) {
        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                onCodeChange(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        editor.addTextChangedListener(watcher)
        onDispose {
            editor.removeTextChangedListener(watcher)
            editor.release()
        }
    }

    AndroidView(factory = { editor }, modifier = modifier)
}

private fun initTextMate(context: Context) {
    FileProviderRegistry.getInstance()
        .addFileProvider(AssetsFileResolver(context.applicationContext.assets))

    val themeRegistry = ThemeRegistry.getInstance()
    val themeName = "quietlight"
    val themePath = "textmate/theme/$themeName.json"
    runCatching {
        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream(themePath),
                    themePath,
                    null
                ),
                themeName
            ).apply { isDark = false }
        )
        themeRegistry.setTheme(themeName)
    }

    runCatching {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }
}