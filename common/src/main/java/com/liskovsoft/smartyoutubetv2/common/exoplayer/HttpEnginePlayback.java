package com.liskovsoft.smartyoutubetv2.common.exoplayer;

import android.content.Context;
import android.net.http.HttpEngine;

import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Experimental Android 14+ HttpEngine transport for existing DASH/HLS/SABR sources.
 *
 * <p>It reuses DefaultHttpDataSource's header, Range, redirect and timeout handling.
 * Cronet and OkHttp remain available through the network engine preference.
 */
@RequiresApi(34)
final class HttpEnginePlayback {
    private static HttpEngine sEngine;

    private HttpEnginePlayback() {
    }

    private static synchronized HttpEngine getEngine(Context context) {
        if (sEngine == null) {
            sEngine = new HttpEngine.Builder(context.getApplicationContext())
                    .setEnableHttp2(true)
                    // The TCL transfer test favored HTTP/2 with QUIC disabled.
                    .setEnableQuic(false)
                    .setEnableBrotli(true)
                    .build();
        }
        return sEngine;
    }

    static HttpDataSource.Factory createFactory(Context context, String userAgent,
                                                int connectTimeoutMs, int readTimeoutMs,
                                                TransferListener transferListener) {
        HttpEngine transport = getEngine(context);
        return new HttpDataSource.BaseFactory() {
            @Override
            protected HttpDataSource createDataSourceInternal(HttpDataSource.RequestProperties defaults) {
                DefaultHttpDataSource source = new DefaultHttpDataSource(
                        userAgent, null, connectTimeoutMs, readTimeoutMs, true, defaults) {
                    @Override
                    protected HttpURLConnection openConnection(URL url) throws IOException {
                        return (HttpURLConnection) transport.openConnection(url);
                    }
                };
                if (transferListener != null) {
                    source.addTransferListener(transferListener);
                }
                return source;
            }
        };
    }
}
