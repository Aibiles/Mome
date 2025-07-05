package com.example.mome.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mome.R;

import java.io.IOException;
import java.util.ArrayList;

public class AllAdapter extends RecyclerView.Adapter<AllAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<String> list;

    public AllAdapter(Context context, ArrayList<String> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.all_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String s = list.get(position);
        holder.tvName.setText(s.substring(0,s.lastIndexOf(".")));
        holder.ivBg.setOnClickListener(v->click.click(position));

        SharedPreferences app = context.getSharedPreferences("video", 0);
        holder.tvHis.setText("上次看到："+app.getString(s, "00分00秒"));

        try {
            AssetFileDescriptor fd = context.getAssets().openFd(s);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            holder.ivBg.setBackground(new BitmapDrawable(context.getResources(), retriever.getFrameAtTime()));
            retriever.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface Click {
        void  click (int position);
    }
    private Click click;

    public void setClick(Click click) {
        this.click = click;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvHis;
        private final ImageView ivBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvHis = itemView.findViewById(R.id.tv_his);
            ivBg = itemView.findViewById(R.id.iv_bg);
        }
    }
}
