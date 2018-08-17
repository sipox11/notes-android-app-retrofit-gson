package com.sipox11.notesappretrofitgson.data.network;

import android.content.Context;
import android.text.TextUtils;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.sipox11.notesappretrofitgson.config.Constants;
import com.sipox11.notesappretrofitgson.utils.PrefUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Holds and instantiates a Retrofit instance. This should be used via DI
 * with dagger2, but the goal here is to just understand how Retrofit works.
 */
public class ApiClient {

    private static Retrofit retrofit = null;
    private static int REQUEST_TIMEOUT = 60;
    private static OkHttpClient okHttpClient;

    public static Retrofit getClient(Context context) {
        // Init okHttpClient
        if(okHttpClient == null) {
            initOkHttp(context);
        }
        // Init Retrofit
        if(retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    // Use okHttpClient instance as client
                    .client(okHttpClient)
                    // Use RxJava2CallAdapter factory
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    // Use Gson to serialize / deserialize
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static void initOkHttp(final Context context) {
        // Configure timeouts for request connection, data read and data write.
        OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS);
        // Configure logging interceptor so that requests and responses can be debugged
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Add logging interceptor to http client
        httpClient.addInterceptor(loggingInterceptor);

        // Add new interceptor to stub content type json header
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json");

                // Add authorization token too (API KEY) if it's present in Shared Prefs
                String apiKey = PrefUtils.getApiKey(context);
                if(!TextUtils.isEmpty(apiKey)) {
                    requestBuilder.addHeader("Authorization", apiKey);
                }

                // Build request
                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });

        // Build http client
        okHttpClient = httpClient.build();
    }

}
