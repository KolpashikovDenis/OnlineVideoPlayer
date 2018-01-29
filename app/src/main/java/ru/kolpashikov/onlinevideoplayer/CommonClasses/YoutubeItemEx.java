package ru.kolpashikov.onlinevideoplayer.CommonClasses;

/**
 * Created by Denis on 28.01.2018.
 */

public class YoutubeItemEx {
    public int typeOfId;        // type of resource: video, playlist, channel
    public String ytId;         // videoId, channelId, playlistId
    public String ytThumbnail;  // url to preview
    public String ytTitle;      // title of video, playlist, channel
    public String urlResoucre;  // url to resource: video, playlist, channel (navigationEndpoint)
    public String author;       // author, name of channel: shortBylineText, longBylineText

    // Здесь будут поля которые могут отличаться по смыслу
    public String viewCountText; // duration of video or count of video (if playlist)
}
