package ru.kolpashikov.onlinevideoplayer.CommonClasses;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ru.kolpashikov.onlinevideoplayer.MainActivity;
import ru.kolpashikov.onlinevideoplayer.R;
import ru.kolpashikov.onlinevideoplayer.fragments.FragmentVideo;

/**
 * Created by Denis on 06.11.2017.
 */

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.VideoViewHolder> {
    static String LOG = "mLogs";
    static private List<YoutubeItem> videoList;
    static Context context;
    static  OnVideoSelectedListener mCallback;

    public RVAdapter(Context _context, List<YoutubeItem> _videoList) {
        videoList = _videoList;
        context = _context;
        try{
            mCallback = (OnVideoSelectedListener)context;
        }catch (ClassCastException e){
            mCallback = null;
            throw new ClassCastException("RVAdapter: " + context.toString()
                    + " must implements OnVideoSelectedListener.");
        }
    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_item, parent, false);

        VideoViewHolder vvh = new VideoViewHolder(view);

        return vvh;
    }

    @Override
    public void onBindViewHolder(VideoViewHolder holder, int position) {
        YoutubeItem videoListItem = videoList.get(position);

        holder.tvDescription.setText(videoListItem.title);
        if(!videoListItem.count_of_view.isEmpty()) {
            holder.tvCountView.setVisibility(View.VISIBLE);
            holder.tvCountView.setText(videoListItem.count_of_view);
        } else {
            holder.tvCountView.setVisibility(View.GONE);
        }
        if(!videoListItem.duration.isEmpty()) {
            holder.tvDuration.setText(videoListItem.duration);
            holder.tvDuration.setVisibility(View.VISIBLE);
        }else{
            holder.tvDuration.setVisibility(View.GONE);
        }
        if(!videoListItem.publicationDate.isEmpty()) {
            holder.tvPublicationDate.setVisibility(View.VISIBLE);
            holder.tvPublicationDate.setText(videoListItem.publicationDate);
        }else{
            holder.tvPublicationDate.setVisibility(View.GONE);
        }
        holder.tvAuthor.setText(videoListItem.author);
        holder.iv.setImageBitmap(videoListItem.image);

        // TODO: реализавать отображение что под карточкой скрывается список
        if(videoListItem.typeOfId == 1){

        }else if(videoListItem.typeOfId == 2){

        } else{

        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    @Override
    public void onViewAttachedToWindow(VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    //----------------------------------------------------------------------
    public static class VideoViewHolder extends RecyclerView.ViewHolder{

        CardView cv;
        FrameLayout frameLayout;
        ImageView iv;
        TextView tvDescription;
        TextView tvCountView;
        TextView tvDuration;
        TextView tvPublicationDate;
        TextView tvAuthor;

        public VideoViewHolder(View itemView){
            super(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
                    if( pos != RecyclerView.NO_POSITION ){
                        if(mCallback != null){
                            mCallback.onVideoSelected(pos);
                        }
                    }
                }
            });

            cv = (CardView)itemView.findViewById(R.id.cv_item);
            frameLayout = itemView.findViewById(R.id.frameLayout);
            iv = (ImageView)itemView.findViewById(R.id.thumbnailImage);
            tvDescription = (TextView)itemView.findViewById(R.id.tvDescription);
            tvCountView = itemView.findViewById(R.id.tvCountView);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvPublicationDate = itemView.findViewById(R.id.tvPublicationDate);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
        }
    }
}
