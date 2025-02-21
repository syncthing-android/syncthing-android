package com.latec.syncthinglegacy.activities;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Proxy;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.latec.syncthinglegacy.R;
import com.latec.syncthinglegacy.databinding.ActivityWebGuiBinding;
import com.latec.syncthinglegacy.service.Constants;
import com.latec.syncthinglegacy.service.SyncthingService;
import com.latec.syncthinglegacy.service.SyncthingServiceBinder;
import com.latec.syncthinglegacy.util.ConfigXml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Holds a WebView that shows the web ui of the local syncthing instance.
 */
public class WebGuiActivity extends StateDialogActivity
        implements SyncthingService.OnServiceStateChangeListener {

    private static final String TAG = "WebGuiActivity";

    private X509Certificate mCaCert;

    private ConfigXml mConfig;

    private ActivityWebGuiBinding binding;

    /**
     * Hides the loading screen and shows the WebView once it is fully loaded.
     */
    private final WebViewClient mWebViewClient = new WebViewClient() {

        /**
         * Catch (self-signed) SSL errors and test if they correspond to Syncthing's certificate.
         */
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            try {
                // Use reflection to access the private mX509Certificate field of SslCertificate
                SslCertificate sslCert = error.getCertificate();
                Field f = sslCert.getClass().getDeclaredField("mX509Certificate");
                f.setAccessible(true);
                X509Certificate cert = (X509Certificate)f.get(sslCert);
                if (cert == null) {
                    Log.w(TAG, "X509Certificate reference invalid");
                    handler.cancel();
                    return;
                }
                cert.verify(mCaCert.getPublicKey());
                handler.proceed();
            } catch (NoSuchFieldException|IllegalAccessException|CertificateException|
                    NoSuchAlgorithmException|InvalidKeyException|NoSuchProviderException|
                    SignatureException e) {
                Log.w(TAG, e);
                handler.cancel();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if(uri.getHost().equals(getService().getWebGuiUrl().getHost())) {
                return false;
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            binding.webview.setVisibility(View.VISIBLE);
            binding.loading.setVisibility(View.GONE);
        }
    };

    /**
     * Initialize WebView.
     *
     * Ignore lint javascript warning as js is loaded only from our known, local service.
     */
    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebGuiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mConfig = new ConfigXml(this);
        loadCaCert();

        binding.webview.getSettings().setJavaScriptEnabled(true);
        binding.webview.getSettings().setDomStorageEnabled(true);
        binding.webview.setWebViewClient(mWebViewClient);
        binding.webview.clearCache(true);

        // SyncthingService needs to be started from this activity as the user
        // can directly launch this activity from the recent activity switcher.
        Intent serviceIntent = new Intent(this, SyncthingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        super.onServiceConnected(componentName, iBinder);
        SyncthingServiceBinder syncthingServiceBinder = (SyncthingServiceBinder) iBinder;
        syncthingServiceBinder.getService().registerOnServiceStateChangeListener(this);
    }

    @Override
    public void onServiceStateChange(SyncthingService.State newState) {
        Log.v(TAG, "onServiceStateChange(" + newState + ")");
        if (newState == SyncthingService.State.ACTIVE) {
            if (binding.webview == null) {
                Log.v(TAG, "onWebGuiAvailable: Skipped event due to mWebView == null");
                return;
            }
            if (binding.webview.getUrl() == null) {
                binding.webview.stopLoading();
                setWebViewProxy(binding.webview.getContext().getApplicationContext(), "", 0, "localhost|0.0.0.0|127.*|[::1]");
                String credentials = mConfig.getUserName() + ":" + mConfig.getApiKey();
                String b64Credentials = Base64.encodeToString(credentials.getBytes(UTF_8), Base64.NO_WRAP);
                Map<String,String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + b64Credentials);
                binding.webview.loadUrl(getService().getWebGuiUrl().toString(), headers);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack();
        } else {
            finish();
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        binding.webview.onPause();
        binding.webview.pauseTimers();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.webview.resumeTimers();
        binding.webview.onResume();
    }

    @Override
    protected void onDestroy() {
        SyncthingService mSyncthingService = getService();
        if (mSyncthingService != null) {
            mSyncthingService.unregisterOnServiceStateChangeListener(this);
        }
        binding.webview.destroy();
        super.onDestroy();
    }

    /**
     * Reads the SyncthingService.HTTPS_CERT_FILE Ca Cert key and loads it in memory
     */
    private void loadCaCert() {
        InputStream inStream = null;
        File httpsCertFile = Constants.getHttpsCertFile(this);
        if (!httpsCertFile.exists()) {
            Toast.makeText(WebGuiActivity.this, R.string.config_file_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            inStream = new FileInputStream(httpsCertFile);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            mCaCert = (X509Certificate)
                    cf.generateCertificate(inStream);
        } catch (FileNotFoundException|CertificateException e) {
            throw new IllegalArgumentException("Untrusted Certificate", e);
        } finally {
            try {
                if (inStream != null)
                    inStream.close();
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Set webview proxy and sites that are not retrieved using proxy.
     * Compatible with KitKat or higher android version.
     * Returns boolean if successful.
     * Source: https://stackoverflow.com/a/26781539
     */
    @SuppressLint("PrivateApi")
    public static boolean setWebViewProxy(Context appContext, String host, int port, String exclusionList) {
        Properties properties = System.getProperties();
        properties.setProperty("http.proxyHost", host);
        properties.setProperty("http.proxyPort", Integer.toString(port));
        properties.setProperty("https.proxyHost", host);
        properties.setProperty("https.proxyPort", Integer.toString(port));
        properties.setProperty("http.nonProxyHosts", exclusionList);
        properties.setProperty("https.nonProxyHosts", exclusionList);

        try {
            Class applictionCls = Class.forName("android.app.Application");
            Field loadedApkField = applictionCls.getDeclaredField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(appContext);
            Class loadedApkCls = Class.forName("android.app.LoadedApk");
            Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                    Class clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);

                        String CLASS_NAME;
                        CLASS_NAME = "android.net.ProxyInfo";
                        Class cls = Class.forName(CLASS_NAME);
                        Constructor constructor = cls.getConstructor(String.class, Integer.TYPE, String.class);
                        constructor.setAccessible(true);
                        Object proxyProperties = constructor.newInstance(host, port, exclusionList);
                        intent.putExtra("proxy", (Parcelable) proxyProperties);

                        onReceiveMethod.invoke(rec, appContext, intent);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "setWebViewProxy exception", e);
        }
        return false;
    }
}
