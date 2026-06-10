package pe.themaro.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://themaro.pe/";

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout offlineLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String lastUrl = HOME_URL;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        offlineLayout = findViewById(R.id.offlineLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> reloadPage());

        setupWebView();
        setupSwipeRefresh();

        if (isNetworkAvailable()) {
            loadWebsite(HOME_URL);
        } else {
            showOfflineScreen();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // JavaScript habilitado (necesario para WooCommerce)
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Soporte para medios y contenido web
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setAllowFileAccess(true);

        // Rendimiento y caché
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // User Agent personalizado para TheMaro App
        String userAgent = settings.getUserAgentString() + " TheMaro-Android-App/1.0";
        settings.setUserAgentString(userAgent);

        // Zoom
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                lastUrl = url;
                progressBar.setVisibility(View.VISIBLE);
                hideOfflineScreen();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Abrir WhatsApp, Facebook, Instagram, etc. en app externa
                if (url.startsWith("whatsapp://") ||
                        url.startsWith("tel:") ||
                        url.startsWith("mailto:") ||
                        url.startsWith("fb://") ||
                        url.startsWith("instagram://") ||
                        url.startsWith("tiktok://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        // Ignorar si no hay app disponible
                    }
                    return true;
                }

                // Redes sociales: abrir en navegador externo
                if (url.contains("facebook.com") ||
                        url.contains("instagram.com") ||
                        url.contains("twitter.com") ||
                        url.contains("x.com") ||
                        url.contains("tiktok.com") ||
                        url.contains("youtube.com") ||
                        url.contains("pinterest.com")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                // Todo lo de themaro.pe carga en el WebView
                if (url.contains("themaro.pe")) {
                    if (!isNetworkAvailable()) {
                        showOfflineScreen();
                        return true;
                    }
                    return false;
                }

                // Otros enlaces externos: abrir en navegador
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (!isNetworkAvailable()) {
                    showOfflineScreen();
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.themaro_primary, getTheme()),
                getResources().getColor(R.color.themaro_secondary, getTheme())
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                reloadPage();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                showOfflineScreen();
            }
        });
    }

    private void loadWebsite(String url) {
        hideOfflineScreen();
        webView.loadUrl(url);
    }

    private void reloadPage() {
        if (isNetworkAvailable()) {
            hideOfflineScreen();
            webView.reload();
            if (webView.getUrl() == null) {
                webView.loadUrl(lastUrl);
            }
        } else {
            showOfflineScreen();
        }
    }

    private void showOfflineScreen() {
        webView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        offlineLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void hideOfflineScreen() {
        offlineLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo activeInfo = cm.getActiveNetworkInfo();
            return activeInfo != null && activeInfo.isConnected();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}
