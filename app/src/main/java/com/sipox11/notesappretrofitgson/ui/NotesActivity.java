package com.sipox11.notesappretrofitgson.ui;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.sipox11.notesappretrofitgson.R;
import com.sipox11.notesappretrofitgson.data.network.ApiClient;
import com.sipox11.notesappretrofitgson.data.network.NotesApi;
import com.sipox11.notesappretrofitgson.data.network.response_models.NoteResponse;
import com.sipox11.notesappretrofitgson.data.network.response_models.UserResponse;
import com.sipox11.notesappretrofitgson.utils.DividerItemDecoration;
import com.sipox11.notesappretrofitgson.utils.PrefUtils;
import com.sipox11.notesappretrofitgson.utils.RecyclerTouchListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.internal.operators.completable.CompletableDisposeOn;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class NotesActivity extends AppCompatActivity {

    private static final String TAG = NotesActivity.class.getSimpleName();
    private NotesApi notesApi;
    private CompositeDisposable disposable = new CompositeDisposable();
    private NotesAdapter mAdapter;
    private List<NoteResponse> notesList = new ArrayList<>();

    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.txt_empty_notes_view)
    TextView noNotesView;

    //region Activity Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        setupUI();
        setupApiService();

        /**
         * Check for stored Api Key in shared preferences
         * If not present, make api call to register the user
         * This will be executed when app is installed for the first time
         * or data is cleared from settings
         * */
        if (TextUtils.isEmpty(PrefUtils.getApiKey(this))) {
            registerDevice();
        } else {
            // user is already registered, fetch all notes
            Log.d(TAG, "onCreate: Fetching all notes...");
            fetchAllNotes();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
    //endregion

    //region UI management
    private void setupUI() {
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.activity_title_home));
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNoteDialog(false, null, -1);
            }
        });
        // white background notification bar
        whiteNotificationBar(fab);

        // Setup adapter
        setupAdapter();
    }

    private void setupAdapter() {
        mAdapter = new NotesAdapter(this, notesList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        /**
         * On long press on RecyclerView item, open alert dialog
         * with options to choose
         * Edit and Delete
         * */
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
            }

            @Override
            public void onLongClick(View view, int position) {
                showActionsDialog(position);
            }
        }));
    }

    private void refreshUI() {
        recyclerView.setAdapter(new NotesAdapter(getApplicationContext(), notesList));
        manageEmptyNotesMessage();
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

    //region Alert dialogs and notifications
    private void manageEmptyNotesMessage() {
        if (notesList.size() > 0) {
            noNotesView.setVisibility(View.GONE);
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows alert dialog with EditText options to enter / edit
     * a note.
     * when shouldUpdate=true, it automatically displays old note and changes the
     * button text to UPDATE
     */
    private void showNoteDialog(final boolean shouldUpdate, final NoteResponse note, final int position) {
        // Inflate dialog layout
        LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
        View view = layoutInflater.inflate(R.layout.notes_dialog, null);

        // Build Alert dialog
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(NotesActivity.this);
        alertDialogBuilderUserInput.setView(view);

        // Configure input note edit text
        final EditText inputNote = view.findViewById(R.id.note);
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        dialogTitle.setText(!shouldUpdate ? getString(R.string.lbl_new_note_title) : getString(R.string.lbl_edit_note_title));

        if(shouldUpdate && note != null) {
            inputNote.setText(note.getNote());
        }

        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(shouldUpdate ? "update" : "save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNegativeButton("cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });
        // Build and show alert dialog
        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show toast message when no text is entered
                if (TextUtils.isEmpty(inputNote.getText().toString())) {
                    Toast.makeText(NotesActivity.this, "Enter note!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alertDialog.dismiss();
                }

                // check if user updating note
                if (shouldUpdate && note != null) {
                    // update note by it's id
                    Log.d(TAG, "onClick: Updating note...");
                    updateNote(note.getId(), inputNote.getText().toString(), position);
                } else {
                    // create new note
                    Log.d(TAG, "onClick: Creating new note...");
                    createNote(inputNote.getText().toString());
                }
            }
        });
    }

    /**
     * Opens dialog with Edit - Delete options
     * Edit - 0
     * Delete - 0
     */
    private void showActionsDialog(final int position) {
        CharSequence colors[] = new CharSequence[]{"Edit", "Delete"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose option");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showNoteDialog(true, notesList.get(position), position);
                } else {
                    Log.d(TAG, "onClick: Deleting note...");
                    // TODO: Delete action
//                    deleteNote(notesList.get(position).getId(), position);
                }
            }
        });
        builder.show();
    }

    /**
     * Showing a Snackbar with error message
     * The error body will be in json format
     * {"error": "Error message!"}
     */
    private void showError(Throwable e) {
        String message = "";
        try {
            if (e instanceof IOException) {
                message = "No internet connection!";
            } else if (e instanceof HttpException) {
                HttpException error = (HttpException) e;
                String errorBody = error.response().errorBody().string();
                JSONObject jObj = new JSONObject(errorBody);

                message = jObj.getString("error");
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (JSONException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        if (TextUtils.isEmpty(message)) {
            message = "Unknown error occurred! Check LogCat.";
        }

        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, message, Snackbar.LENGTH_LONG);

        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    private void whiteNotificationBar(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            getWindow().setStatusBarColor(Color.WHITE);
        }
    }

    //endregion

    //region Network

    /**
     * Generate NotesApi instance.
     */
    private void setupApiService() {
        // Create api instance
        notesApi = ApiClient.getClient(getApplicationContext())
                .create(NotesApi.class);
    }


    /**
     * Register current device to generate API KEY
     */
    private void registerDevice() {
        // Determine device id as a random one
        String devId = UUID.randomUUID().toString();
        // Perform register user network request
        disposable.add(
            notesApi.register(devId)
                    // Subscribe to response on main thread
                    .subscribeOn(Schedulers.io())
                    // Observe in a background thread
                    .observeOn(AndroidSchedulers.mainThread())
                    // Define the disposable single observer
                    .subscribeWith(new DisposableSingleObserver<UserResponse>() {
                        /**
                         * Notifies the SingleObserver with a single item and that the {@link Single} has finished sending
                         * push-based notifications.
                         * <p>
                         * The {@link Single} will not call this method if it calls {@link #onError}.
                         *
                         * @param userResponse the item emitted by the Single
                         */
                        @Override
                        public void onSuccess(UserResponse userResponse) {
                            String apiKey = userResponse.getApiKey();
                            PrefUtils.storeApiKey(getApplicationContext(), apiKey);
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Device is registered successfully! ApiKey: " + apiKey,
                                    Toast.LENGTH_LONG
                            ).show();
                            fetchAllNotes();
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
                            Log.e(TAG, "onError: " + e.getMessage());
                            showError(e);
                        }
                    }));

    }

    /**
     * Retrieve all notes from the server
     */
    private void fetchAllNotes() {
        disposable.add(
            notesApi.fetchAllNotes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<NoteResponse>>() {
                    /**
                     * Notifies the SingleObserver with a single item and that the {@link Single} has finished sending
                     * push-based notifications.
                     * <p>
                     * The {@link Single} will not call this method if it calls {@link #onError}.
                     *
                     * @param noteResponses the item emitted by the Single
                     */
                    @Override
                    public void onSuccess(List<NoteResponse> noteResponses) {
                        Log.d(TAG, "onSuccess: Successful retrieval of [" + notesList.size() + "] notes :)");
                        notesList = noteResponses;
                        refreshUI();
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
                        Log.e(TAG, "onError: Notes retrieval failed :(");
                        showError(e);
                    }
                })
        );
    }

    /**
     * Create a new note in the server.
     * @param note The new note that is to be created.
     */
    private void createNote(String note) {
        disposable.add(
                notesApi.createNote(note)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<NoteResponse>() {
                        /**
                         * Notifies the SingleObserver with a single item and that the {@link Single} has finished sending
                         * push-based notifications.
                         * <p>
                         * The {@link Single} will not call this method if it calls {@link #onError}.
                         *
                         * @param noteResponse the item emitted by the Single
                         */
                        @Override
                        public void onSuccess(NoteResponse noteResponse) {
                            Log.d(TAG, "onSuccess: New note successfully created -> Note: " + noteResponse.getNote());
                            notesList.add(noteResponse);
                            refreshUI();
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
                            Log.e(TAG, "onError: Request to create note failed...");
                            showError(e);
                        }
                    })
        );
    }

    /**
     * Updates a note with new content.
     *
     * @param id            Of the note.
     * @param newNote       The new content of the note.
     * @param position      Its position within the list.
     */
    private void updateNote(final int id, final String newNote, final int position) {
        disposable.add(
                notesApi.updateNote(id, newNote)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableCompletableObserver() {
                            /**
                             * Called once the deferred computation completes normally.
                             */
                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: Successfully updated note with id=" +
                                        id + " with content: " + newNote + " in position" + position);
                                // Update note within adapter
                                notesList.get(position).setNote(newNote);
                            }

                            /**
                             * Called once if the deferred computation 'throws' an exception.
                             *
                             * @param e the exception, not null.
                             */
                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: Update request failed :(");
                                showError(e);
                            }
                        })
        );
    }

    //endregion
}
