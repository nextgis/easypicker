package com.keenfin.easypicker;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class DownloadPhotoIntentService extends Service {


    private static final String ACTION_DOWNLOAD_PHOTO = "com.nextgis.mobile.util.action.download.photo";
    private static final String ITEM_URL = "com.nextgis.mobile.util.action.download.photo.url";
    private static final String ITEM_URL_ID = "com.nextgis.mobile.util.action.download.photo.urlid";

    private static final String ITEM_FILEPATH = "com.nextgis.mobile.util.action.download.photo.filepath";
    private static final String ITEM_FILEPATH_FILE = "com.nextgis.mobile.util.action.download.photo.filepath.file";
    private static final String USER_AGENT = "com.nextgis.mobile.util.action.download.photo.useragent";


    public final static int TIMEOUT_CONNECTION = 10000;
    public final static int TIMEOUT_SOCKET = 240000; // 180 sec

    public static final String DOWNLOAD_ACTION = "com.nextgis.mobile.download.photo.action";

    public static final String EXTRA_URL_ID = "url_id";// url for search image
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_VALUE_START = "start";
    public static final String EXTRA_VALUE_END = "end";
    public static final String EXTRA_HTTP_CODE = "httpcode";


    public static final String EXTRA_LOGIN = "login";
    public static final String EXTRA_PASS = "password";

    public static final String EXTRA_PREVIEW = "preview";
    public static final String EXTRA_WIDTH = "width";
    public static final String EXTRA_HEIGHT = "height";

    public static final String EXTRA_PROGRESS_LIST = "preview";

    private static final String IP_ADDRESS = "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
            + "|[1-9][0-9]|[0-9]))(:[0-9]{1,5})?";

    public static final String URL_PATTERN = "^(?i)((ftp|https?)://)?(([\\da-z.-]+)\\.([a-z.]{2,6})|" + IP_ADDRESS + ")(:[0-9]{1,5})?(/\\S*)?$";


    static private ArrayList<String> inProgressList = new ArrayList<>();
    static Object objectProgressSync = new Object();

    static public ArrayList<String> getProgressList(){
        ArrayList<String> result = new ArrayList<>();
        synchronized (objectProgressSync){
            for (final String item:inProgressList){
                result.add(item);
            }
        }
        return result;
    };

    static public void putProgressListItem(String item){
        synchronized (objectProgressSync){
            for (String progressItem:inProgressList)
                if (progressItem.equals(item))
                    return;
            inProgressList.add(item);
        }
    }

    static public void deleteProgressListItem(String item){
        synchronized (objectProgressSync){
            for (String progressItem:inProgressList)
                if (progressItem !=null && progressItem.equals(item)){
                    inProgressList.remove(progressItem);
                    return;
                }
        }
    }


    public boolean previewOperation = false;

    String TAG = "dowwnload";

//    public DownloadPhotoIntentService() {
//        super("DownloadPhotoIntentService");
//    }

    public static void startActionDownload(final Context context, String url, String filepath, String filename,
                                           String login, String password, String userAgent) {

        final Intent intent = new Intent(context, DownloadPhotoIntentService.class);
        intent.setAction(ACTION_DOWNLOAD_PHOTO);
        intent.putExtra(ITEM_URL, url);
        intent.putExtra(ITEM_URL_ID, url);

        intent.putExtra(ITEM_FILEPATH, filepath);
        intent.putExtra(ITEM_FILEPATH_FILE, filename);
        intent.putExtra(EXTRA_LOGIN, login);
        intent.putExtra(EXTRA_PASS, password);
        intent.putExtra(EXTRA_PREVIEW, false);
        intent.putExtra(USER_AGENT, userAgent);

        putProgressListItem(url);

        context.startService(intent);
    }

    public static void startActionDownload(final Context context, String url, String filepath, String filename,
                                           String login, String password, int width, int height, String urlId, String userAgent) {
        final Intent intent = new Intent(context, DownloadPhotoIntentService.class);
        intent.setAction(ACTION_DOWNLOAD_PHOTO);
        intent.putExtra(ITEM_URL, url);
        intent.putExtra(ITEM_URL_ID, urlId);

        intent.putExtra(ITEM_FILEPATH, filepath);
        intent.putExtra(ITEM_FILEPATH_FILE, filename);
        intent.putExtra(EXTRA_LOGIN, login);
        intent.putExtra(EXTRA_PASS, password);
        intent.putExtra(EXTRA_WIDTH, width);
        intent.putExtra(EXTRA_HEIGHT, height);
        intent.putExtra(EXTRA_PREVIEW, true);
        intent.putExtra(USER_AGENT, userAgent);

        context.startService(intent);
    }


    private void handleActionDownload(final Intent intent) {
        previewOperation = intent.getExtras().getBoolean(EXTRA_PREVIEW, false);
        final String url = intent.getExtras().getString(ITEM_URL);
        final String urlId = intent.getExtras().getString(ITEM_URL_ID);

        final String filepath = intent.getExtras().getString(ITEM_FILEPATH);
        final String filename = intent.getExtras().getString(ITEM_FILEPATH_FILE);

        final String login = intent.getExtras().getString(EXTRA_LOGIN);
        final String password = intent.getExtras().getString(EXTRA_PASS);
        final String userAgent = intent.getExtras().getString(USER_AGENT);

        final String fullFilePath = getBaseContext().getExternalCacheDir().getAbsolutePath() + "/" + filepath;

        final File targetFile = new File(fullFilePath);
        FileUtil.createDir(targetFile);
        sendBroadcast(getApplicationContext(), urlId, true, -1);

        try {
            int responseCode = getFileFromStream(url, fullFilePath, filename, login, password, userAgent );

            if (responseCode == HttpURLConnection.HTTP_OK)
                sendBroadcast(getApplicationContext(), urlId, false, responseCode);
        } catch (Exception ex){
            try {
                if (targetFile.exists())
                    targetFile.delete();
            } catch (Exception ex1){
                Log.e("TAG", ex1.toString());
            }
            sendBroadcast(getApplicationContext(), urlId, false, -1);
            Log.e(TAG, ex.toString());
        }
    }

    protected int getFileFromStream(   String url,
                                        String filePath,
                                        String filename, String login, String password, String useragent)
                                        throws IOException {
        File file = new File(new File(filePath), filename);
        if (!file.exists()){
            boolean result = file.createNewFile();
            if (!result)
                throw  new IOException("file not created");
        }

        OutputStream output = Files.newOutputStream(Paths.get(file.getAbsolutePath()));
        //getStream(url, getLogin(), getPassword(), output);
        return getStream(url, login, password, output, useragent);
    }

    public int getStream(
            String targetURL,
            String username,
            String password,
            OutputStream outputStream,
            String userAgent ) throws IOException {
        int    IO_BUFFER_SIZE     = 32 * 1024; // 32k


        //try {Thread.sleep(5000);} catch (Exception ex) {}

        final HttpURLConnection conn = getHttpConnection("GET", targetURL, username, password, userAgent);
        if (null == conn) {
            if (true)
                Log.d(TAG, "Error get stream: " + targetURL);
            throw new IOException("Connection is null");
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM && conn.getURL().getProtocol().equals("http")) {
            targetURL = targetURL.replace("http", "https");
            return  getStream(targetURL, username, password, outputStream, userAgent);
        }

        if ( responseCode == HttpURLConnection.HTTP_NOT_FOUND) //404
                return responseCode;

        if (responseCode != HttpURLConnection.HTTP_OK) {
            if (true)
                Log.d(TAG, "Problem execute getStream: " + targetURL + " HTTP response: " +
                        responseCode + " username: " + username);
            throw new IOException("Response code is " + responseCode);
        }

        byte data[] = new byte[IO_BUFFER_SIZE];
        InputStream is = conn.getInputStream();
        copyStream(is, outputStream, data, IO_BUFFER_SIZE);
        outputStream.close();
        return responseCode;
    }

    public void copyStream(
            InputStream is,
            OutputStream os,
            byte[] buffer,
            int bufferSize)
            throws IOException {
        int len;
        while ((len = is.read(buffer, 0, bufferSize)) > 0) {
            os.write(buffer, 0, len);
        }
    }

    public HttpURLConnection getHttpConnection(
            String method,
            String targetURL,
            String username,
            String password,
            String userAgent)
            throws IOException {

        HttpURLConnection conn = getProperConnection(targetURL);

        String basicAuth = getHTTPBaseAuth(username, password);
        if (null != basicAuth) {
            conn.setRequestProperty("Authorization", basicAuth);
        }

        return getHttpConnection(method, targetURL, conn, userAgent);
    }

    public HttpURLConnection getHttpConnection(
            String method,
            String targetURL,
            HttpURLConnection conn,
            String userAgent)
            throws IOException {
        conn.setRequestProperty("User-Agent", userAgent);

        // Allow Inputs
        conn.setDoInput(true);
        // Don't use a cached copy.
        conn.setUseCaches(false);
        // Use a post method.
        if (method.length() > 0)
            conn.setRequestMethod(method);

        conn.setConnectTimeout(TIMEOUT_CONNECTION);
        conn.setReadTimeout(TIMEOUT_SOCKET);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("connection", "keep-alive");

        return isValidUri(targetURL) ? conn : null;
    }

    public static boolean isValidUri(String url) {
        Pattern pattern = Pattern.compile(URL_PATTERN);
        Matcher match = pattern.matcher(url);
        return match.matches();
    }

    public HttpURLConnection getProperConnection(String targetURL) throws IOException {
        URL url = new URL(targetURL);
        // Open a HTTP connection to the URL
        HttpURLConnection result = null;
        if (targetURL.startsWith("https://")) {
            //configureSSLdefault();
            result = (HttpsURLConnection) url.openConnection();
        } else
            result = (HttpURLConnection) url.openConnection();
        result.setRequestProperty("User-Agent", getUserAgent("_midpart_"));
        result.setRequestProperty("connection", "keep-alive");
        return result;

    }

    public  String getHTTPBaseAuth(String username, String password) {
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            return "Basic " + Base64.encodeToString(
                    (username + ":" + password).getBytes(), Base64.NO_WRAP);
        }
        return null;
    }

    public  String getUserAgent(String middlePart){
        return getUserAgentPrefix() + " "
                + middlePart + " " + getUserAgentPostfix();
    }

    private  String getUserAgentPrefix(){
        return "NGMOBILE";
    }

    private  String getUserAgentPostfix(){
        return "_ngmobile";
    }

    public static IntentFilter getReceiverIntent(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DOWNLOAD_ACTION);
        return intentFilter;
    }

    /*
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_VALUE_START = "start";
    public static final String EXTRA_VALUE_END = "end";
    */

    public void sendBroadcast(Context context, String url, boolean start,
                              int httpCode){
        if (previewOperation) {
            if (start != false) {
                return;
            }
        }

        Intent msg = new Intent(DOWNLOAD_ACTION);
        msg.putExtra(EXTRA_URL_ID, url);
        msg.putExtra(EXTRA_URL, url);

        msg.putExtra(EXTRA_TYPE, start ? EXTRA_VALUE_START: EXTRA_VALUE_END);
        msg.putExtra(EXTRA_HTTP_CODE, httpCode);
        msg.putExtra(EXTRA_PREVIEW, previewOperation);

        if (!start){ // end service
            deleteProgressListItem(url);
        }

        context.sendBroadcast(msg);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD_PHOTO.equals(action)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handleActionDownload(intent);
                    }
                }).start();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
