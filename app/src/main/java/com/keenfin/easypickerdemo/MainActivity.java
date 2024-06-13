/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypickerdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.keenfin.easypicker.PhotoPicker;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private PhotoPicker mPhotoPickerStrip;
    private PhotoPicker mPhotoPickerGrid;
    private PhotoPicker mPhotoPickerGridNoControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPhotoPickerStrip = findViewById(R.id.pp_easypicker_strip);
        mPhotoPickerGrid = findViewById(R.id.pp_easypicker_grid);
        mPhotoPickerGridNoControls = findViewById(R.id.pp_easypicker_grid_no_controls);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, com.keenfin.easypicker.R.string.manage_all_files, Toast.LENGTH_LONG).show();
                return;
            }
        } else if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }
        if (savedInstanceState == null) {
            getSupportLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    getSupportLoaderManager().initLoader(0, null, this);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPhotoPickerStrip.onActivityResult(requestCode, resultCode, data);
        mPhotoPickerGrid.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri sourceUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        return new CursorLoader(this, sourceUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, MediaStore.Audio.Media.TITLE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            ArrayList<String> images = new ArrayList<>();

            int i = 0;
            do {
                images.add(data.getString(0));
                i++;
            } while (data.moveToNext() && i < 6);

            mPhotoPickerGridNoControls.restoreImages(images);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
