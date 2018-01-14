package ru.kolpashikov.onlinevideoplayer.fragments;

import android.os.Bundle;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import ru.kolpashikov.onlinevideoplayer.R;

/**
 * Created by Denis on 18.11.2017.
 */

public class FragmentVideo extends YouTubePlayerFragment implements YouTubePlayer.OnInitializedListener{
    private YouTubePlayer ytPlayer;
    private String videoId;

    @Override
    public void onDestroy() {
        if(ytPlayer != null)
            ytPlayer.release();

        super.onDestroy();
    }

    public void setVideoId(String _videoId){
        if(_videoId != null && !_videoId.equals(videoId)){
            videoId = _videoId;
            if(ytPlayer != null){
                ytPlayer.cueVideo(videoId);
            }
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        initialize(getResources().getString(R.string.youtube_apikey), this);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        ytPlayer = youTubePlayer;
        ytPlayer.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE);
        ytPlayer.setOnFullscreenListener((YouTubePlayer.OnFullscreenListener)getActivity());
        if(b && videoId != null){
            ytPlayer.cueVideo(videoId);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        ytPlayer = null;
    }

    public void backnormal(){
        ytPlayer.setFullscreen(false);
    }
}
