package com.sipox11.notesappretrofitgson.data.network;

import com.sipox11.notesappretrofitgson.data.models.Note;
import com.sipox11.notesappretrofitgson.data.network.response_models.NoteResponse;
import com.sipox11.notesappretrofitgson.data.network.response_models.UserResponse;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotesApi {

    // Register new user
    @FormUrlEncoded
    @POST("notes/user/register")
    Single<UserResponse> register(@Field("device_id") String deviceId);

    // Create note
    @FormUrlEncoded
    @POST("/notes/new")
    Single<NoteResponse> createNote(@Field("note") String note);

    // Fetch all notes
    @GET("notes/all")
    Single<List<Note>> fetchAllNotes();

    // Update single note
    @PUT("notes/{id}")
    Completable updateNote(@Path("id") int noteId, @Field("note") String note);

   // Delete note
    @DELETE("notes/{id}")
    Completable deleteNote(@Path("id") int noteId);
}
