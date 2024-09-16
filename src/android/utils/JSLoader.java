package org.apache.cordova.firebase.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class JSLoader {

    @SuppressLint("SetJavaScriptEnabled")
    public static void saveFCMToIndexedDB(Context context, String msgs) {
        String currentUserJid = SharedPrefsUtils.getString(context, "current_user_jid");

        Handler handler = new Handler(Looper.getMainLooper());
        try {
            handler.post(
                    () -> {
                        final WebView webView = new WebView(context.getApplicationContext());
                        webView.getSettings().setJavaScriptEnabled(true);
                        SaveFcmBridge saveFcmBridge = new SaveFcmBridge();
                        webView.addJavascriptInterface(saveFcmBridge, "saveFcmBridge");
                        webView.setWebViewClient(new WebViewClient() {
                            public void onPageFinished(WebView view, String url) {
                                Log.d("JSLoader", "[saveFCMToIndexedDB][onPageFinished]");
                                if (TextUtils.isEmpty(msgs)) {
                                    Log.d("JSLoader", "[saveFCMToIndexedDB] msgs is empty");
                                    view.destroy();
                                    return;
                                }

                                Log.d("JSLoader", "[saveFCMToIndexedDB] msgs: " + msgs);
                                try {
                                    view.evaluateJavascript(
                                            "        const result2 = " + JSONObject.quote(msgs) + ";" +
                                                    "        const userJid2 = '" + currentUserJid + "';" +
                                                    "        saveFCMToIndexedDB(result2, userJid2);", result -> {
                                                Log.d("JSLoader", "[evaluateJavascript] callback");
                                                try {
                                                    boolean isSavingCompleted;
                                                    int counter = 0;

                                                    do {
                                                        Thread.sleep(100);

                                                        isSavingCompleted = saveFcmBridge.isSavingCompleted();
                                                        counter++;
                                                    } while (!isSavingCompleted && counter < 10);

                                                    Log.d("JSLoader", "[evaluateJavascript] callback, isSavingCompleted: " + isSavingCompleted);
                                                    if (isSavingCompleted || counter == 10) {
                                                        view.destroy();
                                                    }
                                                } catch (Exception e) {
                                                    view.destroy();
                                                    e.printStackTrace();
                                                }
                                            });
                                } catch (Exception e) {
                                    view.destroy();
                                    e.printStackTrace();
                                }
                            }
                        });
                        webView.loadUrl("file:///android_asset/indexed_db_worker.html");
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void syncMessages(Context context) {
        String currentUserJid = SharedPrefsUtils.getString(context, "current_user_jid");

        Handler handler = new Handler(Looper.getMainLooper());
        try {
            handler.post(
                    () -> {
                        WebView webView = new WebView(context.getApplicationContext());
                        webView.getSettings().setJavaScriptEnabled(true);
                        SyncMsgsBridge syncMsgsBridge = new SyncMsgsBridge();
                        webView.addJavascriptInterface(syncMsgsBridge, "syncMsgsBridge");
                        webView.setWebViewClient(new WebViewClient() {
                            public void onPageFinished(WebView view, String url) {
                                Log.d("JSLoader", "[onPageFinished]");
                                try {
                                    view.evaluateJavascript(
                                            "const currentUserJid = '" + currentUserJid + "';\n" +
                                                    "console.log('Start script from Java');\n" +
                                                    "getMaxSortIdSync(currentUserJid).then(sortId => {\n" +
                                                    "    syncMsgsBridge.putSortIdResult(sortId);\n" +
                                                    "});", result -> {
                                                Log.d("JSLoader", "[evaluateJavascript] callback");
                                                try {
                                                    String sortIdString;
                                                    int counter = 0;

                                                    do {
                                                        Thread.sleep(100);

                                                        sortIdString = syncMsgsBridge.getSortIdResult();
                                                        counter++;
                                                    } while (sortIdString == null && counter < 10);

                                                    Log.d("JSLoader", "[evaluateJavascript] callback, sortIdString " + sortIdString);
                                                    if (sortIdString != null) {
                                                        long sortId = Long.parseLong(sortIdString);
                                                        new SyncMessagesTask(view, syncMsgsBridge).execute(sortId);
                                                    } else {
                                                        view.destroy();
                                                    }
                                                } catch (Exception e) {
                                                    view.destroy();
                                                    e.printStackTrace();
                                                }
                                            });
                                } catch (Exception e) {
                                    view.destroy();
                                    e.printStackTrace();
                                }
                            }
                        });

                        webView.loadUrl("file:///android_asset/indexed_db_worker.html");
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class SyncMessagesTask extends AsyncTask<Long, Void, String> {
        private static final String TAG = "SyncMessagesTask";

        @SuppressLint("StaticFieldLeak")
        private final WebView webView;
        private final SyncMsgsBridge syncMsgsBridge;

        public SyncMessagesTask(WebView view, SyncMsgsBridge syncMsgsBridge) {
            super();
            this.webView = view;
            this.syncMsgsBridge = syncMsgsBridge;
        }

        @Override
        protected String doInBackground(Long... params) {
            Log.i(TAG, "[doInBackground] params: " + Arrays.toString(params));

            String result = "";
            try {
                JSONObject postData = new JSONObject();
                postData.put("excludeGroupchat", false);
                postData.put("offset", 0);
                postData.put("rows", 1000);
                postData.put("sida", params[0]);
                postData.put("tags", new JSONArray());

                Log.i(TAG, "[doInBackground] postData: " + postData);

                HttpURLConnection urlConnection = createUrlConnection(this.webView.getContext());

                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();

                int statusCode = urlConnection.getResponseCode();
                Log.i(TAG, "[doInBackground] Response statusCode: " + statusCode);

                if (statusCode != 200) {
                    Log.w(TAG, "[doInBackground] Server response message: " + urlConnection.getResponseMessage());

                    StringBuilder sb = new StringBuilder();
                    InputStreamReader in = new InputStreamReader(urlConnection.getErrorStream());
                    BufferedReader bufferedReader = new BufferedReader(in);
                    int cp;

                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }

                    bufferedReader.close();
                    in.close();
                    Log.w(TAG, "[doInBackground] Server response Error: " + sb.toString());
                } else {
                    StringBuilder sb = new StringBuilder();
                    InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(in);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }
                    bufferedReader.close();
                    in.close();
                    result = sb.toString();
                }
            } catch (Exception e) {
                Log.i(TAG, "[doInBackground]", e);
            }

            return result;
        }

        private HttpURLConnection createUrlConnection(Context context) throws IOException {
            String apiUrl = SharedPrefsUtils.getString(context, "apiUrl");
            String token = SharedPrefsUtils.getString(context, "auth-token");

            Log.i(TAG, "[createUrlConnection] apiUrl: " + apiUrl);
            Log.i(TAG, "[createUrlConnection] token: " + token);

            URL url = new URL(apiUrl + "/sync-messages");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            urlConnection.setRequestMethod("POST");

            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization", token);
            urlConnection.setRequestProperty("Accept", "application/json");

            return urlConnection;
        }

        @Override
        protected void onPostExecute(String result) {
            if (TextUtils.isEmpty(result)) {
                Log.i(TAG, "[onPostExecute] result is empty");
                webView.destroy();
                return;
            }

            String currentUserJid = SharedPrefsUtils.getString(webView.getContext(), "current_user_jid");

            try {
                Log.i(TAG, "[onPostExecute] jsonResult: " + result);
                this.webView.evaluateJavascript(
                        "        const result = " + JSONObject.quote(result) + ";" +
                                "        const userJid = '" + currentUserJid + "';" +
                                "        processSyncMessagesResponse(result, userJid);", jsResult -> {
                            Log.d("JSLoader", "[evaluateJavascript] callback");
                            try {
                                boolean isCompleted;
                                int counter = 0;

                                do {
                                    Thread.sleep(100);

                                    isCompleted = syncMsgsBridge.isSyncCompleted();
                                    counter++;
                                } while (!isCompleted && counter < 20);

                                Log.d(TAG, "[onPostExecute] isCompleted: " + isCompleted);
                                if (isCompleted || counter == 20) {
                                    webView.destroy();
                                }
                            } catch (Exception e) {
                                webView.destroy();
                                e.printStackTrace();
                            }
                        });
            } catch (Exception e) {
                webView.destroy();
                e.printStackTrace();
            }
        }
    }

    static class SyncMsgsBridge {

        public String sortIdResult = null;
        public boolean syncCompleted = false;

        @JavascriptInterface
        public void putSortIdResult(String result) {
            this.sortIdResult = result;
        }

        public String getSortIdResult() {
            return this.sortIdResult;
        }

        @JavascriptInterface
        public void putSyncCompletedResult(boolean result) {
            this.syncCompleted = result;
        }

        public boolean isSyncCompleted() {
            return this.syncCompleted;
        }
    }

    static class SaveFcmBridge {

        public boolean savingCompleted = false;

        @JavascriptInterface
        public void putSavingCompletedResult(boolean result) {
            this.savingCompleted = result;
        }

        public boolean isSavingCompleted() {
            return this.savingCompleted;
        }
    }
}
