/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

// http://developer.android.com/intl/ru/training/displaying-bitmaps/display-bitmap.html
public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    private final WeakReference<ImageView> mIvReference;
    private final int mSize;

    public BitmapWorkerTask(ImageView imageView, int size) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        mIvReference = new WeakReference<>(imageView);
        mSize = size;
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params) {
        return BitmapUtil.getBitmap(params[0], mSize);
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