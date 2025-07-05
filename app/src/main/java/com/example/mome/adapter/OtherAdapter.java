package com.example.mome.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mome.R;

import java.util.ArrayList;

public class OtherAdapter extends RecyclerView.Adapter<OtherAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<String> list;

    public OtherAdapter(Context context, ArrayList<String> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.other_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String s = list.get(position);
        holder.tvName.setText(s.substring(0,s.lastIndexOf(".")));
        holder.tvName.setOnClickListener(v->click.click(position));
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
        }
    }
}
