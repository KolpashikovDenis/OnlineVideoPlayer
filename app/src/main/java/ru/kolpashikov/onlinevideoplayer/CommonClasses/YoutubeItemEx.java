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
    public String viewCountText; // count of views
    public String lengthText; // duration of video
    public String publishedTimeText; // Date of publication video
    public String linkToAuthorChannel; // link to author channel
    public String urlChannelThumbnail; // link to icon of author's channel

    public YoutubeItemEx(int typeOfId, String ytId, String ytThumbnail, String ytTitle, String urlResoucre,
                         String author, String viewCountText, String lengthText, String publishedTimeText,
                         String linkToAuthorChannel, String urlChannelThumbnail) {
        this.typeOfId = typeOfId;
        this.ytId = ytId;
        this.ytThumbnail = ytThumbnail;
        this.ytTitle = ytTitle;
        this.urlResoucre = urlResoucre;
        this.author = author;
        this.viewCountText = viewCountText;
        this.lengthText = lengthText;
        this.publishedTimeText = publishedTimeText;
        this.linkToAuthorChannel = linkToAuthorChannel;
        this.urlChannelThumbnail = urlChannelThumbnail;
    }
}
