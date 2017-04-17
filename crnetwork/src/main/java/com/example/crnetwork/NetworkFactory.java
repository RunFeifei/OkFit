package com.example.crnetwork;

import android.support.annotation.NonNull;
import android.util.Log;

import com.dianrong.android.common.AppContext;
import com.dianrong.android.common.utils.ContextUtils;
import com.dianrong.android.common.utils.DRPreferences;
import com.dianrong.android.user.UserStatus;
import com.example.crnetwork.internal.ExtendInterceptor;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import util.Strings;

/**
 * <p>
 * Created by yangcheng on 16/3/25.
 */
public class NetworkFactory {

    private static final String TAG = NetworkFactory.class.getSimpleName();

    private static final long CONNECT_TIMEOUT = 10_000;
    private static final long READ_TIMEOUT = 10_000;
    private static final long WRITE_TIMEOUT = 30_000;
    private static final byte[] retrofitInitLock = new byte[0];
    private static OkHttpClient client;
    private static Retrofit retrofit;
    private static HeaderInterceptor headerInterceptor;
    private static ExtendInterceptor applicationInterceptor;
    private static ExtendInterceptor networkInterceptor;
    private static String baseUrl;

    public static final OkHttpClient getClient() {

        if (client == null) {
            init();
        }
        return client;
    }

