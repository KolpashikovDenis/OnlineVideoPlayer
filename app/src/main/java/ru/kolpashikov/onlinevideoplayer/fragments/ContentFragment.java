package ru.kolpashikov.onlinevideoplayer.fragments;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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
import ru.kolpashikov.onlinevideoplayer.CommonClasses.Const;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.OnChangeNavDrawerMenu;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.OnVideoSelectedListener;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.RVAdapter;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.YoutubeItem;
import ru.kolpashikov.onlinevideoplayer.CommonClasses.YoutubeItemEx;
import ru.kolpashikov.onlinevideoplayer.MainActivity;
import ru.kolpashikov.onlinevideoplayer.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContentFragment extends Fragment {
    final String LOG = "m_logs";
    final int TYPE_VIDEO_ID = 1;
    final int TYPE_CHANNEL_ID = 2;

    private RecyclerView rvMain;
    private RVAdapter adapter;
    private List<YoutubeItemEx> videoList; // ВОт тут самый главный videoList
    OnVideoSelectedListener mCallback = null;

    public ContentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context _context) {
        super.onAttach(_context);
        try{
            mCallback = (OnVideoSelectedListener)_context;
        }catch (ClassCastException e){
            throw new ClassCastException("ContentFragment: "
                    + _context.toString()
                    + " must implements OnVideoSelectedListener");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false);
        rvMain = (RecyclerView) view.findViewById(R.id.rvMain);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rvMain.setLayoutManager(llm);
        rvMain.setHasFixedSize(true);

        if(videoList == null) {
            videoList = mCallback.getVideoList();
        } else{
            videoList.clear();
        }
        //adapter = new RVAdapter(getActivity(), videoList);
        adapter = mCallback.getVideoAdapter();
        rvMain.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        videoList.clear();
        adapter.notifyDataSetChanged();

//        new PredownloadTask(videoList).execute();
        mCallback.startAsyncTask(Const.BASE_URL);
    }

    private class PredownloadTask extends AsyncTask<Void, YoutubeItem, Void> {

        private String LOG = "mLogs";
        private String url = "https://www.youtube.com";
        final String _start = "ytInitialData";
        final String _end   = "adSafetyReason";
        private List<YoutubeItem> list;

//        Context ctx;

        public PredownloadTask(List<YoutubeItem> _list){
            list = _list;
        }

        @Override
        protected void onProgressUpdate(YoutubeItem... values) {
            super.onProgressUpdate(values);

            list.add(new YoutubeItem(values[0].videoId, values[0].fullUrl, values[0].title,
                    values[0].count_of_view, values[0].duration, values[0].publicationDate,
                    values[0].author, values[0].typeOfId, values[0].image));

            int i = videoList.size();
            adapter.notifyItemInserted(i);
        }

        @Override
        protected Void doInBackground(Void... params) {

            try{
                getContent(url);
            } catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        private void getContent(String path) throws IOException {
            BufferedReader reader = null;
            try {
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
                /*
                ЗДЕСЬ ИЩЕМ JSON-МАССИВ С ДАННЫМИ
                 */
                int iStart = buf.indexOf(_start);
                int i = iStart + _start.length();
                while (buf.charAt(i) != '{') {
                    i++;
                }

                iStart = i;
                int iEnd = buf.indexOf(_end) + _end.length();
                i = iEnd;
                while (buf.charAt(i) != ';') i++;
                iEnd = i + 1;
                line = buf.substring(iStart - 1, iEnd);
                /*  КОНЕЦ ПОИСКА */

                //TODO: initialize JSON object and parse it to get links on videos.
                try {
                    JSONObject root = new JSONObject(line);
                    JSONObject tmp = root.getJSONObject("contents");
                    tmp = tmp.getJSONObject("twoColumnBrowseResultsRenderer");
                    JSONArray tmpA = tmp.getJSONArray("tabs");
                    tmp = tmpA.getJSONObject(0);
                    tmp = tmp.getJSONObject("tabRenderer");
                    tmp = tmp.getJSONObject("content");
                    tmp = tmp.getJSONObject("sectionListRenderer");
                    tmpA = tmp.getJSONArray("contents");

                    // First 'for...' получение списка рекомандованных каналов.
                    for (i = 0; i < tmpA.length(); i++) {
                        tmp = (JSONObject) tmpA.get(i);
                        tmp = tmp.getJSONObject("itemSectionRenderer");
                        JSONArray contentTmp = tmp.getJSONArray("contents");
                        tmp = (JSONObject)contentTmp.get(0);
                        tmp = tmp.getJSONObject("shelfRenderer");

                        JSONObject jsonTitle = tmp.getJSONObject("title");
                        String title = jsonTitle.getString("simpleText");

                        tmp = tmp.getJSONObject("content");
                        tmp = tmp.getJSONObject("horizontalListRenderer");
                        contentTmp = tmp.getJSONArray("items");
                        // Вложенный 'for' получение пунктов из списка рекомендованных каналов
                        for( int k = 0; k < contentTmp.length(); k++){
                            JSONObject tmp1 = contentTmp.getJSONObject(k);
                            tmp1 = tmp1.getJSONObject("gridVideoRenderer");

                            // TODO: get videoId -> video url
                            String strId = null;
                            int typeOfId = 0;
                            if(tmp1.has("videoId")){
                                strId = tmp1.getString("videoId");
                                typeOfId = 1;
                            }else if(tmp1.has("playlistId")){
                                strId = tmp1.getString("playlistId");
                                typeOfId = 2;
                            }else {
                                strId = tmp1.getString("videoId");
                                typeOfId = 3;
                            }

                            // Получаем полный урл
                            String fullUrl = url + tmp1.getJSONObject("navigationEndpoint")
                                    .getJSONObject("webNavigationEndpointData").getString("url");

                            // TODO: get thumbnail ->
                            JSONObject thumbnails = tmp1.getJSONObject("thumbnail");
                            JSONArray thumbArr = thumbnails.getJSONArray("thumbnails");
                            int iii = thumbArr.length();
                            if(iii > 1){
                                thumbnails = thumbArr.getJSONObject(1);
                            }else {
                                thumbnails = thumbArr.getJSONObject(0);
                            }
                            String urlImage = thumbnails.getString("url");
//                            Log.d(LOG, "url - " + urlImage);
//                            int width = thumbnails.getInt("width");
//                            int height = thumbnails.getInt("height");

                            // TODO: get title
                            thumbnails = tmp1.getJSONObject("title");
                            String simpleText = thumbnails.getString("simpleText");

                            // TODO: get count of view
//                            JSONObject jsonCountView = tmp1.getJSONObject("viewCountText");
                            String stCountView = tmp1.getJSONObject("viewCountText").getString("simpleText");

                            // TODO: get duration
                            String stDuration = tmp1.getJSONArray("thumbnailOverlays").getJSONObject(0)
                                    .getJSONObject("thumbnailOverlayTimeStatusRenderer")
                                    .getJSONObject("text").getString("simpleText");

                            // TODO: get publication date
                            String stPublicationDate = tmp1.getJSONObject("publishedTimeText")
                                    .getString("simpleText");

                            // TODO: get author
                            String stAuthor = tmp1.getJSONObject("shortBylineText")
                                    .getJSONArray("runs").getJSONObject(0)
                                    .getString("text");

                            publishProgress(new YoutubeItem(strId, fullUrl, simpleText, stCountView, stDuration,
                                    stPublicationDate, stAuthor, typeOfId, getBitmap(urlImage)));
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                return line;
            }finally{
                if(reader != null)
                    reader.close();
            }
        }

        Bitmap getBitmap(String urlImage){
            Bitmap b;
            try{
                InputStream in = new URL(urlImage).openStream();
                b = BitmapFactory.decodeStream(in);
            }catch( Exception e){
                b = null;
            }
            return b;
        }
    }

}
