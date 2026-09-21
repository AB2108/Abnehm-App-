package de.abnehm.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Android-Hülle für die Ein-Datei-Abnehm-App.
 *
 * Die komplette App (HTML + CSS + JS) liegt unverändert unter
 * assets/abnehm-app.html und wird hier in einem WebView angezeigt, damit
 * Aussehen und Verhalten 1:1 der Web-Version entsprechen. localStorage bleibt
 * lokal auf dem Gerät erhalten; Backup-Export/-Import werden nativ überbrückt.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Callback des <input type="file"> für den Backup-Import.
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Zwischengespeicherter Base64-Export, falls erst noch die Schreib-
    // berechtigung (nur Android <= 9) erteilt werden muss.
    private var pendingBackup: Triple<String, String, String>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = filePathCallback
            filePathCallback = null
            if (callback == null) return@registerForActivityResult
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            callback.onReceiveValue(uris)
        }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val backup = pendingBackup
            pendingBackup = null
            if (granted && backup != null) {
                writeBackup(backup.first, backup.second, backup.third)
            } else if (!granted) {
                toast(getString(R.string.backup_permission_denied))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        WebView.setWebContentsDebuggingEnabled(false)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true // localStorage
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    fileChooserLauncher.launch(params?.createIntent())
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }

        // Backup-Brücke: JavaScript reicht den erzeugten Blob als Base64 herein.
        webView.addJavascriptInterface(BackupBridge(), "AndroidBackup")

        // Der Export erzeugt eine blob:-URL mit a.download. WebView kann diese
        // nicht selbst herunterladen – wir lesen den Blob per JS aus und
        // speichern ihn nativ in den Download-Ordner.
        webView.setDownloadListener { url, _, _, mimeType, _ ->
            if (url.startsWith("blob:")) {
                fetchBlobAndSave(url, mimeType)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/abnehm-app.html")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    private fun fetchBlobAndSave(blobUrl: String, mimeType: String) {
        val mime = if (mimeType.isNullOrBlank()) "application/json" else mimeType
        val js = """
            (function() {
              var xhr = new XMLHttpRequest();
              xhr.open('GET', '$blobUrl', true);
              xhr.responseType = 'blob';
              xhr.onload = function() {
                if (this.status === 200) {
                  var reader = new FileReader();
                  reader.onloadend = function() {
                    var data = reader.result;
                    var base64 = data.substring(data.indexOf(',') + 1);
                    AndroidBackup.saveBase64(base64, '$mime');
                  };
                  reader.readAsDataURL(this.response);
                }
              };
              xhr.send();
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    inner class BackupBridge {
        @JavascriptInterface
        fun saveBase64(base64: String, mimeType: String) {
            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())
            val fileName = "abnehm-backup-$stamp.json"
            runOnUiThread {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingBackup = Triple(base64, fileName, mimeType)
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    writeBackup(base64, fileName, mimeType)
                }
            }
        }
    }

    private fun writeBackup(base64: String, fileName: String, mimeType: String) {
        val bytes = try {
            Base64.decode(base64, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            toast(getString(R.string.backup_failed))
            return
        }

        val ok = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    true
                } else false
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                FileOutputStream(File(dir, fileName)).use { it.write(bytes) }
                true
            }
        } catch (e: Exception) {
            false
        }

        toast(if (ok) getString(R.string.backup_saved, fileName) else getString(R.string.backup_failed))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
