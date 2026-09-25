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
            "(function(){var el=document.activeElement||document.body;" +
            "var o={key:'"+safe+"',code:'"+safe+"',bubbles:true,cancelable:true};" +
            "el.dispatchEvent(new KeyboardEvent('keydown',o));" +
            "el.dispatchEvent(new KeyboardEvent('keyup',o));})();";
        web.evaluateJavascript(js, null);
        return true;
    }

    private boolean activateFocused() {
        String js =
            "(function(){var el=document.activeElement;" +
            "if(el&&el!==document.body){" +
            "var o={key:'Enter',code:'Enter',bubbles:true,cancelable:true};" +
            "el.dispatchEvent(new KeyboardEvent('keydown',o));" +
            "el.dispatchEvent(new KeyboardEvent('keyup',o));" +
            "if(typeof el.click==='function')el.click();}})();";
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
            case KeyEvent.KEYCODE_NUMPAD_ENTER: return activateFocused();
            case KeyEvent.KEYCODE_BACK:
                // Use the web app's own Back behavior first. This mirrors the
                // visible Back button that already works correctly on Shield.
                web.evaluateJavascript(
                    "(function(){if(typeof goBack==='function'){goBack();return 'handled';}" +
                    "var b=document.querySelector('#close');if(b){b.click();return 'handled';}" +
                    "return 'none';})()", null);
                return true;
            default: return super.dispatchKeyEvent(e);
        }
    }

    @Override protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
