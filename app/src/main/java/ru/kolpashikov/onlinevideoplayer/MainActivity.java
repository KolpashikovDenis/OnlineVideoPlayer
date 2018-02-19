package ru.kolpashikov.onlinevideoplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubePlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ru.kolpashikov.onlinevideoplayer.CommonClasses.ChannelItem;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.Const;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.OnVideoSelectedListener;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.RVAdapter;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.StackItem;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.YoutubeItem;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.YoutubeItemEx;
import ru.kolpashikov.onlinevideoplayer.fragments.ContentFragment;
import ru.kolpashikov.onlinevideoplayer.fragments.FragmentVideo;
import ru.kolpashikov.onlinevideoplayer.fragments.NoConnectionFragment;

import static ru.kolpashikov.onlinevideoplayer.CommonClasses.Const.PLAYLIST_ID;
import static ru.kolpashikov.onlinevideoplayer.CommonClasses.Const.VIDEO_ID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, YouTubePlayer.OnFullscreenListener,
        OnVideoSelectedListener{

    private static final String LOG = "mlogs";
    private String baseUrl = "https://www.youtube.com";

    private boolean isFullScreen = false;

    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private Menu menu;
    private EditText etSearch;
    private ConstraintLayout searchPanel;

    private List<ChannelItem> menuItemList;
    private List<YoutubeItemEx> videoList;
    private RVAdapter videoAdapter;
    private int count = 0;

    private ContentFragment contentFragment;
    private NoConnectionFragment noConnectionFragment;
    private FragmentVideo fragmentVideo;
    private View fragmentVideoView;
    private ParseContentTask threadSelectVideoTask;
    private Stack<StackItem> history;
    private StackItem prevItem;
    String videoId;
    String fullUrl;


    private FragmentTransaction ft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etSearch = (EditText)findViewById(R.id.etSearch);
        searchPanel = (ConstraintLayout)findViewById(R.id.search_panel);

        drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        menu = navigationView.getMenu();
        menu.clear();
        menuItemList = new ArrayList<>();
        videoList = new ArrayList<>();
        videoAdapter = new RVAdapter(this, videoList);
        new ParseContentTask(videoList).execute(baseUrl);
        history = new Stack<>();
        videoId = "";
        fullUrl = Const.BASE_URL;
        prevItem = new StackItem(fullUrl, videoId);

        contentFragment= new ContentFragment();

        noConnectionFragment = new NoConnectionFragment();
        fragmentVideo = (FragmentVideo)getFragmentManager().findFragmentById(R.id.fragment_video_container);
        fragmentVideoView = findViewById(R.id.fragment_video_container);
        fragmentVideoView.setVisibility(View.GONE);

        ft = getSupportFragmentManager().beginTransaction();
        if(isOnline()) {
            ft.replace(R.id.rv_container, contentFragment);
        } else {
            ft.replace(R.id.rv_container, noConnectionFragment);
        }
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }else if(isFullScreen){
            fragmentVideo.backnormal();
            isFullScreen = false;
        } else if(!history.empty()){
            videoList.clear();
            videoAdapter.notifyDataSetChanged();
            StackItem prev = history.pop();

            if(prev.resourceId.isEmpty()){
                fragmentVideoView.setVisibility(View.GONE);
            } else {
                fragmentVideoView.setVisibility(View.VISIBLE);
                fragmentVideo.setVideoId(prev.resourceId);
            }

            new ParseContentTask(videoList).execute(prev.urlToResource);
        } else super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id){
            case R.id.action_settings:
                break;
            case R.id.action_search:
                searchPanel.setVisibility(View.VISIBLE);
                /*
                etSearch.setHint("Search content");
                etSearch.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
                InputMethodManager mm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                mm.showSoftInput(etSearch, 1);

                etSearch.setOnKeyListener(new View.OnKeyListener(){
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        int keyEvent = event.getAction();
                        if(keyEvent == KeyEvent.ACTION_DOWN){
                            switch(keyCode){
                                case KeyEvent.KEYCODE_DPAD_CENTER:
                                case KeyEvent.KEYCODE_ENTER:
                                    Toast.makeText(MainActivity.this, etSearch.getText(),
                                            Toast.LENGTH_SHORT).show();

                                    try {
                                        String searchUrl = Const.BASE_URL + "/results?search_query="
                                                + URLEncoder.encode(etSearch.getText().toString(), "UTF-8");
                                        new ParseContentTask(videoList).execute(searchUrl);
                                    }catch (UnsupportedEncodingException e){
                                        Toast.makeText(MainActivity.this, "UnsupportedEncodingException\n"
                                                +e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                                    return true;
                                default:
                                    break;
                            }
                        }
                        return false;
                    }
                });
                */

                break;
            default:
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String fullUrl = baseUrl + ((ChannelItem)menuItemList.get(id)).channelUrl;
//        String videoId = "";

//        videoAdapter = contentFragment.getAdapter();
        videoList.clear();
        videoAdapter.notifyDataSetChanged();
        fragmentVideo.setVideoId("");
        fragmentVideoView.setVisibility(View.GONE);
        threadSelectVideoTask = new ParseContentTask(videoList);
        threadSelectVideoTask.execute(fullUrl);

//        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        switch(id){
            default:
        }

        drawer.closeDrawer(Gravity.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        new InitialNavDrawerTask(menuItemList).execute();
        new ParseContentTask(videoList).execute(Const.BASE_URL);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    boolean isOnline(){
        String cs = CONNECTIVITY_SERVICE;
        boolean result = false;
        ConnectivityManager cm = (ConnectivityManager)getSystemService(cs);
        try {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            result = (networkInfo != null && networkInfo.isConnectedOrConnecting());
        } catch (java.lang.NullPointerException e){
            result = false;
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void onVideoSelected(int position) {

        history.push(prevItem);

        String videoId = videoList.get(position).ytId;
        String fullUrl = videoList.get(position).urlResoucre;

        prevItem = new StackItem(fullUrl, videoId);

        videoList.clear();
        videoAdapter.notifyDataSetChanged();

        fragmentVideoView.setVisibility(View.VISIBLE);
        fragmentVideo.setVideoId(videoId);
        threadSelectVideoTask = new ParseContentTask(videoList);
        threadSelectVideoTask.execute(fullUrl);
    }

    @Override
    public List<YoutubeItemEx> getVideoList() {
        return videoList;
    }

    @Override
    public RVAdapter getVideoAdapter() {
        return videoAdapter;
    }

    @Override
    public void startAsyncTask(String url){
        new ParseContentTask(videoList).execute(url);
    }

    @Override
    public void onFullscreen(boolean b) {
        isFullScreen = b;
    }

    void layout(){

    }

    int dp2px(int dp){
        return (int)(dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    void setLayoutSize(View view, int width, int height){
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

    public class ParseContentTask extends AsyncTask<String, YoutubeItemEx, Void> {
        List<YoutubeItemEx> list;
        final String _start = "ytInitialData";

        public ParseContentTask(List<YoutubeItemEx> list) {
            this.list = list;
            list.clear();
        }

        @Override
        protected Void doInBackground(String... strings) {
            String mainUrl = strings[0];
            try {
                getContent(mainUrl);
            }catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }

        private void getContent(String url)throws IOException{
            BufferedReader reader = null;
            try{
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection)u.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9"
                        +",image/webp,image/apng,*/*;q=0.8");
                conn.setRequestProperty("Accept-Lareanguage", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36 OPR/47.0.2631.80");
                conn.setReadTimeout(10000);
                conn.connect();
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    sb.append(line);
                }

                int iStart = sb.indexOf(_start)+_start.length();
                while(sb.charAt(iStart) != '{') iStart++;

                int nTmp = 1;
                int nCount = iStart + 1;
                do{
                    if(sb.charAt(nCount) == '{'){
                        nTmp++;
                    }
                    if(sb.charAt(nCount) == '}'){
                        nTmp--;
                    }
                    nCount++;
                } while (nTmp != 0);
                line = sb.substring(iStart, nCount);

                // PARSE JSON DATA
                JSONObject root;
                try{
                    root = new JSONObject(line);
                    // Главная или канал
                    if(root.getJSONObject("contents").has("twoColumnBrowseResultsRenderer")){
                        JSONArray tmpArray = root.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer")
                                .getJSONArray("tabs").getJSONObject(0).getJSONObject("tabRenderer")
                                .getJSONObject("content").getJSONObject("sectionListRenderer").getJSONArray("contents");

                        /*JSONObject tmp = tmpArray.getJSONObject(0).getJSONObject("itemSectionRenderer")
                                .getJSONArray("contents").getJSONObject(1).getJSONObject("shelfRenderer")
                                .getJSONObject("content");
                        if(tmp.has("gridRenderer")){
                            // Здесь "Главная"
                            parseMainPage(tmpArray);
                        } else if(tmp.has("horizontalListRenderer")){
                            // Здесь выбор канала
                            parseChannelPage(tmpArray);
                        } else{
                            Log.d(_start, tmp.names().toString());
                        }*/

                        // Главная страница
                        if(root.has("adSafetyReason")){
                            parseMainPage(tmpArray);
                        } else if(root.has("microformat")){ // Здесь канал
                            parseChannelPage(tmpArray);
                        } else {
                            Log.d(_start, root.names().toString());
                        }

                        return;
                    }

                    // Быбор ролика или плейлиста
                    if(root.getJSONObject("contents").has("twoColumnWatchNextResults")){
                        if(root.getJSONObject("contents").getJSONObject("twoColumnWatchNextResults")
                                .has("secondaryResults")){
                            JSONArray items = root.getJSONObject("contents").getJSONObject("twoColumnWatchNextResults")
                                    .getJSONObject("secondaryResults").getJSONObject("secondaryResults")
                                    .getJSONArray("results");
                            parseItemPage(items);
                        } else if(root.getJSONObject("contents").getJSONObject("twoColumnWatchNextResults")
                                .has("playlist")) {
                            // TODO: Вот тут реализовать не разбор плейлиста, а вызов дочерней активности
                            JSONObject playlist = root.getJSONObject("contents").getJSONObject("twoColumnWatchNextResults")
                                    .getJSONObject("playlist").getJSONObject("playlist");
                            parsePlaylistPage(playlist);
                        } else {
                            Log.d(_start, root.getJSONObject("contents").getJSONObject("twoColumnWatchNextResults").names().toString());
                        }

                        return;
                    }

                    // Поиск
                    if(root.getJSONObject("contents").has("twoColumnSearchResultsRenderer")){
                        JSONArray tmpArray = root.getJSONObject("contents").getJSONObject("twoColumnSearchResultsRenderer")
                                .getJSONObject("primaryContents").getJSONObject("sectionListRenderer")
                                .getJSONArray("contents").getJSONObject(0).getJSONObject("itemSectionRenderer")
                                .getJSONArray("contents");
                        parseSearchPage(tmpArray);
                        return;
                    }

                }catch(JSONException e){
                    e.printStackTrace();
                }
            }finally{
                if(reader != null){
                    reader.close();
                }
            }
        }
//------------------------------------------------------------------------------------
        private void parseMainPage(JSONArray localRoot){
            String videoId, linkToPreview, title, publishedTimeText, viewCountText, urlToVideo
                    ,author, linkToAuthorChannel, duration;
            int typeOfId;
            try {
                for (int i = 0; i < localRoot.length(); i++) {
                    JSONArray items;

                    if(localRoot.getJSONObject(i).getJSONObject("itemSectionRenderer")
                            .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                            .getJSONObject("content").has("gridRenderer")) {
                        items = localRoot.getJSONObject(i).getJSONObject("itemSectionRenderer")
                                .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                                .getJSONObject("content").getJSONObject("gridRenderer").getJSONArray("items");
                    } else if(localRoot.getJSONObject(i).getJSONObject("itemSectionRenderer")
                            .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                            .getJSONObject("content").has("horizontalListRenderer")){
                        items = localRoot.getJSONObject(i).getJSONObject("itemSectionRenderer")
                                .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                                .getJSONObject("content").getJSONObject("horizontalListRenderer").getJSONArray("items");
                    } else {
                        Log.d(_start, "-"+i+"-");
                        Log.d(_start, localRoot.getJSONObject(i).getJSONObject("itemSectionRenderer")
                                .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                                .getJSONObject("content").names().toString());
                        continue;
                    }
                    for(int k = 0; k < items.length(); k++){
                        JSONObject item = items.getJSONObject(k).getJSONObject("gridVideoRenderer");
                        if(item.has("videoId")){
                            typeOfId = Const.VIDEO_ID;
                            videoId = item.getString("videoId");
                            linkToPreview = item.getJSONObject("thumbnail").getJSONArray("thumbnails").getJSONObject(0)
                                    .getString("url");
                            title = item.getJSONObject("title").getString("simpleText");
                            publishedTimeText = item.getJSONObject("publishedTimeText").getString("simpleText");
                            viewCountText = item.getJSONObject("viewCountText").getString("simpleText");
                            urlToVideo = Const.BASE_URL + item.getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                                    .getString("url");
                            author = item.getJSONObject("shortBylineText").getJSONArray("runs").getJSONObject(0).getString("text");
                            linkToAuthorChannel = Const.BASE_URL+item.getJSONObject("shortBylineText").getJSONArray("runs")
                                    .getJSONObject(0).getJSONObject("navigationEndpoint")
                                    .getJSONObject("webNavigationEndpointData").getString("url");
                            duration = item.getJSONArray("thumbnailOverlays").getJSONObject(0)
                                    .getJSONObject("thumbnailOverlayTimeStatusRenderer").getJSONObject("text")
                                    .getString("simpleText");

                            publishProgress(new YoutubeItemEx(typeOfId, videoId, linkToPreview, title, urlToVideo,
                                    author, viewCountText, duration, publishedTimeText, linkToAuthorChannel,
                                    ""));
                        } /*else if(item.has("playlistId")){
                            Log.d(_start, item.names().toString());

                        } else if(item.has("channelId")){
                            Log.d(_start, item.names().toString());
                        } */else {
                            Log.d(_start, item.names().toString());
                        }
                    }

                }
            }catch(JSONException e){
                e.printStackTrace();
            }

        }

        private void parseItemPage(JSONArray localRoot){
            String videoId, linkToPreview, title, publishedTimeText, viewCountText, urlToVideo
                    ,author, linkToAuthorChannel, duration, channelThumbnail;
            int typeOfId;
            try{
                for(int i = 0; i < localRoot.length(); i++){
                    JSONObject item = localRoot.getJSONObject(i);
                    if(item.has("compactAutoplayRenderer")){
                        JSONObject subItem = item.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                                .getJSONObject(0).getJSONObject("compactVideoRenderer");
                        typeOfId = Const.VIDEO_ID;
                        videoId = subItem.getString("videoId");
                        linkToPreview = subItem.getJSONObject("thumbnail").getJSONArray("thumbnails")
                                .getJSONObject(0).getString("url");
                        title = subItem.getJSONObject("title").getString("simpleText");
                        publishedTimeText = "";
                        viewCountText = subItem.getJSONObject("viewCountText").getString("simpleText");
                        urlToVideo = Const.BASE_URL+subItem.getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                                .getString("url");
                        author = subItem.getJSONObject("shortBylineText").getJSONArray("runs")
                                .getJSONObject(0).getString("text");
                        linkToAuthorChannel = Const.BASE_URL+subItem.getJSONObject("shortBylineText").getJSONArray("runs")
                                .getJSONObject(0).getJSONObject("navigationEndpoint")
                                .getJSONObject("webNavigationEndpointData").getString("url");
                        duration = subItem.getJSONArray("thumbnailOverlays").getJSONObject(0)
                                .getJSONObject("thumbnailOverlayTimeStatusRenderer").getJSONObject("text")
                                .getString("simpleText");
                        channelThumbnail = Const.BASE_URL+subItem.getJSONObject("channelThumbnail").getJSONArray("thumbnails")
                                .getJSONObject(0).getString("url");

                        publishProgress(new YoutubeItemEx(typeOfId, videoId, linkToPreview, title, urlToVideo,
                                author, viewCountText, duration, publishedTimeText, linkToAuthorChannel,
                                channelThumbnail));
                    }else if(item.has("compactVideoRenderer")){
                        typeOfId = Const.VIDEO_ID;
                        videoId = item.getJSONObject("compactVideoRenderer").getString("videoId");
                        linkToPreview = item.getJSONObject("compactVideoRenderer").getJSONObject("thumbnail")
                                .getJSONArray("thumbnails").getJSONObject(0).getString("url");
                        title = item.getJSONObject("compactVideoRenderer").getJSONObject("title").getString("simpleText");
                        publishedTimeText= "";
                        if(item.getJSONObject("compactVideoRenderer").getJSONObject("viewCountText")
                                .has("simpleText")) {
                            viewCountText = item.getJSONObject("compactVideoRenderer").getJSONObject("viewCountText")
                                    .getString("simpleText");
                        } else {
                            Log.d(_start, "--viewCountText--");
                            Log.d(_start, item.getJSONObject("compactVideoRenderer").getJSONObject("viewCountText")
                                    .names().toString());
                            viewCountText ="n/a";
                        }
                        urlToVideo = Const.BASE_URL+item.getJSONObject("compactVideoRenderer").getJSONObject("navigationEndpoint")
                                .getJSONObject("webNavigationEndpointData").getString("url");
                        author = item.getJSONObject("compactVideoRenderer").getJSONObject("shortBylineText")
                                .getJSONArray("runs").getJSONObject(0).getString("text");
                        linkToAuthorChannel = Const.BASE_URL + item.getJSONObject("compactVideoRenderer")
                                .getJSONObject("shortBylineText").getJSONArray("runs").getJSONObject(0)
                                .getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                                .getString("url");
                        if(item.getJSONObject("compactVideoRenderer").getJSONArray("thumbnailOverlays")
                                .getJSONObject(0).has("thumbnailOverlayTimeStatusRenderer")) {
                            duration = item.getJSONObject("compactVideoRenderer").getJSONArray("thumbnailOverlays")
                                    .getJSONObject(0).getJSONObject("thumbnailOverlayTimeStatusRenderer")
                                    .getJSONObject("text").getString("simpleText");
                        } else
                            duration = "";
                        channelThumbnail = Const.BASE_URL + item.getJSONObject("compactVideoRenderer").getJSONObject("channelThumbnail")
                                .getJSONArray("thumbnails").getJSONObject(0).getString("url");

                        publishProgress(new YoutubeItemEx(typeOfId, videoId, linkToPreview, title, urlToVideo,
                                author, viewCountText, duration, publishedTimeText, linkToAuthorChannel,
                                channelThumbnail));

                    }else if(item.has("compactRadioRenderer")){

                    }else{
                        Log.d(_start, item.names().toString());
                    } // else
                }
            }catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void parseSearchPage(JSONArray localRoot){
            String videoId, linkToPreview, title, publishedTimeText, viewCountText, urlToVideo
                    ,author, linkToAuthorChannel, duration, channelThumbnail;
            int typeOfId;
            try {
                for (int i = 0; i < localRoot.length(); i++) {
                    JSONObject item = localRoot.getJSONObject(i);
                    if (item.has("videoRenderer")) {
                        typeOfId = Const.VIDEO_ID;
                        videoId = item.getJSONObject("videoRenderer").getString("videoId");
                        linkToPreview = item.getJSONObject("videoRenderer").getJSONObject("thumbnail")
                                .getJSONArray("thumbnails").getJSONObject(0).getString("url");
                        title = item.getJSONObject("videoRenderer").getJSONObject("title").getString("simpleText");
                        publishedTimeText = item.getJSONObject("videoRenderer").getJSONObject("publishedTimeText")
                                .getString("simpleText");
                        viewCountText = item.getJSONObject("videoRenderer").getJSONObject("viewCountText")
                                .getString("simpleText");
                        urlToVideo = Const.BASE_URL+item.getJSONObject("videoRenderer").getJSONObject("navigationEndpoint").
                                getJSONObject("webNavigationEndpointData").getString("url");
                        author = item.getJSONObject("videoRenderer").getJSONObject("shortBylineText").getJSONArray("runs")
                                .getJSONObject(0).getString("text");
                        linkToAuthorChannel= item.getJSONObject("videoRenderer").getJSONObject("shortBylineText")
                                .getJSONArray("runs").getJSONObject(0).getJSONObject("navigationEndpoint")
                                .getJSONObject("webNavigationEndpointData").getString("url");
                        duration = item.getJSONObject("videoRenderer").getJSONObject("lengthText").getString("simpleText");
                        channelThumbnail = item.getJSONObject("videoRenderer").getJSONObject("channelThumbnail")
                                .getJSONArray("thumbnails").getJSONObject(0).getString("url");

                        publishProgress(new YoutubeItemEx(typeOfId,videoId, linkToPreview, title, urlToVideo,
                                author, viewCountText, duration, publishedTimeText, linkToAuthorChannel,channelThumbnail));
                    } else if(item.has("channelRenderer")){
                        typeOfId = Const.CHANNEL_ID;
                        videoId = item.getJSONObject("channelRenderer").getString("channelId");
                        linkToPreview = item.getJSONObject("channelRenderer").getJSONObject("thumbnail")
                                .getJSONArray("thumbnails").getJSONObject(0).getString("url");
                        title = item.getJSONObject("channelRenderer").getJSONObject("title").getString("simpleText");
                        publishedTimeText = "";
                        viewCountText = item.getJSONObject("channelRenderer").getJSONObject("videoCountText")
                                .getJSONArray("runs").getJSONObject(0).getString("text");
                        urlToVideo = Const.BASE_URL+item.getJSONObject("channelRenderer").getJSONObject("navigationEndpoint")
                                .getJSONObject("webNavigationEndpointData").getString("url");
                        author = item.getJSONObject("channelRenderer").getJSONObject("shortBylineText")
                                .getJSONArray("runs").getJSONObject(0).getString("text");
                        linkToAuthorChannel = Const.BASE_URL+item.getJSONObject("channelRenderer").getJSONObject("shortBylineText")
                                .getJSONArray("runs").getJSONObject(0).getJSONObject("navigationEndpoint")
                                .getJSONObject("webNavigationEndpointData").getString("url");
                        duration = "";
                        channelThumbnail = "";

                        publishProgress(new YoutubeItemEx(typeOfId, videoId, linkToPreview, title, urlToVideo, author,
                                viewCountText, duration, publishedTimeText, linkToAuthorChannel, channelThumbnail));
                    } else if(item.has("playlistRenderer")){
                        typeOfId = Const.PLAYLIST_ID;
                        videoId = item.getJSONObject("playlistRenderer").getString("playlistId");
                        linkToPreview = item.getJSONObject("playlistRenderer").getJSONArray("thumbnails").getJSONObject(0)
                                .getJSONArray("thumbnails").getJSONObject(0).getString("url");
                        title = item.getJSONObject("playlistRenderer").getJSONObject("title").getString("simpleText");
                        publishedTimeText = "";
                        viewCountText = item.getJSONObject("playlistRenderer").getJSONObject("videoCountText").getJSONArray("runs")
                                .getJSONObject(0).getString("text");
                        urlToVideo = Const.BASE_URL+item.getJSONObject("playlistRenderer").getJSONObject("navigationEndpoint")
                                .getJSONObject("webNavigationEndpointData").getString("url");
                        author = item.getJSONObject("playlistRenderer").getJSONObject("shortByLineText").getJSONArray("runs")
                                .getJSONObject(0).getString("text");
                        linkToAuthorChannel= item.getJSONObject("playlistRenderer").getJSONObject("shortBylineText").getJSONArray("runs")
                                .getJSONObject(0).getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                                .getString("url");
                        duration = "";
                        channelThumbnail = "";

                        publishProgress(new YoutubeItemEx(typeOfId, videoId, linkToPreview, title, urlToVideo, author, viewCountText,
                                duration, publishedTimeText, linkToAuthorChannel, channelThumbnail));
                    }else {
                        Log.d(_start, item.names().toString());
                    }
                }
            }catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void parsePlaylistPage(JSONObject localRoot){
            String videoId, linkToPreview, title, publishedTimeText, viewCountText, urlToVideo
                    ,author, linkToAuthorChannel, duration;
            int typeOfId;
//            try{
//
//            }catch (JSONException e){
//                e.printStackTrace();
//            }
        }

        private void parseChannelPage(JSONArray localRoot){

        }

        @Override
        protected void onProgressUpdate(YoutubeItemEx... values) {
            super.onProgressUpdate(values);

            videoList.add(values[0]);
            int index = videoList.size();
            videoAdapter.notifyItemInserted(index);
        }
    }

    private class InitialNavDrawerTask extends AsyncTask<Void, ChannelItem, Void> {
        final String LOG = "mLogs";
        private String url = "https://www.youtube.com";
        final String _start = "ytInitialGuideData";
        final String _end = "loadGuideDataHook";
        private List<ChannelItem> list;

        public InitialNavDrawerTask(List<ChannelItem> _list) {
            list = _list;
            list.clear();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                getContent(url);
            } catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(ChannelItem... values) {
            super.onProgressUpdate(values);
            menu.add(0, values[0].id, values[0].id, values[0].channelName);
            list.add(new ChannelItem(values[0].id, values[0].channelUrl, values[0].channelName, null));
        }

        private void getContent(String path) throws IOException {
            BufferedReader reader = null;
            try{
                URL url = new URL(path);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

                c.setRequestProperty("Accept-Lareanguage", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36 OPR/47.0.2631.80");
                c.setReadTimeout(10000);
                c.connect();
                reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder buf = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line);
                }

                int iStart = buf.indexOf(_start);
                buf.delete(0, iStart + _start.length());
                iStart = buf.indexOf(_start);

                int i = iStart + _start.length();
                while (buf.charAt(i) != '{') {
                    i++;
                }
                iStart = i;

                i = buf.indexOf(_end);
                while (buf.charAt(i)!=';'){
                    i--;
                }
                int iEnd = i;
                line = buf.substring(iStart, iEnd);

                try{
                    JSONObject root = new JSONObject(line);
                    JSONArray array = root.getJSONArray("items");
                    int count = 0;
                    for(int k = 0; k < array.length(); k++){
                        JSONArray tmpArray = array.getJSONObject(k).getJSONObject("guideSectionRenderer")
                                .getJSONArray("items");
                        for(int m = 0; m < tmpArray.length(); m++){
                            String itemTitle = tmpArray.getJSONObject(m).getJSONObject("guideEntryRenderer")
                                    .getString("title");
                            String itemUrl = tmpArray.getJSONObject(m).getJSONObject("guideEntryRenderer")
                                    .getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                                    .getString("url");

                            publishProgress(new ChannelItem(count, itemUrl, itemTitle, null));
                            count++;
                        }
                    }
                }catch(JSONException e){
                    e.printStackTrace();
//                    Log.d(LOG, "JSONObject exception: "+ e.getMessage());
                }
            }finally{
                if( reader != null){
                    reader.close();
                }
            }
        }

        Bitmap getBitmap(String urlImage){
            Bitmap b = null;
            try{
                InputStream in = new URL(urlImage).openStream();
                b = BitmapFactory.decodeStream(in);
            }catch (Exception e){
                b = null;
            }
            return b;
        }
    }

    private class SelectVideoIdTask extends AsyncTask<String, YoutubeItem, Void>{
        private String baseUrl = "https://www.youtube.com";
        private String _start = "ytInitialData";
        private String _end = "ytInitialPlayerResponse";
        private final String LOG = "svIdt";

        private List<YoutubeItem> list; // сюда получим ссылку на videoList

        private RVAdapter adapter;  //  = contentFragment.getAdapter();

        public SelectVideoIdTask(List<YoutubeItem> _list) {
            list = _list;
//            adapter = contentFragment.getAdapter();
        }

        @Override
        protected Void doInBackground(String... videoId) {
            try{
                getContent(videoId[0]);
            } catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }

        private void parseMainJson(JSONObject root) throws JSONException{
            String strId = "", fullUrl = "", name = "", count_of_view = "", duration = "",
                    publicationDate = "", author = "", link_to_image = "";
            int typeOfId = 0;
            Bitmap b;

            JSONArray groupItems = root.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs").getJSONObject(0).getJSONObject("tabRenderer")
                    .getJSONObject("content").getJSONObject("sectionListRenderer").getJSONArray("contents");
            for( int i = 0; i < groupItems.length(); i++){
                // TODO: проверка некоторых ключей для правильного парсинга
                /*
                JSONArray contentItems = groupItems.getJSONObject();
                */

                JSONArray items = groupItems.getJSONObject(i).getJSONObject("itemSectionRenderer")
                        .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                        .getJSONObject("content").getJSONObject("gridRenderer").getJSONArray("items");
                for(int k = 0; k < items.length(); k++){
                    if(items.getJSONObject(k).getJSONObject("gridVideoRenderer").has("videoId")){
                        strId = items.getJSONObject(k).getJSONObject("gridVideoRenderer").getString("videoId");
                        typeOfId = VIDEO_ID;
                    } else {
                        strId = items.getJSONObject(k).getJSONObject("gridVideoRenderer").getString("playlistId");
                        typeOfId = PLAYLIST_ID;
                    }

                    name = items.getJSONObject(k).getJSONObject("gridVideoRenderer").getJSONObject("title")
                            .getString("simpleText");

                    fullUrl = baseUrl + items.getJSONObject(k).getJSONObject("gridVideoRenderer")
                            .getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                            .getString("url");

                    count_of_view = items.getJSONObject(k).getJSONObject("gridVideoRenderer")
                            .getJSONObject("viewCountText").getString("simpleText");

                    duration = items.getJSONObject(k).getJSONArray("thumbnailOverlays").getJSONObject(0)
                            .getJSONObject("thumbnailOverlayTimeStatusRenderer").getJSONObject("text")
                            .getString("simpleText");

                    author = items.getJSONObject(k).getJSONObject("shortBylineText").getJSONArray("runs")
                            .getJSONObject(0).getString("text");

                    publicationDate = items.getJSONObject(k).getJSONObject("publishedTimeText").getString("simpleText");

                    JSONArray thumbnails = items.getJSONObject(k).getJSONObject("gridVideoRenderer")
                            .getJSONObject("thumbnail").getJSONArray("thumbnails");
                    if(thumbnails.length() > 1){
                        link_to_image = thumbnails.getJSONObject(1).getString("url");
                    } else {
                        link_to_image = thumbnails.getJSONObject(0).getString("url");
                    }

                    b = getBitmap(link_to_image);
                    publishProgress(new YoutubeItem(strId, fullUrl, name, count_of_view, duration,
                            publicationDate, author, typeOfId, b));
                }
            }

        }

        private void parseJson01(JSONObject root) throws JSONException{
            String strId = "", fullUrl = "", name = "", count_of_view = "", duration = "",
                    publicationDate = "", author = "", link_to_image = "";
            int typeOfId = 0;
            Bitmap b;

            JSONArray tmpList = root.getJSONObject("contents")
                    .getJSONObject("twoColumnWatchNextResults").getJSONObject("secondaryResults")
                    .getJSONObject("secondaryResults").getJSONArray("results");

            for(int i = 0; i < tmpList.length(); i++){
                JSONObject tmpItem = tmpList.getJSONObject(i);

                if(tmpItem.has("compactVideoRenderer")){
                    // compactVideoRenderer
                    Log.d(LOG, "compactVideoRenderer");
                    if(tmpItem.getJSONObject("compactVideoRenderer").has("videoId")){
                        strId = tmpItem.getJSONObject("compactVideoRenderer").getString("videoId");
                        typeOfId = VIDEO_ID;
                    }else{
                        strId = tmpItem.getJSONObject("compactVideoRenderer").getString("playlistId");
                        typeOfId = PLAYLIST_ID;
                    }
                    // Имя ролика
                    name = tmpItem.getJSONObject("compactVideoRenderer").getJSONObject("title")
                            .getString("simpleText");

                    // Полный урл ролика или плейлиста
                    fullUrl= baseUrl+ tmpItem.getJSONObject("compactVideoRenderer").getJSONObject("navigationEndpoint")
                            .getJSONObject("webNavigationEndpointData").getString("url");

                    // Автор
                    author = tmpItem.getJSONObject("compactVideoRenderer")
                            .getJSONObject("longBylineText").getJSONArray("runs")
                            .getJSONObject(0).getString("text");

                    // Количество просмотров
                    count_of_view = tmpItem.getJSONObject("compactVideoRenderer")
                            .getJSONObject("viewCountText").getString("simpleText");

                    // Длительность ролика
                    duration = tmpItem.getJSONObject("compactVideoRenderer")
                            .getJSONObject("lengthText").getString("simpleText");

                    // Время со дня публикации
                    publicationDate = "";

                    // Ссылка на изображение
                    JSONArray arrImages = tmpItem.getJSONObject("compactVideoRenderer")
                            .getJSONObject("thumbnail").getJSONArray("thumbnails");

                    if(arrImages.length() > 1){
                        link_to_image = arrImages.getJSONObject(1).getString("url");
                    } else {
                        link_to_image = arrImages.getJSONObject(0).getString("url");
                    }

                } else if(tmpItem.has("compactRadioRenderer")){
                    // compactRadioRenderer
                    if(tmpItem.getJSONObject("compactRadioRenderer").has("playlistId")){
                        strId = tmpItem.getJSONObject("compactRadioRenderer").getString("playlistId");
                        typeOfId = PLAYLIST_ID;
                    } else {
                        strId = tmpItem.getJSONObject("compactRadioRenderer").getString("videoId");
                        typeOfId = VIDEO_ID;
                    }
                    // Имя ролика
                    name = tmpItem.getJSONObject("compactRadioRenderer")
                            .getJSONObject("title").getString("simpleText");

                    // Получаем полынй урл ресурса, ролика или плейлиста
                    fullUrl = baseUrl + tmpItem.getJSONObject("compactRadioRenderer").getJSONObject("navigationEndpoint")
                            .getJSONObject("webNavigationEndpointData").getString("url");

                    // Количество просмотров. Если тип пункта PLAYLIST (typeOfId = 2) то тут
                    // хранится количество роликов
                    count_of_view = tmpItem.getJSONObject("compactRadioRenderer")
                            .getJSONObject("videoCountShortText").getJSONArray("runs")
                            .getJSONObject(0).getString("text");

                    // Автора нет, это плейлист
                    author = tmpItem.getJSONObject("shortBylineText").getString("simpleText");

                    // Время со дня публикации
                    publicationDate = "";

                    // Длительность ролика
                    duration = "";

                    // Сыылка на ролки, или плейлист
                    JSONArray arrImages = tmpItem.getJSONObject("compactRadioRenderer")
                            .getJSONObject("thumbnail").getJSONArray("thumbnails");

                    if(arrImages.length() > 1){
                        link_to_image = arrImages.getJSONObject(1)
                                .getString("url");
                    } else {
                        link_to_image = arrImages.getJSONObject(0)
                                .getString("url");
                    }

                }else if(tmpItem.has("compactAutoplayRenderer")){
                    // compactAutoplayRenderer
                    // Получен ИД
                    if(tmpItem.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                            .getJSONObject(0).getJSONObject("compactVideoRenderer")
                            .has("videoId")){
                        strId = tmpItem.getJSONObject("compactAutoplayRenderer")
                                .getJSONArray("contents").getJSONObject(0)
                                .getJSONObject("compactVideoRenderer").getString("videoId");
                        typeOfId = VIDEO_ID;
                    }else{
                        strId = tmpItem.getJSONObject("compactAutoplayRenderer")
                                .getJSONArray("contents").getJSONObject(0)
                                .getJSONObject("compactVideoRenderer").getString("playlistId");
                        typeOfId = PLAYLIST_ID;
                    } // videoId или playlistId

                    // Получаем наименование ролика
                    name = tmpItem.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                            .getJSONObject(0).getJSONObject("compactVideoRenderer")
                            .getJSONObject("title").getString("simpleText");

                    // Получаем полный урл ролика или плейлиста
                    fullUrl = baseUrl+ tmpItem.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                            .getJSONObject(0).getJSONObject("compactVideoRenderer")
                            .getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                            .getString("url");


                    // Получаем автора
                    author = tmpItem.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                            .getJSONObject(0).getJSONObject("compactVideoRenderer")
                            .getJSONObject("longBylineText").getJSONArray("runs")
                            .getJSONObject(0).getString("text");

                    // Количество прсмотров
                    count_of_view = tmpItem.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                            .getJSONObject(0).getJSONObject("compactVideoRenderer")
                            .getJSONObject("viewCountText").getString("simpleText");

                    // Длительность в формате ХХ:ХХ
                    duration = tmpItem.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                            .getJSONObject(0).getJSONObject("compactVideoRenderer")
                            .getJSONObject("lengthText").getString("simpleText");

                    // Как давно опубликован ? - даты публикации нет !!!!
                    publicationDate = "";

                    // картинка
                    JSONArray tmpImage = tmpItem.getJSONObject("compactAutoplayRenderer").getJSONArray("contents")
                            .getJSONObject(0).getJSONObject("compactVideoRenderer")
                            .getJSONObject("thumbnail").getJSONArray("thumbnails");

                    if(tmpImage.length() > 1){
                        link_to_image = tmpImage.getJSONObject(1).getString("url");
                    } else {
                        link_to_image = tmpImage.getJSONObject(0).getString("url");
                    }
                } else if(tmpItem.has("compactPlaylistRenderer")){
                    if(tmpItem.getJSONObject("compactPlaylistRenderer").has("videoId")){
                        strId = tmpItem.getJSONObject("compactPlaylistRenderer").getString("videoId");
                        typeOfId = VIDEO_ID;
                    } else{
                        strId = tmpItem.getJSONObject("compactPlaylistRenderer").getString("playlistId");
                        typeOfId = PLAYLIST_ID;
                    }

                    name = tmpItem.getJSONObject("compactPlaylistRenderer").getJSONObject("title")
                            .getString("simpleText");

                    fullUrl = baseUrl + tmpItem.getJSONObject("compactPlaylistRenderer")
                            .getJSONObject("navigationEndpoint").getJSONObject("webNavigationEndpointData")
                            .getString("url");

                    duration = tmpItem.getJSONObject("compactPlaylistRenderer").getJSONObject("videoCountShortText")
                            .getString("simpleText");

                    author = tmpItem.getJSONObject("compactPlaylistRenderer").getJSONArray("runs")
                            .getJSONObject(0).getString("text");

                    count_of_view = "";

                    publicationDate = "";

                    JSONArray tmpImage = tmpItem.getJSONObject("thumbnail").getJSONArray("thumbnails");
                    if(tmpImage.length() > 1){
                        link_to_image = tmpImage.getJSONObject(1).getString("url");
                    }else{
                        link_to_image = tmpImage.getJSONObject(0).getString("url");
                    }
                }

                b = getBitmap(link_to_image);
                publishProgress(new YoutubeItem(strId, fullUrl, name, count_of_view, duration,
                        publicationDate, author, typeOfId, b));
            }
        }

        private void parseJson02(JSONObject root) throws JSONException{
            String strId = "", fullUrl = "", name = "", count_of_view = "", duration = "",
                    publicationDate = "", author = "", link_to_image = "";
            int typeOfId = 0;
            Bitmap b;




        }

        private void getContent(String path) throws IOException{
            BufferedReader reader = null;
            try{
                URL url = new URL(path);
                HttpURLConnection c = (HttpURLConnection)url.openConnection();
                c.setRequestMethod("GET");
                c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9"
                        +",image/webp,image/apng,*/*;q=0.8");
                c.setRequestProperty("Accept-Lareanguage", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36 OPR/47.0.2631.80");
                c.setReadTimeout(10000);
                c.connect();

                reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder buf = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    buf.append(line);
                }

                int iStart = buf.indexOf(_start) + _start.length();
                while(buf.charAt(iStart) != '{') iStart++;

                int nTmp = 1;
                int nCount = iStart + 1;
                do{
                    if(buf.charAt(nCount) == '{'){
                        nTmp++;
                    }
                    if(buf.charAt(nCount) == '}'){
                        nTmp--;
                    }
                    nCount++;

                } while (nTmp != 0);

                line = buf.substring(iStart, nCount);

                Log.d(LOG, line);
                try{
                    JSONObject root = new JSONObject(line);
                    if(root.getJSONObject("contents").has("twoColumnBrowseResultsRenderer")){
                        // Здесь основная страница
                        parseMainJson(root);
                    }
                    if(root.getJSONObject("contents").has("twoColumnWatchNextResults")){
                        parseJson01(root);
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }finally {
                if(reader != null){
                    reader.close();
                }
            }
        }

        Bitmap getBitmap(String url){
            Bitmap b;
            try{
                InputStream in = new URL(url).openStream();
                b = BitmapFactory.decodeStream(in);
            }catch( Exception e){
                b = null;
            }
            return b;
        }

        @Override
        protected void onProgressUpdate(YoutubeItem... values) {
            super.onProgressUpdate(values);

            list.add(new YoutubeItem(values[0].videoId, values[0].fullUrl, values[0].title, values[0].count_of_view,
                    values[0].duration, values[0].publicationDate, values[0].author,
                    values[0].typeOfId, values[0].image));
            int i = list.size();
            adapter.notifyItemInserted(i);
        }
    }
}
