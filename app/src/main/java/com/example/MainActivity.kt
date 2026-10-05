package com.example

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FreshBluePrimary
import com.example.ui.theme.FreshGreenAccent
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        FreshCareApp()
      }
    }
  }
}

enum class ActiveTab {
  PORTAL_PREVIEW,
  SOURCE_CODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshCareApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var activeTab by remember { mutableStateOf(ActiveTab.PORTAL_PREVIEW) }
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var canGoBack by remember { mutableStateOf(false) }

  // Read the HTML source code from assets
  val htmlCode by remember {
    mutableStateOf(
      try {
        context.assets.open("freshcare.html").bufferedReader().use { it.readText() }
      } catch (e: Exception) {
        "<!-- Error loading freshcare.html: ${e.localizedMessage} -->"
      }
    )
  }

  // Handle hardware / gesture back button
  BackHandler(enabled = (activeTab == ActiveTab.PORTAL_PREVIEW && canGoBack)) {
    webViewInstance?.goBack()
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FreshBluePrimary.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = FreshBluePrimary,
                modifier = Modifier.size(18.dp)
              )
            }
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Fresh",
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = FreshBluePrimary
                )
                Text(
                  text = "Care",
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = FreshGreenAccent
                )
              }
              Text(
                text = "Corporate Portal",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          if (activeTab == ActiveTab.PORTAL_PREVIEW) {
            // Quick autofill demo button
            IconButton(
              onClick = {
                webViewInstance?.evaluateJavascript("autofillDemoCredentials();", null)
                scope.launch {
                  snackbarHostState.showSnackbar("Prefilled demo credentials into login card.")
                }
              },
              modifier = Modifier.testTag("autofill_action_button")
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Autofill Demo Credentials",
                tint = FreshBluePrimary
              )
            }

            // Reload button
            IconButton(
              onClick = {
                webViewInstance?.reload()
                scope.launch {
                  snackbarHostState.showSnackbar("Portal reloaded.")
                }
              },
              modifier = Modifier.testTag("reload_action_button")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reload Portal"
              )
            }
          } else {
            // Copy HTML code button
            IconButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("FreshCare HTML", htmlCode)
                clipboard.setPrimaryClip(clip)
                scope.launch {
                  snackbarHostState.showSnackbar("FreshCare HTML code copied to clipboard!")
                }
              },
              modifier = Modifier.testTag("copy_code_action_button")
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy HTML Source"
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("navigation_bar")
      ) {
        NavigationBarItem(
          selected = activeTab == ActiveTab.PORTAL_PREVIEW,
          onClick = { activeTab = ActiveTab.PORTAL_PREVIEW },
          icon = { Icon(Icons.Default.Language, contentDescription = null) },
          label = { Text("Live Portal") },
          modifier = Modifier.testTag("nav_portal_preview")
        )
        NavigationBarItem(
          selected = activeTab == ActiveTab.SOURCE_CODE,
          onClick = { activeTab = ActiveTab.SOURCE_CODE },
          icon = { Icon(Icons.Default.Code, contentDescription = null) },
          label = { Text("HTML Source") },
          modifier = Modifier.testTag("nav_source_code")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (activeTab) {
        ActiveTab.PORTAL_PREVIEW -> {
          FreshCareWebView(
            onWebViewCreated = { wv ->
              webViewInstance = wv
            },
            onHistoryStateChanged = { canBack ->
              canGoBack = canBack
            }
          )
        }
        ActiveTab.SOURCE_CODE -> {
          FreshCareCodeViewer(
            htmlCode = htmlCode,
            onCopyClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("FreshCare HTML", htmlCode)
              clipboard.setPrimaryClip(clip)
              scope.launch {
                snackbarHostState.showSnackbar("Complete HTML code copied to clipboard!")
              }
            }
          )
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FreshCareWebView(
  onWebViewCreated: (WebView) -> Unit,
  onHistoryStateChanged: (Boolean) -> Unit
) {
  AndroidView(
    factory = { ctx ->
      WebView(ctx).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.displayZoomControls = false
        settings.builtInZoomControls = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webViewClient = object : WebViewClient() {
          override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            onHistoryStateChanged(view?.canGoBack() ?: false)
          }
        }

        webChromeClient = WebChromeClient()

        loadUrl("file:///android_asset/freshcare.html")
        onWebViewCreated(this)
      }
    },
    modifier = Modifier
      .fillMaxSize()
      .testTag("freshcare_webview")
  )
}

@Composable
fun FreshCareCodeViewer(
  htmlCode: String,
  onCopyClick: () -> Unit
) {
  val verticalScroll = rememberScrollState()
  val horizontalScroll = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0F172A))
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "freshcare.html",
          color = Color(0xFF38BDF8),
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "Single-file HTML + embedded CSS & JavaScript",
          color = Color(0xFF94A3B8),
          fontSize = 12.sp
        )
      }

      Button(
        onClick = onCopyClick,
        colors = ButtonDefaults.buttonColors(containerColor = FreshBluePrimary),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.testTag("code_copy_button")
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy All", fontSize = 13.sp, color = Color.White)
      }
    }

    Card(
      modifier = Modifier.fillMaxSize(),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
      shape = RoundedCornerShape(8.dp)
    ) {
      SelectionContainer {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
            .horizontalScroll(horizontalScroll)
            .padding(16.dp)
        ) {
          Text(
            text = htmlCode,
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}
