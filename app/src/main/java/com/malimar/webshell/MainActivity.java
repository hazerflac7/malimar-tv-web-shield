package com.malimar.webshell;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private WebView web;
    private PlayerView playerView;
    private ExoPlayer player;

    private static final String HOME =
            "https://hazerflac7.github.io/malimar-tv-web/";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        web = findViewById(R.id.web);
        playerView = findViewById(R.id.player);

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

        web.addJavascriptInterface(new AndroidBridge(), "AndroidPlayer");

        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus(View.FOCUS_DOWN);

        web.loadUrl(HOME);
    }

    public class AndroidBridge {

        @JavascriptInterface
        public void play(String title, String streamUrl, String token) {
            if (streamUrl == null || streamUrl.isEmpty()) return;

            new Thread(() -> {
                try {
                    String resolved = resolveStream(streamUrl, token);

                    runOnUiThread(() -> startPlayer(resolved));

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "Playback error: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }
            }).start();
        }
    }

    private String resolveStream(String streamUrl, String token)
            throws Exception {

        String resolver;

        if (token == null || token.equalsIgnoreCase("None")) {
            resolver = "https://notoken.malimarcdn.com/";
        } else if (token.equalsIgnoreCase("Fastly")) {
            resolver = "https://fastlytoken.malimarcdn.com/";
        } else if (token.equalsIgnoreCase("Cloudflare")) {
            resolver = "https://cloudflaretoken.malimarcdn.com/";
        } else {
            throw new Exception("Unknown MTOKEN: " + token);
        }

        String requestUrl =
                resolver + "?url=" +
                URLEncoder.encode(streamUrl, "UTF-8");

        HttpURLConnection c =
                (HttpURLConnection) new URL(requestUrl).openConnection();

        c.setRequestMethod("GET");
        c.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
        );
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);

        int code = c.getResponseCode();

        if (code != 200) {
            c.disconnect();
            throw new Exception("Resolver HTTP " + code);
        }

        BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(c.getInputStream())
                );

        StringBuilder out = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null)
            out.append(line);

        br.close();
        c.disconnect();

        JSONObject json = new JSONObject(out.toString());

        String result = json.getString("url");

        if (result == null || result.isEmpty())
            throw new Exception("Resolver returned no URL");

        return result;
    }

    private void startPlayer(String url) {

        if (player != null) {
            player.release();
            player = null;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem item = MediaItem.fromUri(url);

        player.setMediaItem(item);
        player.prepare();
        player.play();

        web.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);
        playerView.requestFocus();
    }

    private void closePlayer() {

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        playerView.setPlayer(null);
        playerView.setVisibility(View.GONE);

        web.setVisibility(View.VISIBLE);
        web.requestFocus(View.FOCUS_DOWN);
    }

    private boolean sendJsKey(String key) {

        String safe = key.replace("'", "\\'");

        String js =
                "(function(){var el=document.activeElement||document.body;" +
                "var o={key:'"+safe+"',code:'"+safe+
                "',bubbles:true,cancelable:true};" +
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
                "if(typeof el.click==='function')el.click();" +
                "}})();";

        web.evaluateJavascript(js, null);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {

        if (e.getAction() != KeyEvent.ACTION_DOWN)
            return super.dispatchKeyEvent(e);

        /*
         * When native video is open, let PlayerView handle
         * normal D-pad controls. BACK closes playback.
         */
        if (playerView.getVisibility() == View.VISIBLE) {

            if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                closePlayer();
                return true;
            }

            return super.dispatchKeyEvent(e);
        }

        switch (e.getKeyCode()) {

            case KeyEvent.KEYCODE_DPAD_UP:
                return sendJsKey("ArrowUp");

            case KeyEvent.KEYCODE_DPAD_DOWN:
                return sendJsKey("ArrowDown");

            case KeyEvent.KEYCODE_DPAD_LEFT:
                return sendJsKey("ArrowLeft");

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return sendJsKey("ArrowRight");

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return activateFocused();

            case KeyEvent.KEYCODE_BACK:

                web.evaluateJavascript(
                        "(function(){" +
                        "if(typeof goBack==='function'){" +
                        "goBack();return 'handled';}" +
                        "var b=document.querySelector('#close');" +
                        "if(b){b.click();return 'handled';}" +
                        "return 'none';" +
                        "})()",
                        null
                );

                return true;

            default:
                return super.dispatchKeyEvent(e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (player != null)
            player.pause();
    }

    @Override
    protected void onDestroy() {

        if (player != null)
            player.release();

        if (web != null)
            web.destroy();

        super.onDestroy();
    }
}
