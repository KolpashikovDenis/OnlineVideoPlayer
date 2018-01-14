package ru.kolpashikov.onlinevideoplayer.CommonClasses;

import android.graphics.Bitmap;

/**
 * Created by Denis on 20.12.2017.
 */

public class ChannelItem {
    public int id;
    public String channelName;
    public String channelUrl;
    public Bitmap urlImage;

    public ChannelItem(int id, String channelUrl, String channelName, Bitmap urlImage) {
        this.id = id;
        this.urlImage = urlImage;
        this.channelUrl = channelUrl;
        this.channelName = channelName;
    }

}
