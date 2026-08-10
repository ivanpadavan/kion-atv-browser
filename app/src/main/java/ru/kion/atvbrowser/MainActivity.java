package ru.kion.atvbrowser;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public final class MainActivity extends Activity {
    private static final String HOME_URL = "https://hkion.kion.ru";
    private static final String PREFERENCES = "browser";
    private static final String STARTUP_URL = "startup_url";

    private WebView webView;
    private LinearLayout addressBar;
    private EditText addressInput;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int consecutiveBackPresses;
    private boolean suppressBackRelease;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemUi();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        webView = createWebView();
        root.addView(webView, matchParent());

        addressBar = createAddressBar();
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72),
                Gravity.TOP
        );
        barParams.setMargins(dp(24), dp(20), dp(24), 0);
        root.addView(addressBar, barParams);
        setContentView(root);

        String startupUrl = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                .getString(STARTUP_URL, HOME_URL);
        String restoredUrl = savedInstanceState == null ? null : savedInstanceState.getString("url");
        openUrl(restoredUrl == null ? startupUrl : restoredUrl, false);
    }

    private WebView createWebView() {
        WebView view = new WebView(this);
        view.setBackgroundColor(Color.BLACK);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);

        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        view.setInitialScale(scaleForFullHdCanvas());

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(view, true);

        view.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView target, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                return !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
            }

            @Override
            public void onPageFinished(WebView target, String url) {
                addressInput.setText(url);
                installTransparentVideoPosters();
            }
        });

        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
                        return;
                    }
                }
                request.deny();
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                ViewGroup root = findViewById(android.R.id.content);
                root.addView(view, matchParent());
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });

        return view;
    }

    private void installTransparentVideoPosters() {
        webView.evaluateJavascript(
                "(() => {"
                        + "const poster='a.jpg';"
                        + "const cleanVideo=video=>{"
                        + "if(video.getAttribute('poster')!==poster)video.setAttribute('poster',poster);"
                        + "};"
                        + "const cleanTree=node=>{"
                        + "if(node instanceof HTMLVideoElement)cleanVideo(node);"
                        + "if(node.querySelectorAll)node.querySelectorAll('video').forEach(cleanVideo);"
                        + "};"
                        + "cleanTree(document);"
                        + "if(window.__atvPosterObserver)window.__atvPosterObserver.disconnect();"
                        + "const observer=new MutationObserver(records=>records.forEach(record=>{"
                        + "if(record.type==='attributes')cleanVideo(record.target);"
                        + "record.addedNodes.forEach(cleanTree);"
                        + "}));"
                        + "observer.observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['poster']});"
                        + "window.__atvPosterObserver=observer;"
                        + "})()",
                null
        );
    }

    private LinearLayout createAddressBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(Color.rgb(28, 29, 36));

        addressInput = new EditText(this);
        addressInput.setSingleLine(true);
        addressInput.setTextColor(Color.WHITE);
        addressInput.setHintTextColor(Color.GRAY);
        addressInput.setHint(R.string.url_hint);
        addressInput.setText(HOME_URL);
        addressInput.setSelectAllOnFocus(true);
        addressInput.setTextSize(17);
        bar.addView(addressInput, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button go = makeButton(R.string.go);
        go.setOnClickListener(ignored -> openFromInput());
        bar.addView(go, new LinearLayout.LayoutParams(dp(130), dp(52)));

        Button close = makeButton(R.string.close);
        close.setContentDescription("Закрыть адресную строку");
        close.setOnClickListener(ignored -> hideAddressBar());
        bar.addView(close, new LinearLayout.LayoutParams(dp(80), dp(52)));

        addressInput.setOnEditorActionListener((ignored, actionId, event) -> {
            openFromInput();
            return true;
        });
        return bar;
    }

    private Button makeButton(int text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        return button;
    }

    private void openFromInput() {
        openUrl(addressInput.getText().toString(), true);
    }

    private void openUrl(String rawUrl, boolean saveAsStartupUrl) {
        String value = rawUrl == null ? "" : rawUrl.trim();
        if (value.isEmpty()) {
            value = HOME_URL;
        }
        Uri parsed = Uri.parse(value);
        if (parsed.getScheme() == null) {
            value = "https://" + value;
        }
        if (saveAsStartupUrl) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE)
                    .edit()
                    .putString(STARTUP_URL, value)
                    .apply();
        }
        addressInput.setText(value);
        webView.loadUrl(value);
        hideAddressBar();
    }

    private void showAddressBar() {
        addressBar.setVisibility(View.VISIBLE);
        addressInput.requestFocus();
    }

    private void hideAddressBar() {
        addressBar.setVisibility(View.GONE);
        webView.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            consecutiveBackPresses = 0;
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (addressBar.getVisibility() == View.VISIBLE) {
                hideAddressBar();
            } else {
                showAddressBar();
            }
            return true;
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (suppressBackRelease && event.getAction() == KeyEvent.ACTION_UP) {
                suppressBackRelease = false;
                return true;
            }
            if (customView != null) {
                consecutiveBackPresses = 0;
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    hideCustomView();
                }
                return true;
            }
            if (addressBar.getVisibility() == View.VISIBLE) {
                consecutiveBackPresses = 0;
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    hideAddressBar();
                }
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                consecutiveBackPresses++;
                if (consecutiveBackPresses == 5) {
                    consecutiveBackPresses = 0;
                    suppressBackRelease = true;
                    showAddressBar();
                    return true;
                }
            }
            KeyEvent escape = new KeyEvent(
                    event.getDownTime(),
                    event.getEventTime(),
                    event.getAction(),
                    KeyEvent.KEYCODE_ESCAPE,
                    event.getRepeatCount(),
                    event.getMetaState(),
                    event.getDeviceId(),
                    event.getScanCode(),
                    event.getFlags(),
                    event.getSource()
            );
            webView.dispatchKeyEvent(escape);
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        ViewGroup root = findViewById(android.R.id.content);
        root.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        webView.requestFocus();
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getDecorView().getWindowInsetsController().hide(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars()
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("url", webView.getUrl());
    }

    @Override
    protected void onPause() {
        webView.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        hideSystemUi();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int scaleForFullHdCanvas() {
        float screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int scale = Math.round(screenWidthPx * 100f / (1920f * density));
        return Math.max(25, Math.min(scale, 200));
    }
}
