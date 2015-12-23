/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BitmapUtil {
    // thanks to http://stackoverflow.com/questions/477572/strange-out-of-memory-issue-while-loading-an-image-to-a-bitmap-object
    public static Bitmap getBitmap(String path, int requiredSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int scale = 1;

        while (options.outWidth / scale > requiredSize && options.outHeight / scale > requiredSize)
            scale *= 2;

        options.inSampleSize = scale;
        options.inJustDecodeBounds = false;
        options.inDither = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[32 * 1024];

        Bitmap result = null;
        File file = new File(path);
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(file);

            try {
                result = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, options);
            } catch (OutOfMemoryError oom) {
                oom.printStackTrace();

                try {
                    options.inSampleSize *= 4;
                    result = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, options);
                } catch (OutOfMemoryError oom1) {
                    oom.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fs != null)
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return result;
    }

    // http://stackoverflow.com/a/19739471/2088273
    public static String getMimeTypeOfFile(String pathName) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, opt);
        return opt.outMimeType;
    }
}
