/*
 *           Copyright © 2015-2017, 2019, 2021 Stanislav Petriakov
 *  Distributed under the Boost Software License, Version 1.0.
 *     (See accompanying file LICENSE_1_0.txt or copy at
 *           http://www.boost.org/LICENSE_1_0.txt)
 */

package com.keenfin.easypicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;
import static com.keenfin.easypicker.DownloadPhotoIntentService.EXTRA_HTTP_CODE;
import static com.keenfin.easypicker.DownloadPhotoIntentService.EXTRA_PREVIEW;
import static com.keenfin.easypicker.DownloadPhotoIntentService.EXTRA_TYPE;
import static com.keenfin.easypicker.DownloadPhotoIntentService.EXTRA_URL;
import static com.keenfin.easypicker.DownloadPhotoIntentService.EXTRA_URL_ID;
import static com.keenfin.easypicker.DownloadPhotoIntentService.EXTRA_VALUE_END;
import static com.keenfin.easypicker.DownloadPhotoIntentService.EXTRA_VALUE_START;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PhotoPicker extends RecyclerView {
    private int mMaxPhotos = -1;
    private int mImagesPerRow, mImagesPerRowPortrait = Constants.IMAGES_PER_ROW_P, mImagesPerRowLandscape = Constants.IMAGES_PER_ROW_L;
    private String mNewPhotosDir = Constants.NEW_PHOTOS_SAVE_DIR;
    private int mColorPrimary, mColorAccent;
    private int mCameraRequest, mPickRequest;
    private final int mPermissionRequest = 936;
    private boolean mIsOneLine = false, mIsUsePreview = true, mDefaultPreview = false;
    private boolean mPrimaryColorDefined, mAccentColorDefined;
    boolean mIsNougat;
    boolean mIsR;

    private Context mContext;
    private PhotoAdapter mPhotoAdapter;
    private Drawable mNewPhotoIcon;
    private String login;
    private String pass;

    ArrayList<String> urlIsInProgress = new ArrayList<>();

    public PhotoPicker(Context context, String login, String pass) {
        this(context, false, login, pass);
    }

    public PhotoPicker(Context context, boolean noControls,  String login, String pass) {
        super(context);

        this.login = login;
        this.pass = pass;
        mNewPhotoIcon = ContextCompat.getDrawable(context, R.drawable.ic_add_white_48dp);
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

    public void setLoginPass(String login, String pass){
        this.login = login;
        this.pass = pass;
        mPhotoAdapter.setLoginPass(login, pass);
    }

    private void init(Context context, boolean noControls) {
        mIsNougat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        mIsR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
        mContext = context;
        mImagesPerRow = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? mImagesPerRowLandscape : mImagesPerRowPortrait;
        RecyclerView.LayoutManager layoutManager;

        if (mIsOneLine)
            layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        else
            layoutManager = new GridLayoutManager(context, mImagesPerRow);

        setLayoutManager(layoutManager);
        setHasFixedSize(true);

        int[] attrs = new int[]{androidx.appcompat.R.attr.colorPrimary, androidx.appcompat.R.attr.colorAccent};
        TypedArray styleable = context.obtainStyledAttributes(attrs);
        if (!mPrimaryColorDefined)
            mColorPrimary = getColor(styleable, 0, R.color.primary);
        if (!mAccentColorDefined)
            mColorAccent = getColor(styleable, 1, R.color.accent);
        styleable.recycle();

        PhotoAdapter adapter = new PhotoAdapter(noControls, login, pass);
        setAdapter(adapter);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray styleable = context.obtainStyledAttributes(attrs, R.styleable.PhotoPicker, 0, 0);
        mImagesPerRowLandscape = styleable.getInt(R.styleable.PhotoPicker_photosPerRowLandscape, mImagesPerRowLandscape);
        mImagesPerRowPortrait = styleable.getInt(R.styleable.PhotoPicker_photosPerRowPortrait, mImagesPerRowPortrait);
        mIsOneLine = styleable.getBoolean(R.styleable.PhotoPicker_oneLineGallery, false);
        mIsOneLine = false;
        int icon = styleable.getResourceId(R.styleable.PhotoPicker_newPhotosIcon, R.drawable.ic_add_white_48dp);
        mNewPhotoIcon = ContextCompat.getDrawable(context, icon);
        boolean noControls = styleable.getBoolean(R.styleable.PhotoPicker_noControls, false);
        init(context, noControls);

        if (mPrimaryColorDefined = styleable.hasValue(R.styleable.PhotoPicker_primaryColor))
            mColorPrimary = getColor(styleable, R.styleable.PhotoPicker_primaryColor, R.color.primary);
        if (mAccentColorDefined = styleable.hasValue(R.styleable.PhotoPicker_accentColor))
            mColorAccent = getColor(styleable, R.styleable.PhotoPicker_accentColor, R.color.accent);

        mIsUsePreview = styleable.getBoolean(R.styleable.PhotoPicker_usePreview, true);
        mDefaultPreview = styleable.getBoolean(R.styleable.PhotoPicker_previewDefault, false);
        mMaxPhotos = styleable.getInt(R.styleable.PhotoPicker_maxPhotos, mMaxPhotos);
        mNewPhotosDir = styleable.getString(R.styleable.PhotoPicker_newPhotosDirectory);
        mNewPhotosDir = mNewPhotosDir == null ? Constants.NEW_PHOTOS_SAVE_DIR : mNewPhotosDir;
        styleable.recycle();
    }

    private int getColor(TypedArray array, int index, int defValue) {
        return array.getColor(index, mContext.getResources().getColor(defValue));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putParcelableArrayList(Constants.BUNDLE_ATTACHED_IMAGES, (ArrayList<? extends android.os.Parcelable>) mPhotoAdapter.getImagesPathOrUriAttach());
        //outState.putParcelableArrayList(KEY_STATES, (ArrayList<? extends android.os.Parcelable>) mListAdapter.getCheckState());
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

            List<AttachInfo> images = bundle.getParcelableArrayList(Constants.BUNDLE_ATTACHED_IMAGES);
            if (images == null)
                images = new ArrayList<>();
            mPhotoAdapter.restoreImages(images, null);
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
        if (!isInEditMode())
            mPhotoAdapter.measureParent();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPhotoAdapter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (!isInEditMode()) {
            if (!(adapter instanceof PhotoAdapter))
                throw new IllegalArgumentException("You should not pass adapter to PhotoPicker. It uses specific one.");

            mPhotoAdapter = (PhotoAdapter) adapter;
        }

        super.setAdapter(adapter);
    }

    public void restoreImages(List<AttachInfo> imagesPathOrUri, Map<String, AttachInfo> onlineAttaches) {
        mPhotoAdapter.restoreImages(imagesPathOrUri, onlineAttaches);
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

    public void setDefaultPreview(boolean defaultPreview) {
        mDefaultPreview = defaultPreview;
    }

    public void setNewPhotosDrawable(int drawableResourceId) {
        mPhotoAdapter.replaceNewPhotoIcon(drawableResourceId);
    }

    public List<AttachInfo> getImagesPathOrUri() {
        return mPhotoAdapter.getImagesPathOrUri();
    }

    public class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> implements PhotoViewHolder.IViewHolderClick {
        private final ArrayList<AttachInfo> mImagesPathOrUri;
        private Uri mPhotoUri;
        private String login;
        private String pass;
        private final boolean mNoControls;

        public PhotoAdapter(boolean noControls, String login, String pass) {
            mImagesPathOrUri = new ArrayList<>();
            mNoControls = noControls;
            this.login = login;
            this.pass = pass;

            if (!noControls)
                addNewPhotoIcon();
        }

        private void addNewPhotoIcon() {
            mImagesPathOrUri.add(0, null);
            notifyItemInserted(0);
        }

        protected void replaceNewPhotoIcon(int drawableResourceId) {
            if (!mNoControls && mImagesPathOrUri.size() > 0) {
                mNewPhotoIcon = ContextCompat.getDrawable(mContext, drawableResourceId);
                notifyItemChanged(0);
            }
        }

        public void setLoginPass(String login, String pass){
            this.login = login;
            this.pass = pass;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
            //view.setBackgroundColor(mColorAccent);
            view.setBackgroundColor(getResources().getColor(R.color.white));
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final PhotoViewHolder holder, final int position) {

            holder.setOnClickListener(this);
            boolean isControl = position == 0 && !mNoControls;
            holder.adjustControl(getMeasuredWidth() / mImagesPerRow, mColorPrimary, isControl, mIsOneLine, mNoControls);
            // todo

            if (isControl)
                holder.setIcon(mNewPhotoIcon);
            else {
                AttachInfo attachInfo = mImagesPathOrUri.get(position);
                holder.setOnline(attachInfo.onlineAttach ,mNoControls);

                if (!attachInfo.onlineAttach)
                    holder.loadPhoto(mContext, attachInfo.oldAttachString, getMeasuredWidth() / mImagesPerRow, null);
                else {
                    File cacheDir = getContext().getExternalCacheDir();
                    File fileInCache = new File(cacheDir.getAbsoluteFile() + "/" + attachInfo.storePath + attachInfo.filename);
                    File previewFile =  new File(cacheDir.getAbsoluteFile() + "/" + attachInfo.storePath + "preview_" + attachInfo.filename);


                    boolean inProgress = false;
                    for (String item: urlIsInProgress){
                        if (item.equals(attachInfo.url)) {
                            inProgress = true;
                            break;
                        }
                    }

                    if (!previewFile.exists() || previewFile.length() <= 0) {
                       if (!inProgress)
                        startDownloadPreviewImageFromWeb( position, login, pass,
                                holder.pImageWidth,
                                holder.pImageHeight);
                    }


                    if ( (fileInCache.exists() && fileInCache.length() > 0)|| (previewFile.exists() && previewFile.length() > 0)){
                        holder.loadPhoto(mContext, cacheDir.getAbsoluteFile() + "/" + attachInfo.storePath + attachInfo.filename, getMeasuredWidth() / mImagesPerRow, previewFile);
                    }

                    if (fileInCache.exists() && fileInCache.length() > 0){
                        holder.mPhotoDownload.setVisibility(GONE);
                    } else {
                        holder.mPhotoDownload.setVisibility(inProgress? GONE : VISIBLE);
                    }

                    holder.progressBar.setVisibility(inProgress? VISIBLE : GONE);
                        //holder.mPhoto.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.));
                        //holder.loadPhotoOnline(mContext,getMeasuredWidth() / mImagesPerRow,  attachInfo);
                    // online attach
                }
            }
        }

        @Override
        public int getItemCount() {
            return mImagesPathOrUri.size();
        }


        public ArrayList<AttachInfo> getImagesPathOrUriAttach() {
            ArrayList<AttachInfo> images = new ArrayList<>();
            for (AttachInfo attachInfo:mImagesPathOrUri)

                if (attachInfo != null && !attachInfo.onlineAttach)
                    images.add(attachInfo);
           return images;
        }

        public ArrayList<AttachInfo> getImagesPathOrUri() {
            return  mImagesPathOrUri;
        }

        protected void restoreImages(List<AttachInfo> imagesPathOrUri, Map<String, AttachInfo> onlineAttaches) {
            if (imagesPathOrUri!= null)
                for (AttachInfo image : imagesPathOrUri)
                    addImage(image);
            if (onlineAttaches != null)
                for (AttachInfo image : onlineAttaches.values())
                    addImage(image);
        }

        public Uri getPhotoUri() {
            return mPhotoUri;
        }

        public void setPhotoUri(Uri photoUri) {
            this.mPhotoUri = photoUri;
        }

        @Override
        public void onItemClick(View caller, int position) {
            onItemClick(caller.getId(), position);
        }

        @RequiresApi(api = Build.VERSION_CODES.R)
        private void askForAllFilesAccess() {
            String message = mContext.getString(R.string.manage_all_files_message);
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.manage_all_files)
                   .setMessage(message)
                   .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                           Intent permission = new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                           ((Activity) mContext).startActivityForResult(permission, mPermissionRequest);
                       }
                   })
                   .setNegativeButton(android.R.string.cancel, null)
                   .show();
        }

        private void onItemClick(int id, int position) {
            if (id == R.id.iv_photo) {
                if (position == 0 && !mNoControls) {
                    if (mMaxPhotos != -1 && getItemCount() - 1 >= mMaxPhotos) {
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
                                        photo = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), mNewPhotosDir);
                                        if (!photo.mkdirs() && !photo.exists()) {
                                            photo = mContext.getDir(mNewPhotosDir, 0);
                                            if (!photo.mkdirs() && !photo.exists()) {
                                                Toast.makeText(mContext, R.string.hw_error, Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        }
                                    }

                                    photo = new File(photo, System.currentTimeMillis() + ".jpg");
                                    mPhotoUri = Uri.fromFile(photo);
                                    Uri uri = mIsNougat ? FileProvider.getUriForFile(mContext, getUriProviderAuthority(getContext()), photo) : mPhotoUri;
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    mCameraRequest = randomizeRequest.nextInt(0xffff);
                                    ((Activity) mContext).startActivityForResult(intent, mCameraRequest);
                                    break;
                                case 1:

                                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
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

                    ArrayList<AttachInfo> imagesPath = getImagesPathOrUri();
                    int offset = position - (mNoControls ? 0 : 1);
                    Intent preview;

                    ArrayList<String> paths = new ArrayList<>();
                    for (AttachInfo info:imagesPath){
                        if (info == null)
                            continue;

                        if (info.onlineAttach) {
                            String filepath = getContext().getExternalCacheDir().getAbsolutePath() + "/" + info.storePath + "/" + info.filename;
                            paths.add(filepath);
                        } else {
                            String filepath = info.oldAttachString;
                            paths.add(filepath);
                        }
                    }

                    if (!mDefaultPreview) {
                        preview = new Intent(getContext(), PreviewActivity.class);
                        preview.putStringArrayListExtra(Constants.BUNDLE_ATTACHED_IMAGES, paths);
                        preview.putExtra(Constants.BUNDLE_NEW_PHOTO_PATH, offset);
                    } else {
                        preview = new Intent(Intent.ACTION_VIEW);
                        AttachInfo pathOrUriAttachInfo = imagesPath.get(offset);
                        String pathOrUri = getContext().getExternalCacheDir() + "/" + (pathOrUriAttachInfo.onlineAttach ? pathOrUriAttachInfo.storePath + pathOrUriAttachInfo.filename : pathOrUriAttachInfo.oldAttachString);

                        if (mIsR && !pathOrUri.startsWith("/")) {
                            preview.setDataAndType(Uri.parse(pathOrUri), "image/*");
                            preview.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else if (mIsNougat) {
                            File path = new File(pathOrUri);

                            if (!path.exists() || path.length() <= 0) {
                                startDownloadImageFromWeb( position, login, pass);
                                return;
                            }

                            preview.setDataAndType(FileProvider.getUriForFile(mContext, getUriProviderAuthority(getContext()), path), "image/*");
                            preview.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else
                            preview.setDataAndType(Uri.parse("file://" + pathOrUri), "image/*");
                    }

                    getContext().startActivity(preview);
                }
            } else if (id == R.id.ib_remove) {
                if (position <= 0)
                    return;

                mImagesPathOrUri.remove(position);
                notifyItemRemoved(position);
                measureParent();
            } else if (id == R.id.ib_download) { // download image
                if (position < 0)
                    return;
                startDownloadImageFromWeb( position, login, pass);
            }
        }

        void startDownloadImageFromWeb(int position, String login, String pass){
            AttachInfo attachInfo = mImagesPathOrUri.get(position);

            urlIsInProgress.add(attachInfo.url);


            DownloadPhotoIntentService.startActionDownload(
                    getContext(), attachInfo.url,
                    attachInfo.storePath,
                    attachInfo.filename,
                    login, pass);
            updateNotifyItemByURL(attachInfo.url);
        }

        void startDownloadPreviewImageFromWeb(int position, String login, String pass,
                                              int width, int height){
            AttachInfo attachInfo = mImagesPathOrUri.get(position);
            DownloadPhotoIntentService.startActionDownload(
                    getContext(), attachInfo.url + "?size=" + width +"x" +  height,
                    attachInfo.storePath,
                    "preview_" + attachInfo.filename,
                    login, pass, width, height, attachInfo.url);
        }

        void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                String selectedImagePath;

                if (requestCode == mCameraRequest) {
                    selectedImagePath = mPhotoUri.getPath();
                    MediaScannerConnection.scanFile(mContext, new String[]{mPhotoUri.getPath()}, null,
                                                    new MediaScannerConnection.OnScanCompletedListener() {
                                                        public void onScanCompleted(String path, Uri uri) {}
                                                    });
                } else if (requestCode == mPickRequest) {
                    if (data.getClipData() != null){ // multi select
                        int count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                        if (mMaxPhotos != -1 && count + getItemCount()-1 > mMaxPhotos) {
                                Toast.makeText(mContext, String.format(mContext.getString(R.string.max_photos), mMaxPhotos), Toast.LENGTH_SHORT).show();
                                return;
                        }
                        for(int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                                selectedImagePath = imageUri.toString();
                            } else {
                                selectedImagePath = FileUtil.getRealPath(mContext, imageUri);
                            }
                            addImage( new AttachInfo(false, selectedImagePath, "-1"));
                        }
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        selectedImagePath = data.getData().toString();
                    } else {
                        selectedImagePath = FileUtil.getRealPath(mContext, data.getData());
                    }
                } else if (requestCode == mPermissionRequest) {
                    onItemClick(R.id.iv_photo, 0);
                    return;
                } else {
                    return;
                }

                addImage(new AttachInfo(false, selectedImagePath, "-1"));
            }
        }

        private boolean addImage(AttachInfo imagePath) {
            if (imagePath == null) {
                Toast.makeText(mContext, mContext.getString(R.string.photo_fail_attach), Toast.LENGTH_SHORT).show();
                return false;
            }

            boolean result = mImagesPathOrUri.add(imagePath);
            notifyItemInserted(mImagesPathOrUri.size() - 1);
            measureParent();

            return result;
        }

        void measureParent() {
            ViewGroup.LayoutParams params = getLayoutParams();
            int itemsCount = mIsOneLine ? 1 : mImagesPathOrUri.size();
            params.height = (int) Math.ceil(1f * itemsCount / mImagesPerRow) * getMeasuredWidth() / mImagesPerRow;
            setLayoutParams(params);
        }

        public void updateStatus(Intent intent){
            Toast.makeText(getContext(), "intent", Toast.LENGTH_LONG).show();
        }
    }

    public static String getUriProviderAuthority(Context context) {
        return context.getPackageName() + ".easypicker.provider";
    }

    public void updateStatus(Intent intent){
        String url = intent.getExtras().getString(EXTRA_URL);
        String urlId = intent.getExtras().getString(EXTRA_URL_ID);
        String type = intent.getExtras().getString(EXTRA_TYPE);
        int responseCode = intent.getExtras().getInt(EXTRA_HTTP_CODE, -1);
        boolean previewOperation = intent.getExtras().getBoolean(EXTRA_PREVIEW, false);

        if (!previewOperation && responseCode == HTTP_NOT_FOUND){
            Toast.makeText(getContext(), "unable to locate picture on server", Toast.LENGTH_LONG).show();
        }

        //EXTRA_VALUE_START: EXTRA_VALUE_END
        if (type.equals(EXTRA_VALUE_START)){
            //urlIsInProgress.add(urlId);
        }

        if (type.equals(EXTRA_VALUE_END)){
            for (int i = 0; i<urlIsInProgress.size(); i++){
                if (urlIsInProgress.get(i).equals(urlId)) {
                    urlIsInProgress.remove(i);
                    break;
                }
            }

//            for (String item:urlIsInProgress) {
//                if (item.equals(urlId)) {
//                    urlIsInProgress.remove(item);
//                    break;
//                }
//            }
        }
        updateNotifyItemByURL(urlId);
    }

    public void updateNotifyItemByURL(String urlId){
        Integer elementPosition = getElementIdByUrl(urlId);

        if (elementPosition != null) {
            getAdapter().notifyItemChanged(elementPosition);
        }
    }

    public Integer getElementIdByUrl(String url){
        ArrayList<AttachInfo> elementsList = mPhotoAdapter.getImagesPathOrUri();
        for (int i =0; i < elementsList.size(); i++){
            AttachInfo info = elementsList.get(i);
            if (info == null)
                continue;
            if (info.onlineAttach && info.url != null && info.url.equals(url))
                return i;
            if (!info.onlineAttach && info.url != null && url.equals(info.oldAttachString))
                return i;
        }
        return null;
    }

    public void updateInProgressList(final ArrayList<String> list){
        urlIsInProgress.addAll(list);
    }
}
