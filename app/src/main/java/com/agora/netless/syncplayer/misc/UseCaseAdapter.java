package com.agora.netless.syncplayer.misc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agora.netless.syncplayer.R;

import java.util.List;

public class UseCaseAdapter extends RecyclerView.Adapter<UseCaseAdapter.ViewHolder> {
    private Context context;
    private final List<UseCase> useCases;
    private OnItemClickListener onItemClickListener;

    public UseCaseAdapter(List<UseCase> useCases) {
        this.useCases = useCases;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        context = recyclerView.getContext();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        context = null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_usecase, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UseCase useCase = useCases.get(position);

        holder.titleView.setText(useCase.title);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(useCase);
                }
            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @Override
    public int getItemCount() {
        return useCases.size();
    }

    public interface OnItemClickListener {
        void onItemClick(UseCase useCase);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        View cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }
}
