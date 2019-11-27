/*
 *           Copyright © 2015-2016, 2019 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

public class PhotoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private ImageButton mPhotoRemove;
    private ImageView mPhoto;
    private IViewHolderClick mViewHolderClick;

    public interface IViewHolderClick {
        void onItemClick(View caller, int position);
    }

    public PhotoViewHolder(View itemView) {
        super(itemView);
        mPhotoRemove = itemView.findViewById(R.id.ib_remove);
        mPhotoRemove.setOnClickListener(this);
        mPhoto = itemView.findViewById(R.id.iv_photo);
        mPhoto.setOnClickListener(this);
    }

    public void adjustControl(int side, int color, boolean isControl, boolean isOneLine, boolean noControls) {
        View parentBox = mPhoto.getRootView();
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) parentBox.getLayoutParams();

        if (isControl) {
            if (isOneLine)
                lp.height = lp.width = side / 2;

            lp.setMargins(side / 4, side / 4, side / 4, side / 4);
            mPhotoRemove.setVisibility(View.GONE);
            mPhoto.setColorFilter(color);
            mPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            lp.height = lp.width = side - lp.leftMargin - lp.rightMargin;
            mPhotoRemove.setColorFilter(color);
        }

        if (noControls)
            mPhotoRemove.setVisibility(View.GONE);

        parentBox.setLayoutParams(lp);
    }

    public void setIcon(Drawable photo) {
        mPhoto.setImageDrawable(photo);
    }

    public void loadPhoto(String path, int size) {
        BitmapWorkerTask task = new BitmapWorkerTask(mPhoto, size > 0 ? size : Constants.REQUIRED_THUMBNAIL_SIZE);
        task.execute(path);
    }

    public void setOnClickListener(IViewHolderClick listener) {
        mViewHolderClick = listener;
    }

    @Override
    public void onClick(View view) {
        mViewHolderClick.onItemClick(view, getAdapterPosition());
    }
}