    private static void init() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // 为了调试方便，DEBUG版忽视所有证书问题。
        if (BuildConfig.DEBUG) {
            try {
                final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        // 解决okhttp3.x的NPE Bug
                        return new java.security.cert.X509Certificate[0];
                    }
                }};

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                builder.sslSocketFactory(sslSocketFactory);
                builder.hostnameVerifier((hostname, session) -> true);

                MyLoggingInterceptor loggingInterceptor = new MyLoggingInterceptor();
                loggingInterceptor.setLevel(MyLoggingInterceptor.Level.BODY);
                builder.addNetworkInterceptor(loggingInterceptor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 设置缓存大小
        File file = new File(AppContext.getInstance().getCacheDir().getAbsolutePath(), "okhttp");
        builder.cache(new Cache(file, 4 * 1024 * 1024));

        /**
         * addNetworkInterceptor-->网络拦截器，在request和resposne是分别被调用一次
         * addinterceptor-->aplication拦截器，只在response被调用一次
         */
        builder.addInterceptor(getHeaderInterceptor());

        builder.cookieJar(OkCookieStore.getInstance());

        builder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS);

        builder.addInterceptor(getApplicationInterceptor());
        builder.addNetworkInterceptor(getNetworkInterceptor());

        client = builder.build();
    }

    private static Retrofit getRetrofit() {
        if (client == null) {
            init();
        }
        synchronized (retrofitInitLock) {
            if (retrofit == null) {
                initRetrofit(NetworkFactory.baseUrl);
            }
        }

        return retrofit;
    }

    /**
     * 初始化,并将client和retrofit联系在一起
     *
     * @param baseUrl
     */
    private static void initRetrofit(String baseUrl) {
        Retrofit.Builder builder = new Retrofit.Builder();
        if (!Strings.isEmpty(baseUrl) && (baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            builder.baseUrl(baseUrl);
        }
        //TODO Jackson和Gson对bean的字段注解不一致!!!! 统一jackson和gson的引用
        builder.addConverterFactory(JacksonConverterFactory.create());
        builder.addCallAdapterFactory(RxJavaCallAdapterFactory.create());
        builder.client(NetworkFactory.getClient());
        retrofit = builder.build();
        Log.i(TAG, "Now: baseUrl is " + getBaseUrl());
    }

    //TODO log优化
    public static void resetBaseUrl(@NonNull String baseUrl) {
        if (Strings.isEqual(baseUrl, getBaseUrl())) {
            Log.i(TAG, "baseUrl stays in " + baseUrl);
            return;
        }
        if (Strings.isEmpty(baseUrl) || !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalStateException("baseUrl is illegal: " + baseUrl);
        }
        NetworkFactory.baseUrl = baseUrl;
        boolean needUpdateCookie = OkCookieStore.updateCookie(baseUrl);
        rebuildRetrofit(needUpdateCookie);
    }

    private static void rebuildRetrofit(boolean needUpdateCookie) {
        if (client == null || needUpdateCookie) {
            init();
        }
        initRetrofit(NetworkFactory.baseUrl);
    }

    //TODO 隐蔽此接口-->private
    public static <S> S createService(Class<S> serviceClass) {
        return getRetrofit().create(serviceClass);
    }

    public static void addApplicationInterceptor(Interceptor interceptor) {
        getApplicationInterceptor().addInterceptor(interceptor);
    }

    public static void addNetworkInterceptor(Interceptor interceptor) {
        getNetworkInterceptor().addInterceptor(interceptor);
    }

    private static HeaderInterceptor getHeaderInterceptor() {
        if (headerInterceptor == null) {
            headerInterceptor = new HeaderInterceptor();
        }
        return headerInterceptor;
    }

    private static ExtendInterceptor getApplicationInterceptor() {
        if (applicationInterceptor == null) {
            applicationInterceptor = new ExtendInterceptor();
        }
        return applicationInterceptor;
    }

    public static ExtendInterceptor getNetworkInterceptor() {
        if (networkInterceptor == null) {
            networkInterceptor = new ExtendInterceptor();
        }
        return networkInterceptor;
    }

    public static String getUserAgent() {
        return getHeaderInterceptor().getUserAgent();
    }

    /**
     * 设置UserAgent，如果不想覆盖当前的UserAgent，可以先通过getUserAgent()获取，再加上新增的UserAgent
     *
     * @param userAgent
     */
    public static void setUserAgent(String userAgent) {
        getHeaderInterceptor().setUserAgent(userAgent);
    }

    public static void addHeader(String key, String value) {
        if (value == null) {
            return;
        }
        getHeaderInterceptor().addHeader(key, value);
    }

    private static class HeaderInterceptor implements Interceptor {

        private String userAgent = "";
        private HashMap<String, String> headers = new HashMap<>();

        /**
         * userAgent是一个特殊字符串头
         * 使得服务器能够识别客户使用的操作系统及版本、CPU 类型、浏览器及版本...
         */
        HeaderInterceptor() {
            userAgent = "Android/" + ContextUtils.getSystemVersion()
                    + " " + ContextUtils.getAppName() + "/" + ContextUtils.getVersionCode(AppContext.getInstance())
                    + " ClientType/" + ContextUtils.getClientType()
                    + " ChannelId/" + ContextUtils.getChannelName();
        }

        @Override
        public Response intercept(Chain chain) throws IOException {

            Request request = chain.request();
            Request.Builder requestBuilder = request.newBuilder();

            requestBuilder.header("User-Agent", userAgent);
            if (UserStatus.isLoggedIn() && UserStatus.getUser() != null && UserStatus.getUser().getAid() != null) {
                requestBuilder.header("X-SL-Username", UserStatus.getUser().getAid());
            }
            requestBuilder.header("X-SL-UUID", DRPreferences.getSlUUID());
            requestBuilder.header("IMEI", ContextUtils.getImei());
            //TODO 非空判断
            requestBuilder.header("Referer", NetworkFactory.baseUrl);
            // headers.forEach((k, v) -> requestBuilder.header(k, v));
            for (Map.Entry<String, String> item : headers.entrySet()) {
                requestBuilder.header(item.getKey(), item.getValue());
            }

            Response response = chain.proceed(requestBuilder.build());
            return response;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public void addHeader(String key, String value) {
            headers.put(key, value);
        }
    }

    public static String getBaseUrl() {
        if (retrofit == null || client == null) {
            return null;
        }
        return retrofit.baseUrl().toString();
    }
}
