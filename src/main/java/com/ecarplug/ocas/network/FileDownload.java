package com.ecarplug.ocas.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

@Slf4j
public class FileDownload
{
    // private static final String TAG = "FileDownload";
    // private DownloadZipFileTask downloadZipFileTask;
    // @Getter private boolean downloadCompleted = false;
    // @Getter private boolean downloadProcessing = false;
    //
    // private void downloadZipFile(String baseUrl, String fileUrl)
    // {
    //     if (downloadProcessing) return;
    //
    //     downloadCompleted = false;
    //     downloadProcessing = true;
    //
    //     DownloadInterface downloadService = createService(DownloadInterface.class, baseUrl);    // _application.getBaseUri());
    //     Call<ResponseBody> call = downloadService.downloadFileByUrl(fileUrl);                   // "/ajax/firmware/download?file=31");
    //
    //     call.enqueue(new Callback<ResponseBody>()
    //     {
    //         @Override
    //         public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response)
    //         {
    //             if (response.isSuccessful())
    //             {
    //                 log.debug(TAG, "Got the body for the file");
    //                 // Toast.makeText(getApplicationContext(), "Downloading...", Toast.LENGTH_SHORT).show();
    //
    //                 String header = response.headers().get("Content-Disposition");
    //                 String fileName = header.replace("attachment; filename=", "");
    //                 fileName = fileName.replaceAll("\"", "");
    //
    //                 downloadZipFileTask = new DownloadZipFileTask(fileName);
    //                 downloadZipFileTask.execute(response.body());
    //             }
    //             else
    //             {
    //                 log.debug(TAG, "Connection failed " + response.errorBody());
    //             }
    //         }
    //
    //         @Override
    //         public void onFailure(Call<ResponseBody> call, Throwable t)
    //         {
    //             t.printStackTrace();
    //             log.error(TAG, t.getMessage());
    //         }
    //     });
    // }
    //
    // public <T> T createService(Class<T> serviceClass, String baseUrl)
    // {
    //     Retrofit retrofit = new Retrofit.Builder()
    //             .baseUrl(baseUrl)
    //             .client(new OkHttpClient.Builder().build())
    //             .build();
    //     return retrofit.create(serviceClass);
    // }
    //
    // private class DownloadZipFileTask extends AsyncTask<ResponseBody, Pair<Integer, Long>, String>
    // {
    //     String filename;
    //     public DownloadZipFileTask(String fileName) {this.filename = fileName;}
    //
    //     @Override
    //     protected void onPreExecute()
    //     {
    //         super.onPreExecute();
    //     }
    //
    //     @Override
    //     protected String doInBackground(ResponseBody... urls)
    //     {
    //         //Copy you logic to calculate progress and call
    //         saveToDisk(urls[0], filename);
    //         return null;
    //     }
    //
    //     protected void onProgressUpdate(Pair<Integer, Long>... progress)
    //     {
    //         log.debug("API123", progress[0].second + " ");
    //
    //         if (progress[0].first == 100)
    //         {
    //             // Toast.makeText(getApplicationContext(), "File downloaded successfully", Toast.LENGTH_SHORT).show();
    //             log.debug(TAG, "File downloaded successfully");
    //             downloadCompleted = true;
    //         }
    //
    //         if (progress[0].second > 0)
    //         {
    //             int currentProgress = (int) ((double) progress[0].first / (double) progress[0].second * 100);
    //             // progressBar.setProgress(currentProgress);
    //             // txtProgressPercent.setText("Progress " + currentProgress + "%");
    //             log.debug(TAG, "Progress " + currentProgress + "%");
    //         }
    //
    //         if (progress[0].first == -1)
    //         {
    //             // Toast.makeText(getApplicationContext(), "Download failed", Toast.LENGTH_SHORT).show();
    //             log.debug(TAG, "Download failed");
    //         }
    //     }
    //
    //     public void doProgress(Pair<Integer, Long> progressDetails)
    //     {
    //         publishProgress(progressDetails);
    //     }
    //
    //     @Override
    //     protected void onPostExecute(String result)
    //     {
    //
    //     }
    // }
    //
    // private void saveToDisk(ResponseBody body, String filename)
    // {
    //     try
    //     {
    //         // sbTodo 앱의 캐시 디렉토리로 변경이 필요
    //         File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
    //
    //         InputStream inputStream = null;
    //         OutputStream outputStream = null;
    //
    //         try
    //         {
    //
    //             inputStream = body.byteStream();
    //             outputStream = new FileOutputStream(destinationFile);
    //             byte data[] = new byte[4096];
    //             int count;
    //             int progress = 0;
    //             long fileSize = body.contentLength();
    //             log.debug(TAG, "File Size=" + fileSize);
    //             while ((count = inputStream.read(data)) != -1)
    //             {
    //                 outputStream.write(data, 0, count);
    //                 progress += count;
    //                 Pair<Integer, Long> pairs = new Pair<>(progress, fileSize);
    //                 downloadZipFileTask.doProgress(pairs);
    //                 log.debug(TAG, "Progress: " + progress + "/" + fileSize + " >>>> " + (float) progress / fileSize);
    //             }
    //
    //             outputStream.flush();
    //
    //             log.debug(TAG, destinationFile.getParent());
    //             Pair<Integer, Long> pairs = new Pair<>(100, 100L);
    //             downloadZipFileTask.doProgress(pairs);
    //             return;
    //         }
    //         catch (IOException e)
    //         {
    //             e.printStackTrace();
    //             Pair<Integer, Long> pairs = new Pair<>(-1, Long.valueOf(-1));
    //             downloadZipFileTask.doProgress(pairs);
    //             log.debug(TAG, "Failed to save the file!");
    //             return;
    //         }
    //         finally
    //         {
    //             if (inputStream != null) inputStream.close();
    //             if (outputStream != null) outputStream.close();
    //         }
    //     }
    //     catch (IOException e)
    //     {
    //         e.printStackTrace();
    //         log.debug(TAG, "Failed to save the file!");
    //         return;
    //     }
    // }
}
