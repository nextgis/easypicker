/*
 *           Copyright © 2015 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PhotoPicker extends RecyclerView {
    private int mMaxPhotos = -1;
    private int mRowHeight;
    private int mImagesPerRow, mImagesPerRowPortrait = Constants.IMAGES_PER_ROW_P, mImagesPerRowLandscape = Constants.IMAGES_PER_ROW_L;
    private String mNewPhotosDir = Constants.NEW_PHOTOS_SAVE_DIR;
    private int mColorPrimary, mColorAccent;
    private int mCameraRequest, mPickRequest;
    private boolean mIsOneLine = false, mIsUsePreview = true;
    private boolean mPrimaryColorDefined, mAccentColorDefined;

    private Context mContext;
    private PhotoAdapter mPhotoAdapter;
    private Bitmap mNewPhotoIcon;

    public PhotoPicker(Context context) {
        this(context, false);
    }

    public PhotoPicker(Context context, boolean noControls) {
        super(context);
        mNewPhotoIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_add_white_48dp);
        init(context, noControls);
    }

    public PhotoPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PhotoPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, boolean noControls) {
        mContext = context;

        PhotoAdapter adapter = new PhotoAdapter(noControls);
        setAdapter(adapter);

        mImagesPerRow = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? mImagesPerRowLandscape : mImagesPerRowPortrait;
        setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager;

        if (mIsOneLine)
            layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        else
            layoutManager = new GridLayoutManager(context, mImagesPerRow);

        setLayoutManager(layoutManager);

        int[] attrs = new int[] { R.attr.colorPrimary, R.attr.colorAccent };
        TypedArray styleable = context.obtainStyledAttributes(attrs);
        if (!mPrimaryColorDefined)
            mColorPrimary = getColor(styleable, 0, R.color.primary);
        if (!mAccentColorDefined)
            mColorAccent = getColor(styleable, 1, R.color.accent);
        styleable.recycle();
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray styleable = context.obtainStyledAttributes(attrs, R.styleable.PhotoPicker, 0, 0);
        mImagesPerRowLandscape = styleable.getInt(R.styleable.PhotoPicker_photosPerRowLandscape, mImagesPerRowLandscape);
        mImagesPerRowPortrait = styleable.getInt(R.styleable.PhotoPicker_photosPerRowPortrait, mImagesPerRowPortrait);
        mIsOneLine = styleable.getBoolean(R.styleable.PhotoPicker_oneLineGallery, false);
        int icon = styleable.getResourceId(R.styleable.PhotoPicker_newPhotosIcon, R.drawable.ic_add_white_48dp);
        mNewPhotoIcon = BitmapFactory.decodeResource(getResources(), icon);
        boolean noControls = styleable.getBoolean(R.styleable.PhotoPicker_noControls, false);
        init(context, noControls);

        if (mPrimaryColorDefined = styleable.hasValue(R.styleable.PhotoPicker_primaryColor))
            mColorPrimary = getColor(styleable, R.styleable.PhotoPicker_primaryColor, R.color.primary);
        if (mAccentColorDefined = styleable.hasValue(R.styleable.PhotoPicker_accentColor))
            mColorAccent = getColor(styleable, R.styleable.PhotoPicker_accentColor, R.color.accent);

        mIsUsePreview = styleable.getBoolean(R.styleable.PhotoPicker_usePreview, true);
        mMaxPhotos = styleable.getInt(R.styleable.PhotoPicker_maxPhotos, mMaxPhotos);
        mNewPhotosDir = styleable.getString(R.styleable.PhotoPicker_newPhotosDirectory);
        mNewPhotosDir = mNewPhotosDir == null ? Constants.NEW_PHOTOS_SAVE_DIR : mNewPhotosDir;
        styleable.recycle();
    }

    @SuppressWarnings("deprecation")
    private int getColor(TypedArray array, int index, int defValue) {
        return array.getColor(index, mContext.getResources().getColor(defValue));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putStringArrayList(Constants.BUNDLE_ATTACHED_IMAGES, mPhotoAdapter.getImagesPath());
        bundle.putInt(Constants.BUNDLE_CAMERA_REQUEST, mCameraRequest);
        bundle.putInt(Constants.BUNDLE_PICK_REQUEST, mPickRequest);

        if (mPhotoAdapter.getPhotoUri() != null)
            bundle.putString(Constants.BUNDLE_NEW_PHOTO_PATH, mPhotoAdapter.getPhotoUri().getPath());

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            if (bundle.containsKey(Constants.BUNDLE_NEW_PHOTO_PATH))
                mPhotoAdapter.setPhotoUri(Uri.parse(bundle.getString(Constants.BUNDLE_NEW_PHOTO_PATH)));

            mPhotoAdapter.restoreImages(bundle.getStringArrayList(Constants.BUNDLE_ATTACHED_IMAGES));
            mCameraRequest = bundle.getInt(Constants.BUNDLE_CAMERA_REQUEST);
            mPickRequest = bundle.getInt(Constants.BUNDLE_PICK_REQUEST);

            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            return;
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        mRowHeight = getMeasuredWidth() / mImagesPerRow;
        mPhotoAdapter.measureParent();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPhotoAdapter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (!(adapter instanceof PhotoAdapter))
            throw new IllegalArgumentException("You should not pass adapter to PhotoPicker. It uses specific one.");

        mPhotoAdapter = (PhotoAdapter) adapter;
        super.setAdapter(adapter);
    }

    public void restoreImages(List<String> imagesPath) {
        mPhotoAdapter.restoreImages(imagesPath);
    }

    public void setMaxPhotos(int maxPhotos) {
        mMaxPhotos = maxPhotos;
    }

    public void setNewPhotosDirectory(String directoryName) {
        mNewPhotosDir = directoryName;
    }

    public void setUsePreview(boolean usePreview) {
        mIsUsePreview = usePreview;
    }

    public void setNewPhotosDrawable(int drawableResourceId) {
        mPhotoAdapter.replaceNewPhotoIcon(drawableResourceId);
    }

    public ArrayList<String> getImagesPath() {
        return mPhotoAdapter.getImagesPath();
    }

    public class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> implements PhotoViewHolder.IViewHolderClick {
        private List<String> mImagesPath;
        private Uri mPhotoUri;

        private boolean mNoControls = false;

        public PhotoAdapter() {
            this(false);
        }

        public PhotoAdapter(boolean noControls) {
            mImagesPath = new ArrayList<>();
            mNoControls = noControls;

            if (!noControls)
                addNewPhotoIcon();
        }

        private void addNewPhotoIcon() {
            mImagesPath.add(0, null);
            notifyItemInserted(0);
        }

        protected void replaceNewPhotoIcon(int drawableResourceId) {
            if (!mNoControls && mImagesPath.size() > 0) {
                mNewPhotoIcon = BitmapFactory.decodeResource(getResources(), drawableResourceId);
                notifyItemChanged(0);
            }
        }

        @Override
        public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
            view.setBackgroundColor(mColorAccent);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final PhotoViewHolder holder, final int position) {
            holder.setOnClickListener(this);

            boolean isControl = position == 0 && !mNoControls;
            if (isControl)
                holder.setIcon(mNewPhotoIcon);
            else
                holder.loadPhoto(mImagesPath.get(position), mRowHeight);

            holder.adjustControl(mRowHeight, mColorPrimary, isControl, mIsOneLine, mNoControls);
        }

        @Override
        public int getItemCount() {
            return mImagesPath.size();
        }

        public ArrayList<String> getImagesPath() {
            ArrayList<String> images = new ArrayList<>();
            images.addAll(mImagesPath);

            if (!mNoControls)
                images.remove(0);

            return images;
        }

        protected void restoreImages(List<String> imagesPath) {
            for (String imagePath : imagesPath)
                addImage(imagePath);
        }

        public Uri getPhotoUri() {
            return mPhotoUri;
        }

        public void setPhotoUri(Uri photoUri) {
            this.mPhotoUri = photoUri;
        }

        @Override
        public void onItemClick(View caller, int position) {
            int i = caller.getId();
            if (i == R.id.iv_photo) {
                if (position == 0 && !mNoControls) {
                    if (mMaxPhotos > -1 && getItemCount() - 1 >= mMaxPhotos) {
                        Toast.makeText(mContext, String.format(mContext.getString(R.string.max_photos), mMaxPhotos), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(R.string.photo_add);
                    builder.setItems(R.array.report_add_photos, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            Intent intent;
                            Random randomizeRequest = new Random(System.currentTimeMillis());
                            switch (item) {
                                case 0:
                                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    File photo = new File(Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_DCIM), mNewPhotosDir);

                                    if (!photo.mkdirs() && !photo.exists()) {
                                        Toast.makeText(mContext, R.string.hw_error, Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    photo = new File(photo, System.currentTimeMillis() + ".jpg");
                                    mPhotoUri = Uri.fromFile(photo);
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
                                    mCameraRequest = randomizeRequest.nextInt(0xffff);
                                    ((Activity) mContext).startActivityForResult(intent, mCameraRequest);
                                    break;
                                case 1:
                                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    intent.setType("image/*");
                                    mPickRequest = randomizeRequest.nextInt(0xffff);
                                    ((Activity) mContext).startActivityForResult(
                                            Intent.createChooser(intent, mContext.getString(R.string.photo_pick)), mPickRequest);
                                    break;
                            }
                        }
                    });
                    builder.show();
                } else {
                    if (!mIsUsePreview)
                        return;

                    int offset = mNoControls ? 0 : 1;
                    Intent preview = new Intent(getContext(), PreviewActivity.class);
                    preview.putExtra(Constants.BUNDLE_ATTACHED_IMAGES, getImagesPath());
                    preview.putExtra(Constants.BUNDLE_NEW_PHOTO_PATH, position - offset);
                    getContext().startActivity(preview);
                }
            } else if (i == R.id.ib_remove) {
                if (position <= 0)
                    return;

                mImagesPath.remove(position);
                notifyItemRemoved(position);
                measureParent();
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                String selectedImagePath;

                if (requestCode == mCameraRequest)
                    selectedImagePath = mPhotoUri.getPath();
                else if (requestCode == mPickRequest)
                    selectedImagePath = FileUtil.getPath(mContext, data.getData());
                else
                    return;

                addImage(selectedImagePath);
            }
        }

        private boolean addImage(String imagePath) {
            String mimeType = BitmapUtil.getMimeTypeOfFile(imagePath);

            if (mimeType == null) {
                Toast.makeText(mContext, mContext.getString(R.string.photo_fail_attach), Toast.LENGTH_SHORT).show();
                return false;
            }

            boolean result = mImagesPath.add(imagePath);
            notifyItemInserted(mImagesPath.size() - 1);
            measureParent();

            return result;
        }

        public void measureParent() {
            ViewGroup.LayoutParams params = getLayoutParams();
            int itemsCount = mIsOneLine ? 1 : mImagesPath.size();
            params.height = (int) Math.ceil(1f * itemsCount / mImagesPerRow) * mRowHeight;
            setLayoutParams(params);
        }
    }
}
