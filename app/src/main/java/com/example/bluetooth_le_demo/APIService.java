package com.example.bluetooth_le_demo;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface APIService {
    @POST("api/heart-rate")
    Call<APIResponse> sendHeartRate(@Body HashMap<String, Object> body);
}
