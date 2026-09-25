package com.malimar.webshell;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

public class MainActivity extends Activity {
    private WebView web;
    private static final String HOME = "http://192.168.1.176:8080/";

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

        web.setWebViewClient(new WebViewClient() {
            private boolean handleUrl(String url) {
                if (url == null) return false;

                try {
                    if (url.startsWith("malimar://")) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.setPackage("com.malimar.webtv");
                        startActivity(i);
                        return true;
                    }

                    if (url.startsWith("intent://")) {
                        Intent i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        i.setPackage("com.malimar.webtv");
                        startActivity(i);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    android.webkit.WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
        });
        web.setWebChromeClient(new WebChromeClient());

        web.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void playPremiumChannel(String id, String grid) {
                runOnUiThread(() -> {
                    try {
                        String uri = "malimar://channel?id=" +
                                Uri.encode(id) +
                                "&grid=" +
                                Uri.encode(grid);

                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        i.setPackage("com.malimar.webtv");
                        startActivity(i);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }, "MalimarAndroid");
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
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK &&
            e.getAction() == KeyEvent.ACTION_DOWN) {

            web.evaluateJavascript(
                "(function(){if(typeof goBack==='function'){goBack();return 'handled';}" +
                "if(history.length>1){history.back();return 'handled';}" +
                "return 'none';})()", null);
            return true;
        }

        // Let WebView receive the real Shield/Android TV
        // DPAD and Enter events.
        return super.dispatchKeyEvent(e);
    }

    @Override protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
