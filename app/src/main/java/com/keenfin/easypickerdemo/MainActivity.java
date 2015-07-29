/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypickerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.keenfin.easypicker.PhotoPicker;

public class MainActivity extends AppCompatActivity {
    PhotoPicker mPhotoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPhotoPicker = (PhotoPicker) findViewById(R.id.pp_easypicker);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPhotoPicker.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
