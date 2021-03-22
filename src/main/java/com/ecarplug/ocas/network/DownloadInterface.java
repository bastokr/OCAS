package com.ecarplug.ocas.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface DownloadInterface
{
    @Streaming
    @GET
    Call<ResponseBody> downloadFileByUrl(@Url String fileUrl);

    // // @GET("{fileUri}")
    // // Single<ResponseBody> downloadFirmware(@Path("fileUri") String fileUri);
    // @GET("/ajax/firmware/download?file=31")
    // Single<ResponseBody> downloadFirmware();
    //
    // @Streaming
    // @GET
    // Observable<Response<ResponseBody>> download(@Url String url);
    //
    // // Retrofit 2 GET request for rxjava
    // @Streaming
    // @GET
    // Observable<Response<ResponseBody>> downloadFileByUrlRx(@Url String fileUrl);
    //
    //
    // @GET("users/{owner}/repos")
    // Single<List<GithubRepo>> getRepos(@Path("owner") String owner);
    //
    // @GET("movie/now_playing")
    // Call<MovieResponse> getNowPlayingMovies(@Query("api_key") String apiKey, @Query("page") int page);
    //
    // // option 1: a resource relative to your base URL
    // @GET("resource/example.zip")
    // Call<ResponseBody> downloadFileWithFixedUrl();
    //
    // // option 2: using a dynamic URL
    // @GET
    // Call<ResponseBody> downloadFileWithDynamicUrl(@Url String fileUrl);
}
