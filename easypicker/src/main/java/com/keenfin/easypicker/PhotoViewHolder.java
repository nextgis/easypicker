/*
 *           Copyright © 2015-2016, 2019, 2021 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import static com.keenfin.easypicker.DownloadPhotoIntentService.DOWNLOAD_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

public class PhotoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public ImageButton mPhotoRemove;
    public ProgressBar progressBar;
    public ImageView mPhoto;
    public IViewHolderClick mViewHolderClick;
    public ImageButton mPhotoDownload;
    public int pImageHeight = 0;
    public int pImageWidth = 0;


    public interface IViewHolderClick {
        void onItemClick(View caller, int position);
    }

    public PhotoViewHolder(View itemView) {
        super(itemView);
        progressBar = itemView.findViewById(R.id.progressImageDownload);
        mPhotoRemove = itemView.findViewById(R.id.ib_remove);
        mPhotoDownload = itemView.findViewById(R.id.ib_download);
        mPhotoRemove.setOnClickListener(this);
        mPhoto = itemView.findViewById(R.id.iv_photo);
        mPhoto.setOnClickListener(this);
        mPhotoDownload.setOnClickListener(this);
    }

    public void setOnline(boolean online, boolean isNoControl){
        mPhotoRemove.setVisibility(online || isNoControl ? View.GONE : View.VISIBLE);
        mPhotoDownload.setVisibility(online? View.VISIBLE : View.GONE);
    }

    public void adjustControl(int side, int color, boolean isControl, boolean isOneLine, boolean noControls) {
        View parentBox = mPhoto.getRootView();
        if (! (parentBox.getLayoutParams() instanceof RecyclerView.LayoutParams)) {
            //Toast.makeText(this.itemView.getContext(), "instanceof RecyclerView.LayoutParams Error", Toast.LENGTH_LONG).show();
            return;
        }
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) parentBox.getLayoutParams();

        if (isControl) {
            if (isOneLine)
                lp.height = lp.width = side / 2;

//            lp.height = lp.width = side - lp.leftMargin - lp.rightMargin;
            lp.setMargins(side / 4, side / 4, side / 4, side / 4);
            mPhotoRemove.setVisibility(View.GONE);
            mPhoto.setColorFilter(color);
            mPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            lp.height = lp.width = side - lp.leftMargin - lp.rightMargin;
            mPhotoRemove.setColorFilter(color);
        }

        pImageHeight = lp.height;
        pImageWidth = lp.width;

        if (noControls)
            mPhotoRemove.setVisibility(View.GONE);

        parentBox.setLayoutParams(lp);
    }

    public void setIcon(Drawable photo) {
        mPhoto.setImageDrawable(photo);
    }

    public void loadPhoto(Context context, String path, int size, final File previewFile) {
        size = size > 0 ? size : Constants.REQUIRED_THUMBNAIL_SIZE;
        if (previewFile != null && previewFile.exists()){
            BitmapWorkerTask task = new BitmapWorkerTask(mPhoto, size, previewFile.getAbsolutePath(), BitmapUtil.getFileDescriptor(context, previewFile.getAbsolutePath()));
            task.execute();
        } else {
            BitmapWorkerTask task = new BitmapWorkerTask(mPhoto, size, path, BitmapUtil.getFileDescriptor(context, path));
            task.execute();
        }

    }

//    public void loadPhotoOnline(Context context, int size, AttachInfo attachInfo) {
//        size = size > 0 ? size : Constants.REQUIRED_THUMBNAIL_SIZE;
//        BitmapWorkerTask task = new BitmapWorkerTask(mPhoto, size, attachInfo );
//        task.execute();
//    }

    public void setOnClickListener(IViewHolderClick listener) {
        mViewHolderClick = listener;
    }

    @Override
    public void onClick(View view) {
        mViewHolderClick.onItemClick(view, getAdapterPosition());
    }


}
