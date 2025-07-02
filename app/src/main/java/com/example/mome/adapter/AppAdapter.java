package com.example.mome.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.example.mome.R;
import com.example.mome.utils.Util;


public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<String> list;

    public AppAdapter(Context context, ArrayList<String> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.app_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvName.setText(Util.getName(list.get(position)));
        holder.ivIcon.setBackground(Util.getIcon(list.get(position)));

        holder.ivIcon.setOnClickListener(v->click.click(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface Click{
        void  click(int position);
    }
    private Click click;

    public void setClick(Click click) {
        this.click = click;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }
    }
}
