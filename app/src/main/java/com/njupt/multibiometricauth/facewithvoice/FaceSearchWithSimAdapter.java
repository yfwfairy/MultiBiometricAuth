package com.njupt.multibiometricauth.facewithvoice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.njupt.multibiometricauth.R;
import com.njupt.multibiometricauth.face.faceserver.CompareResult;
import com.njupt.multibiometricauth.face.faceserver.FaceServer;

import java.io.File;
import java.util.List;

class FaceSearchWithSimAdapter extends RecyclerView.Adapter<FaceSearchWithSimAdapter.CompareResultHolder> {
    private List<CompareResult> compareResultList;
    private LayoutInflater inflater;

    public FaceSearchWithSimAdapter(List<CompareResult> compareResultList, Context context) {
        inflater = LayoutInflater.from(context);
        this.compareResultList = compareResultList;
    }

    public List<CompareResult> getResultList() {
        return compareResultList;
    }

    @NonNull
    @Override
    public CompareResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.recycler_item_search_result_with_simi, null, false);
        CompareResultHolder compareResultHolder = new CompareResultHolder(itemView);
        compareResultHolder.textView = itemView.findViewById(R.id.tv_item_name);
        compareResultHolder.imageView = itemView.findViewById(R.id.iv_item_head_img);
        compareResultHolder.simiTxv = itemView.findViewById(R.id.tv_item_simi);
        compareResultHolder.voiceRegTxv = itemView.findViewById(R.id.tv_item_voice_reg);
        return compareResultHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CompareResultHolder holder, int position) {
        if (compareResultList == null) {
            return;
        }
        File imgFile = new File(FaceServer.ROOT_PATH + File.separator + FaceServer.SAVE_IMG_DIR + File.separator + compareResultList.get(position).getUserName() + FaceServer.IMG_SUFFIX);
        Glide.with(holder.imageView)
                .load(imgFile)
                .into(holder.imageView);
        holder.textView.setText(compareResultList.get(position).getUserName());
        holder.simiTxv.setText("???????????????:" + compareResultList.get(position).getSimilar());
        holder.voiceRegTxv.setText("???????????????" + (compareResultList.get(position).getVoiceSimilar()));
    }

    @Override
    public int getItemCount() {
        return compareResultList == null ? 0 : compareResultList.size();
    }

    class CompareResultHolder extends RecyclerView.ViewHolder {

        TextView textView;
        ImageView imageView;
        TextView simiTxv;
        TextView voiceRegTxv;

        CompareResultHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
