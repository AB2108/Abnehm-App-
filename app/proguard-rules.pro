# Keep the JavaScript interface used by the WebView bridge.
-keepclassmembers class de.abnehm.app.** {
    @android.webkit.JavascriptInterface <methods>;
}
