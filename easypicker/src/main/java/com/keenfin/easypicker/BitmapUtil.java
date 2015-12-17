/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtil {
    public static Bitmap getBitmap(String path, int requiredSize) throws OutOfMemoryError {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int scale = 1;

        while (options.outWidth / scale > requiredSize && options.outHeight / scale > requiredSize)
            scale *= 2;

        options.inSampleSize = scale;
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }

    // http://stackoverflow.com/a/19739471/2088273
    public static String getMimeTypeOfFile(String pathName) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, opt);
        return opt.outMimeType;
    }
}
