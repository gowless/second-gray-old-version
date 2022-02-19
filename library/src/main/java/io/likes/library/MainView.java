package io.likes.library;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.likes.library.storage.persistroom.model.Model;


public class MainView extends AppCompatActivity {


    // permission variables
    static boolean ASWP_JSCRIPT             = true;
    static boolean ASWP_FUPLOAD             = true;
    static boolean ASWP_CAMUPLOAD           = true;
    static boolean ASWP_ONLYCAM             = false;
    static boolean ASWP_MULFILE             = true;
    static boolean ASWP_LOCATION            = true;
    static boolean ASWP_RATINGS             = true;
    static boolean ASWP_PBAR                = true;
    static boolean ASWP_ZOOM                = false;
    static boolean ASWP_SFORM               = false;
    static boolean ASWP_OFFLINE             = false;
    static boolean ASWP_EXTURL              = true;
    static boolean ASWP_TAB                 = true;
    static boolean ASWP_EXITDIAL            = true;
    static boolean ASWP_CP                  = false;

    // security variables
    static boolean ASWP_CERT_VERIFICATION   = true;

    // configuration variables
    private static String ASWV_URL          = "";
    private String CURR_URL                 = ASWV_URL;;
    private static String ASWV_SHARE_URL    = "$ASWV_URL?share=";

    private static String ASWV_F_TYPE       = "*/*";



    public static String ASWV_HOST          = "";




    // careful with these variable names if altering
    WebView asw_view;
    WebView print_view;
    ProgressBar asw_progress;
    int asw_error_counter = 0;

    private String asw_pcam_message,asw_vcam_message;
    private ValueCallback<Uri> asw_file_message;
    private ValueCallback<Uri[]> asw_file_path;
    private final static int asw_file_req = 1;

    private final static int loc_perm = 1;
    private final static int file_perm = 2;

    private SecureRandom random = new SecureRandom();

