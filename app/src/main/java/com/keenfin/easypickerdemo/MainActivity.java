/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypickerdemo;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import com.keenfin.easypicker.PhotoPicker;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private PhotoPicker mPhotoPickerStrip;
    private PhotoPicker mPhotoPickerGrid;
    private PhotoPicker mPhotoPickerGridNoControls;
    Uri sourceUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPhotoPickerStrip = (PhotoPicker) findViewById(R.id.pp_easypicker_strip);
        mPhotoPickerGrid = (PhotoPicker) findViewById(R.id.pp_easypicker_grid);
        mPhotoPickerGridNoControls = (PhotoPicker) findViewById(R.id.pp_easypicker_grid_no_controls);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPhotoPickerStrip.onActivityResult(requestCode, resultCode, data);
        mPhotoPickerGrid.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, sourceUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, MediaStore.Audio.Media.TITLE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            ArrayList<String> images = new ArrayList<>();

            do {
                images.add(data.getString(0));
            } while (data.moveToNext());

            mPhotoPickerGridNoControls.restoreImages(images);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
