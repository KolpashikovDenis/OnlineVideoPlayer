package ru.kolpashikov.onlinevideoplayer.CommonClasses;

import android.graphics.Bitmap;

/**
 * Created by Denis on 16.12.2017.
 */

public class NavDrawerMenuItem {
    String channelId;
    String description;
    String tag;
    Bitmap bmpIcon;

    public NavDrawerMenuItem(String channelId, String description, String tag, Bitmap bmpIcon) {
        this.channelId = channelId;
        this.description = description;
        this.tag = tag;
        this.bmpIcon = bmpIcon;
    }
}
