/*
 *           Copyright © 2015, 2021 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.ref.WeakReference;

// http://developer.android.com/intl/ru/training/displaying-bitmaps/display-bitmap.html
public class BitmapWorkerTask extends AsyncTask<Void, Void, Bitmap> {
    private final WeakReference<ImageView> mIvReference;
    private final int mSize;
    private final String mPath;
    private final FileDescriptor mFD;
    private final AttachInfo attachInfo;

    public BitmapWorkerTask(ImageView imageView, int size, String path, FileDescriptor fd) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        mIvReference = new WeakReference<>(imageView);
        mSize = size;
        mPath = path;
        mFD = fd;
        attachInfo = null;
    }


    // online
    public BitmapWorkerTask(ImageView imageView, int size, AttachInfo attachInfo) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        mIvReference = new WeakReference<>(imageView);
        mSize = size;

        mPath = "";
        mFD = null;
        this.attachInfo = attachInfo;

    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Void... params)
    {
        if (attachInfo == null) {
            // ofline case
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mFD != null) {
                return BitmapUtil.getBitmap(mFD);
            } else {
                return BitmapUtil.getBitmap(mPath, mSize);
            }
        } else {
            //online case  - load file or
            return BitmapUtil.getBitmap(mPath, mSize);
        }
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (mIvReference.get() != null) {
            if (bitmap == null)
                Toast.makeText(mIvReference.get().getContext(), R.string.null_bitmap, Toast.LENGTH_SHORT).show();
            else
                mIvReference.get().setImageBitmap(bitmap);
        }
    }
}