package com.prepost.imagetotext.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.borjabravo.readmoretextview.ReadMoreTextView;
import com.prepost.imagetotext.Models.ExtractedModel;
import com.prepost.imagetotext.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.MyViewHolder> {
    private int lastPosition = -1;

    private List<ExtractedModel> mModels = new ArrayList<>();
    private Context mContext;

    private HistoryAdapter.ReportListener listener;

    public HistoryAdapter(Context context) {
        this.mContext = context;
    }

    public void addModel(List<ExtractedModel> mModels) {
        this.mModels = mModels;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cust_history, viewGroup, false);

        return new HistoryAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final HistoryAdapter.MyViewHolder myViewHolder, int i) {
        ExtractedModel mSingleModel = mModels.get(i);
        if (mSingleModel != null) {
            myViewHolder.mReportId_tv.setText("Report id: " + mSingleModel.getId());
            myViewHolder.mContent_tv.setText(mSingleModel.getContent());
        }
    }

    /*Count total items*/
    @Override
    public int getItemCount() {
        return mModels.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView mReportId_tv;
        ReadMoreTextView mContent_tv;
        ImageView mSelect_iv;

        MyViewHolder(@NonNull final View itemView) {
            super(itemView);

            mSelect_iv = itemView.findViewById(R.id.cust_history_select_iv);
            mReportId_tv = itemView.findViewById(R.id.cust_history_id_tv);
            mContent_tv = itemView.findViewById(R.id.cust_history_content_tv);

            mSelect_iv.setOnClickListener(view -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.getReport(mModels.get(pos));
                }
            });
        }
    }

    //listener for sending value from adapter to activity
    public interface ReportListener {
        void getReport(ExtractedModel model);
    }

    public void setOnItemClickListener(ReportListener listener) {
        this.listener = listener;
    }


    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            //TranslateAnimation anim = new TranslateAnimation(0,-1000,0,-1000);
            ScaleAnimation anim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            //anim.setDuration(new Random().nextInt(501));//to make duration random number between [0,501)
            anim.setDuration(550);//to make duration random number between [0,501)
            viewToAnimate.startAnimation(anim);
            lastPosition = position;

        }
    }
}
