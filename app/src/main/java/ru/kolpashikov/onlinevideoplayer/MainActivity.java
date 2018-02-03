package ru.kolpashikov.onlinevideoplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.youtube.player.YouTubePlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ru.kolpashikov.onlinevideoplayer.CommonClasses.ChannelItem;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.OnVideoSelectedListener;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.RVAdapter;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.YoutubeItem;
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
    private List<ChannelItem> menuItemList;
    private List<YoutubeItem> videoList;
    private RVAdapter adapter;
    private int count = 0;

    private ContentFragment contentFragment;
    private NoConnectionFragment noConnectionFragment;
    private FragmentVideo fragmentVideo;
    private View fragmentVideoView;
    private SelectVideoIdTask threadSelectVideoTask;

    private FragmentTransaction ft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

        videoList = contentFragment.getVideoList();
        adapter = contentFragment.getAdapter();
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }else if(isFullScreen){
            fragmentVideo.backnormal();
            isFullScreen = false;
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
        if(id == R.id.action_settings){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String fullUrl = baseUrl + ((ChannelItem)menuItemList.get(id)).channelUrl;
//        String videoId = "";

        videoList = contentFragment.getVideoList();
        adapter = contentFragment.getAdapter();
        videoList.clear();
        adapter.notifyDataSetChanged();
        fragmentVideo.setVideoId("");
        fragmentVideoView.setVisibility(View.GONE);
        threadSelectVideoTask = new SelectVideoIdTask(videoList);
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

        String videoId = videoList.get(position).videoId;
        String fullUrl = videoList.get(position).fullUrl;

        videoList.clear();
        adapter.notifyDataSetChanged();

        fragmentVideoView.setVisibility(View.VISIBLE);
        fragmentVideo.setVideoId(videoId);
        threadSelectVideoTask = new SelectVideoIdTask(videoList);
        threadSelectVideoTask.execute(fullUrl);
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

    private class ParseContentTask extends AsyncTask<String, YoutubeItem, Void> {
        List<YoutubeItem> list;
        final String _start = "ytInitialData";

        public ParseContentTask(List<YoutubeItem> list) {
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
                JSONObject root;
                try{
                    root = new JSONObject(line);
                    // Главная или канал
                    if(root.getJSONObject("contents").has("twoColumnBrowseResultsRenderer")){
                        JSONArray tmpArray = root.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer")
                                .getJSONArray("tabs").getJSONObject(0).getJSONObject("tabRenderer")
                                .getJSONObject("sectionListRenderer").getJSONArray("contents");

                        JSONObject tmp = tmpArray.getJSONObject(0).getJSONObject("itemSectionRenderer")
                                .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                                .getJSONObject("content");
                        if(tmp.has("gridRenderer")){
                            // Здесь "Главная"
                            parseMainPage(tmpArray);
                        } else if(tmp.has("horizontalListRenderer")){
                            // Здесь выбор канала
                            parseChannelPage(tmpArray);
                        } else{
                            Log.d(_start, tmp.names().toString());
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
                            JSONObject playlist = root.getJSONObject("contents").getJSONObject("twoColumnWatchNextResults")
                                    .getJSONObject("playlist").getJSONObject("playlist");
                            parsePlaylist(playlist);
                        } else {
                            Log.d(_start, root.getJSONObject("contents").getJSONObject("twoColumnWatchNextResults").names().toString());
                        }

                        return;
                    }

                    // Поиск
                    if(root.getJSONObject("contents").has("twoColumnSearchResultsRenderer")){

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
            try {
                for (int i = 0; i < localRoot.length(); i++) {
                    JSONArray items = localRoot.getJSONObject(i).getJSONObject("itemSectionRenderer")
                            .getJSONArray("contents").getJSONObject(0).getJSONObject("shelfRenderer")
                            .getJSONObject("content").getJSONObject("gridRenderer").getJSONArray("items");
                    for(int k = 0; k < items.length(); k++){

                    }

                }
            }catch(JSONException e){
                e.printStackTrace();
            }

        }

        private void parsePlaylist(JSONObject localRoot){

        }

        private void parseItemPage(JSONArray localRoot){

        }

        private void parseSearchPage(JSONArray localRoot){

        }

        private void parseChannelPage(JSONArray localRoot){

        }

        @Override
        protected void onProgressUpdate(YoutubeItem... values) {
//            super.onProgressUpdate(values);

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
            adapter = contentFragment.getAdapter();
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