    private static final String TAG = MainView.class.getSimpleName();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            Uri[] results = null;
            if (resultCode == Activity.RESULT_CANCELED) {
                if (requestCode == asw_file_req) {
                    // If the file request was cancelled (i.e. user exited camera),
                    // we must still send a null value in order to ensure that future attempts
                    // to pick files will still work.
                    asw_file_path.onReceiveValue(null);
                    return;
                }
            }
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == asw_file_req) {
                    if (null == asw_file_path) {
                        return;
                    }
                    ClipData clipData;
                    String stringData;
                    try {
                        clipData = intent.getClipData();
                        stringData = intent.getDataString();
                    }catch (Exception e){
                        clipData = null;
                        stringData = null;
                    }

                    if (clipData == null && stringData == null && (asw_pcam_message != null || asw_vcam_message != null)) {
                        results = new Uri[]{Uri.parse(asw_pcam_message != null ? asw_pcam_message:asw_vcam_message)};

                    } else {
                        if (null != clipData) { // checking if multiple files selected or not
                            final int numSelectedFiles = clipData.getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                results[i] = clipData.getItemAt(i).getUri();
                            }
                        } else {
                            try {
                                Bitmap cam_photo = (Bitmap) intent.getExtras().get("data");
                                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                cam_photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                                stringData = MediaStore.Images.Media.insertImage(this.getContentResolver(), cam_photo, null, null);
                            }catch (Exception ignored){}
                            results = new Uri[]{Uri.parse(stringData)};
                        }
                    }
                }
            }
            asw_file_path.onReceiveValue(results);
            asw_file_path = null;

        } else {
            if (requestCode == asw_file_req) {
                if (null == asw_file_message) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                asw_file_message.onReceiveValue(result);
                asw_file_message = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ASWV_URL = getIntent().getStringExtra("url");
        ASWV_HOST = aswm_host(ASWV_URL);
        Log.d("testing", ASWV_URL);
        // ------ PLAY AREA :: for debug purposes only ------ //

        // ------- PLAY AREA END ------ //

        // use Service Worker
        if (Build.VERSION.SDK_INT >= 24) {
            ServiceWorkerController swController = ServiceWorkerController.getInstance();
            swController.setServiceWorkerClient(new ServiceWorkerClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                    return null;
                }
            });
        }

        // prevent app from being started again when it is still alive in the background
        if (!isTaskRoot()) {
            finish();
            return;
        }

        // mLinkViewModel = ViewModelProviders.of(this).get(LinkViewModel.class);
        setContentView(R.layout.startstsas);


        asw_view = findViewById(R.id.msw_view);
        //asw_view.addJavascriptInterface(new JSInterface(), "JSOUT");
        //asw_view.addJavascriptInterface(new NetActivity2.WebViewJavaScriptInterface(this), "androidapp"); //
        // "androidapp is used to call methods exposed from javascript interface, in this example case print
        // method can be called by androidapp.print(String)"
        // load your data from the URL in web view

        // requesting new FCM token; updating final cookie variable



        // swipe refresh
        final ConstraintLayout pullfresh = findViewById(R.id.pullfresh);


        if (ASWP_PBAR) {
            asw_progress = findViewById(R.id.msw_progress);
        } else {
            findViewById(R.id.msw_progress).setVisibility(View.GONE);
        }
        Handler handler = new Handler();

        //Launching app rating request
        if (ASWP_RATINGS) {
            handler.postDelayed(new Runnable() { public void run() {  }}, 1000 * 60); //running request after few moments
        }

        //Getting basic device information


        //Getting GPS location of device if given permission
        if(ASWP_LOCATION && !check_permission(1)){
            ActivityCompat.requestPermissions(MainView.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, loc_perm);
        }


        //Webview settings; defaults are customized for best performance
        WebSettings webSettings = asw_view.getSettings();



        if(!ASWP_OFFLINE){
            webSettings.setJavaScriptEnabled(ASWP_JSCRIPT);
        }
        webSettings.setSaveFormData(ASWP_SFORM);
        webSettings.setSupportZoom(ASWP_ZOOM);
        webSettings.setGeolocationEnabled(ASWP_LOCATION);
        webSettings.setAllowFileAccess(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowFileAccessFromFileURLs(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }

        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);

        if(!ASWP_CP) {
            asw_view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
        }
        asw_view.setHapticFeedbackEnabled(false);

        // download listener
        asw_view.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {

            if(!check_permission(2)){
                ActivityCompat.requestPermissions(MainView.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, file_perm);
            }else {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription(getString(R.string.dl_downloading));
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                assert dm != null;
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), getString(R.string.dl_downloading2), Toast.LENGTH_LONG).show();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        asw_view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        asw_view.setVerticalScrollBarEnabled(false);
        asw_view.setWebViewClient(new Callback());

        //Reading incoming intents
        Intent read_int = getIntent();
        Log.d("INTENT", read_int.toUri(0));
        String uri = read_int.getStringExtra("uri");
        String share = read_int.getStringExtra("s_uri");
        String share_img = read_int.getStringExtra("s_img");

        if(share != null) {
            //Processing shared content
            Log.d("SHARE INTENT",share);
            Matcher matcher = urlPattern.matcher(share);
            String urlStr = "";
            if(matcher.find()){
                urlStr = matcher.group();
                if(urlStr.startsWith("(") && urlStr.endsWith(")")) {
                    urlStr = urlStr.substring(1, urlStr.length() - 1);
                }
            }
            String red_url = ASWV_SHARE_URL+"?text="+share+"&link="+urlStr+"&image_url=";
            //Toast.makeText(NetActivity2.this, "SHARE: "+red_url+"\nLINK: "+urlStr, Toast.LENGTH_LONG).show();
            aswm_view(red_url, false, asw_error_counter);

        }else if(share_img != null) {
            //Processing shared content
            Log.d("SHARE INTENT",share_img);
            Toast.makeText(MainView.this, share_img, Toast.LENGTH_LONG).show();
            aswm_view(ASWV_URL, false, asw_error_counter);

        }else if(uri != null) {
            //Opening notification
            Log.d("NOTIFICATION INTENT",uri);
            aswm_view(uri, false, asw_error_counter);

        }else{
            //Rendering the default URL
            Log.d("MAIN INTENT",ASWV_URL);
            aswm_view(ASWV_URL, false, asw_error_counter);
        }



        asw_view.setWebChromeClient(new WebChromeClient() {
            // handling input[type="file"]
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams){
                if(check_permission(2) && check_permission(3)) {
                    if (ASWP_FUPLOAD) {
                        asw_file_path = filePathCallback;
                        Intent takePictureIntent = null;
                        Intent takeVideoIntent = null;
                        if (ASWP_CAMUPLOAD) {
                            boolean includeVideo = false;
                            boolean includePhoto = false;

                            // Check the accept parameter to determine which intent(s) to include.
                            paramCheck:
                            for (String acceptTypes : fileChooserParams.getAcceptTypes()) {
                                // Although it's an array, it still seems to be the whole value.
                                // Split it out into chunks so that we can detect multiple values.
                                String[] splitTypes = acceptTypes.split(", ?+");
                                for (String acceptType : splitTypes) {
                                    switch (acceptType) {
                                        case "*/*":
                                            includePhoto = true;
                                            includeVideo = true;
                                            break paramCheck;
                                        case "image/*":
                                            includePhoto = true;
                                            break;
                                        case "video/*":
                                            includeVideo = true;
                                            break;
                                    }
                                }
                            }

                            // If no `accept` parameter was specified, allow both photo and video.
                            if (fileChooserParams.getAcceptTypes().length == 0) {
                                includePhoto = true;
                                includeVideo = true;
                            }

                            if (includePhoto) {
                                takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                if (takePictureIntent.resolveActivity(MainView.this.getPackageManager()) != null) {
                                    File photoFile = null;
                                    try {
                                        photoFile = create_image();
                                        takePictureIntent.putExtra("PhotoPath", asw_pcam_message);
                                    } catch (IOException ex) {
                                        Log.e(TAG, "Image file creation failed", ex);
                                    }
                                    if (photoFile != null) {
                                        asw_pcam_message = "file:" + photoFile.getAbsolutePath();
                                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                                    } else {
                                        takePictureIntent = null;
                                    }
                                }
                            }

                            if (includeVideo) {
                                takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                if (takeVideoIntent.resolveActivity(MainView.this.getPackageManager()) != null) {
                                    File videoFile = null;
                                    try {
                                        videoFile = create_video();
                                    } catch (IOException ex) {
                                        Log.e(TAG, "Video file creation failed", ex);
                                    }
                                    if (videoFile != null) {
                                        asw_vcam_message = "file:" + videoFile.getAbsolutePath();
                                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                                    } else {
                                        takeVideoIntent = null;
                                    }
                                }
                            }
                        }

                        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        if (!ASWP_ONLYCAM) {
                            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                            contentSelectionIntent.setType(ASWV_F_TYPE);
                            if (ASWP_MULFILE) {
                                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            }
                        }
                        Intent[] intentArray;
                        if (takePictureIntent != null && takeVideoIntent != null) {
                            intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
                        } else if (takePictureIntent != null) {
                            intentArray = new Intent[]{takePictureIntent};
                        } else if (takeVideoIntent != null) {
                            intentArray = new Intent[]{takeVideoIntent};
                        } else {
                            intentArray = new Intent[0];
                        }

                        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.fl_chooser));
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                        startActivityForResult(chooserIntent, asw_file_req);
                    }
                    return true;
                }else{
                    get_file();
                    return false;
                }
            }

            //Getting webview rendering progress
            @Override
            public void onProgressChanged(WebView view, int p) {
                if (ASWP_PBAR) {
                    asw_progress.setProgress(p);
                    if (p == 100) {
                        asw_progress.setProgress(0);
                    }
                }
            }

            // overload the geoLocations permissions prompt to always allow instantly as app permission was granted previously
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if(Build.VERSION.SDK_INT < 23 || check_permission(1)){
                    // location permissions were granted previously so auto-approve
                    callback.invoke(origin, true, false);
                } else {
                    // location permissions not granted so request them
                    ActivityCompat.requestPermissions(MainView.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, loc_perm);
                }
            }
        });
        if (getIntent().getData() != null) {
            String path     = getIntent().getDataString();
            /*
            If you want to check or use specific directories or schemes or hosts

            Uri data        = getIntent().getData();
            String scheme   = data.getScheme();
            String host     = data.getHost();
            List<String> pr = data.getPathSegments();
            String param1   = pr.get(0);
            */
            aswm_view(path, false, asw_error_counter);
        }
    }



    private void doWebViewPrint(String ss) {
        print_view.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            //use Service Worker
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(!url.contains("error")){
                    Log.d(TAG, url);

                    FormBuilder.INSTANCE.createRepoInstance(getApplicationContext()).updateLink(new Model(1, url));

                }
            }
        });
        // Generate an HTML document on the fly:
        print_view.loadDataWithBaseURL(null, ss, "text/html", "UTF-8", null);
    }

    @Override
    public void onPause() {
        super.onPause();
        asw_view.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        asw_view.onResume();
        //Coloring the "recent apps" tab header; doing it onResume, as an insurance


    }

    //Setting activity layout visibility
    private class Callback extends WebViewClient {
        public void onPageStarted(WebView view, String url, Bitmap favicon) {

        }

        public void onPageFinished(WebView view, String url) {
            findViewById(R.id.msw_view).setVisibility(View.VISIBLE);
            if(!url.contains("error")){
                Log.d(TAG, url);
                FormBuilder.INSTANCE.createRepoInstance(getApplicationContext()).updateLink(new Model(1, url));
            }
        }
        //For android below API 23
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getApplicationContext(), getString(R.string.went_wrong), Toast.LENGTH_SHORT).show();
            aswm_view("file:///android_asset/error.html", false, asw_error_counter);
        }

        //Overriding webview URLs
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            CURR_URL = url;
            return url_actions(view, url);
        }

        //Overriding webview URLs for API 23+ [suggested by github.com/JakePou]
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            CURR_URL = request.getUrl().toString();
            return url_actions(view, request.getUrl().toString());
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if(ASWP_CERT_VERIFICATION) {
                super.onReceivedSslError(view, handler, error);
            } else {
                handler.proceed(); // Ignore SSL certificate errors
            }
        }
    }

    //Random ID creation function to help get fresh cache every-time webview reloaded
    public String random_id() {
        return new BigInteger(130, random).toString(32);
    }

    //Opening URLs inside webview with request
    void aswm_view(String url, Boolean tab, int error_counter) {
        if(error_counter > 2){
            asw_error_counter = 0;
            aswm_exit();
        }else {
            asw_view.loadUrl(url);
        }
    }


    /*--- actions based on URL structure ---*/

    public boolean url_actions(WebView view, String url){
        boolean a = true;
        // show toast error if not connected to the network
        if (!ASWP_OFFLINE && !Inthernet.INSTANCE.isInternetAvailable(MainView.this)) {
            Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();

            // use this in a hyperlink to redirect back to default URL :: href="refresh:android"
        } else if (url.startsWith("refresh:")) {
            String ref_sch = (Uri.parse(url).toString()).replace("refresh:","");
            if(ref_sch.matches("URL")){
                CURR_URL = ASWV_URL;
            }
            pull_fresh();

            // use this in a hyperlink to launch default phone dialer for specific number :: href="tel:+919876543210"
        } else if (url.startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            startActivity(intent);

        }  else if (url.startsWith("rate:")) {
            final String app_package = getPackageName(); //requesting app package name from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app_package)));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app_package)));
            }

            // sharing content from your webview to external apps :: href="share:URL" and remember to place the URL you want to share after share:___
        } else if (url.startsWith("share:")) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, view.getTitle());
            intent.putExtra(Intent.EXTRA_TEXT, view.getTitle()+"\nVisit: "+(Uri.parse(url).toString()).replace("share:",""));
            startActivity(Intent.createChooser(intent, getString(R.string.share_w_friends)));

            // use this in a hyperlink to exit your app :: href="exit:android"
        } else if (url.startsWith("exit:")) {
            aswm_exit();

            // getting location for offline files
        } else if (ASWP_EXTURL && !aswm_host(url).equals(ASWV_HOST)) {
            aswm_view(url,true, asw_error_counter);

            // else return false for no special action
        } else {
            a = false;
        }
        return a;
    }

    //Getting host name
    public static String aswm_host(String url){
        if (url == null || url.length() == 0) {
            return "";
        }
        int dslash = url.indexOf("//");
        if (dslash == -1) {
            dslash = 0;
        } else {
            dslash += 2;
        }
        int end = url.indexOf('/', dslash);
        end = end >= 0 ? end : url.length();
        int port = url.indexOf(':', dslash);
        end = (port > 0 && port < end) ? port : end;
        Log.w("URL Host: ",url.substring(dslash, end));
        return url.substring(dslash, end);
    }

    //Reloading current page
    public void pull_fresh(){
        aswm_view((!CURR_URL.equals("")?CURR_URL:ASWV_URL),false, asw_error_counter);
    }



    //Checking permission for storage and camera for writing and uploading images
    public void get_file(){
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

        //Checking for storage permission to write images for upload
        if (ASWP_FUPLOAD && ASWP_CAMUPLOAD && !check_permission(2) && !check_permission(3)) {
            ActivityCompat.requestPermissions(MainView.this, perms, file_perm);

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (ASWP_FUPLOAD && !check_permission(2)) {
            ActivityCompat.requestPermissions(MainView.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, file_perm);

            //Checking for CAMERA permissions
        } else if (ASWP_CAMUPLOAD && !check_permission(3)) {
            ActivityCompat.requestPermissions(MainView.this, new String[]{Manifest.permission.CAMERA}, file_perm);
        }
    }



    // get cookie value
    public String get_cookies(String cookie){
        String value = "";
        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(ASWV_URL);
        String[] temp=cookies.split(";");
        for (String ar1 : temp ){
            if(ar1.contains(cookie)){
                String[] temp1=ar1.split("=");
                value = temp1[1];
                break;
            }
        }
        return value;
    }

    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"+"(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"+"[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);






    public static int aswm_fcm_id(){
        //Date now = new Date();
        //Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(now));
        return 1;
    }


    //Checking if particular permission is given or not
    public boolean check_permission(int permission){
        switch(permission){
            case 1:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            case 2:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            case 3:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        }
        return false;
    }

    //Creating image file for upload
    private File create_image() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".jpg", sd_directory);
    }

    //Creating video file for upload
    private File create_video() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".3gp", sd_directory);
    }





    //Checking if users allowed the requested permissions or not
    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        }
    }

    //Action on back key tap/click
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (asw_view.canGoBack()) {
                    asw_view.goBack();
                } else {
                    if(ASWP_EXITDIAL) {

                    }else{
                        finish();
                    }
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void aswm_exit(){
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }



    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState ){
        super.onSaveInstanceState(outState);
        asw_view.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        asw_view.restoreState(savedInstanceState);
    }
}
