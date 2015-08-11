/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.graphics.Bitmap;
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
        mPhotoRemove = (ImageButton) itemView.findViewById(R.id.ib_remove);
        mPhotoRemove.setOnClickListener(this);
        mPhoto = (ImageView) itemView.findViewById(R.id.iv_photo);
        mPhoto.setOnClickListener(this);
    }

    public void adjustControl(int side, int color, boolean isControl, boolean isOneLine) {
        View parentBox = mPhoto.getRootView();
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) parentBox.getLayoutParams();

        if (isControl) {
            if (isOneLine)
                lp.height = lp.width = side / 2;

            lp.setMargins(side / 4, side / 4, side / 4, side / 4);
            mPhotoRemove.setVisibility(View.GONE);
            mPhoto.setColorFilter(color);
        } else {
            lp.height = lp.width = side - lp.leftMargin - lp.rightMargin;
            mPhotoRemove.setColorFilter(color);
        }

        parentBox.setLayoutParams(lp);
    }

    public void setPhoto(Bitmap photo) {
        mPhoto.setImageBitmap(photo);
    }

    public void setOnClickListener(IViewHolderClick listener) {
        mViewHolderClick = listener;
    }

    @Override
    public void onClick(View view) {
        mViewHolderClick.onItemClick(view, getPosition());
    }
}
