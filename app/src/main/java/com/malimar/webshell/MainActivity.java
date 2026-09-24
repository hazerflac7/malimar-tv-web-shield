package com.malimar.webshell;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private static final String HOME = "https://hazerflac7.github.io/malimar-tv-web/";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(com.malimar.webshell.R.layout.activity_main);
        web = findViewById(com.malimar.webshell.R.id.web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus(View.FOCUS_DOWN);
        web.loadUrl(HOME);
    }

    private boolean sendJsKey(String key) {
        String safe = key.replace("'", "\\'");
        String js =
            "(function(){var o={key:'"+safe+"',code:'"+safe+"',bubbles:true,cancelable:true};" +
            "document.dispatchEvent(new KeyboardEvent('keydown',o));" +
            "document.activeElement&&document.activeElement.dispatchEvent(new KeyboardEvent('keydown',o));" +
            "document.dispatchEvent(new KeyboardEvent('keyup',o));})();";
        web.evaluateJavascript(js, null);
        return true;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(e);
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP: return sendJsKey("ArrowUp");
            case KeyEvent.KEYCODE_DPAD_DOWN: return sendJsKey("ArrowDown");
            case KeyEvent.KEYCODE_DPAD_LEFT: return sendJsKey("ArrowLeft");
            case KeyEvent.KEYCODE_DPAD_RIGHT: return sendJsKey("ArrowRight");
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER: return sendJsKey("Enter");
            case KeyEvent.KEYCODE_BACK:
                if (web.canGoBack()) { web.goBack(); return true; }
                return super.dispatchKeyEvent(e);
            default: return super.dispatchKeyEvent(e);
        }
    }

    @Override protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
