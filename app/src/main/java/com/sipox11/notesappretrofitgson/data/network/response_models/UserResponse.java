package com.sipox11.notesappretrofitgson.data.network.response_models;

import com.google.gson.annotations.SerializedName;

/**
 * The response only has one field: the api_key.
 */
public class UserResponse extends BaseResponse {
    @SerializedName("api_key")
    String apiKey;

    public String getApiKey() {
        return apiKey;
    }
}
