package com.sipox11.notesappretrofitgson.ui;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.sipox11.notesappretrofitgson.R;
import com.sipox11.notesappretrofitgson.data.network.ApiClient;
import com.sipox11.notesappretrofitgson.data.network.NotesApi;
import com.sipox11.notesappretrofitgson.data.network.response_models.UserResponse;
import com.sipox11.notesappretrofitgson.utils.PrefUtils;

import java.util.UUID;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUI();
        registerDevice();
    }

    //region UI management
    private void setupUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region Network
    private void registerDevice() {
        // Create api instance
        NotesApi notesApi = ApiClient.getClient(getApplicationContext())
                .create(NotesApi.class);
        // Determine device id as a random one
        String devId = UUID.randomUUID().toString();
        // Perform register user network request
        notesApi.register(devId)
                // Subscribe to response on main thread
                .subscribeOn(AndroidSchedulers.mainThread())
                // Observe in a background thread
                .observeOn(Schedulers.io())
                // Define the disposable single observer
                .subscribeWith(new DisposableSingleObserver<UserResponse>() {
                    /**
                     * Notifies the SingleObserver with a single item and that the {@link Single} has finished sending
                     * push-based notifications.
                     * <p>
                     * The {@link Single} will not call this method if it calls {@link #onError}.
                     *
                     * @param o the item emitted by the Single
                     */
                    @Override
                    public void onSuccess(UserResponse userResponse) {
                        String apiKey = userResponse.getApiKey();
                        PrefUtils.storeApiKey(getApplicationContext(), apiKey);
                    }

                    /**
                     * Notifies the SingleObserver that the {@link Single} has experienced an error condition.
                     * <p>
                     * If the {@link Single} calls this method, it will not thereafter call {@link #onSuccess}.
                     *
                     * @param e the exception encountered by the Single
                     */
                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: Register network request failed -> " + e.getLocalizedMessage());
                    }
                });

    }
    //endregion
}
