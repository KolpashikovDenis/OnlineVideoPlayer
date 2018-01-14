package ru.kolpashikov.onlinevideoplayer.CommonClasses;

import android.graphics.Bitmap;

/**
 * Created by Denis on 31.10.2017.
 */

public class YoutubeItem{
    public String videoId;
    public String title;
    public String count_of_view;
    public String duration;
    public String publicationDate;
    public String author;
    public Bitmap image;
    public int typeOfId;
    public String fullUrl;

    public YoutubeItem(String _videoId, String _fullUrl, String _title, String _count_of_view,
                       String _duration, String _publicationDate, String _author, int _typeOfId, Bitmap _image) {
        videoId = _videoId;
        title = _title;
        count_of_view = _count_of_view;
        duration = _duration;
        publicationDate = _publicationDate;
        author = _author;
        typeOfId = _typeOfId;
        image = _image;
        fullUrl = _fullUrl;
    }
}
