package ru.kolpashikov.onlinevideoplayer.CommonClasses;

import java.util.List;

/**
 * Created by Denis on 19.11.2017.
 */

public interface OnVideoSelectedListener {
    public void onVideoSelected(int position);
    public List<YoutubeItemEx> getVideoList();
    public RVAdapter getVideoAdapter();
    public void startAsyncTask(String url);
}
