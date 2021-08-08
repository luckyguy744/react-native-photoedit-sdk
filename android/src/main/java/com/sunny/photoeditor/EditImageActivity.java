package com.sunny.photoeditor;
/**
 * Created by branislav.karan.1979@gmail.com 2/9/2021
 */
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.sunny.photoeditor.base.BaseActivity;
import com.sunny.photoeditor.filters.FilterListener;
import com.sunny.photoeditor.filters.FilterViewAdapter;
import com.sunny.photoeditor.tools.EditingToolsAdapter;
import com.sunny.photoeditor.tools.ToolType;
import com.sunny.photoeditsdk.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;
import ja.burhanrashid52.photoeditor.ViewType;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

public class EditImageActivity extends BaseActivity implements OnPhotoEditorListener,
        View.OnClickListener,
        PropertiesBSFragment.Properties,
        EmojiBSFragment.EmojiListener,
        StickerBSFragment.StickerListener, EditingToolsAdapter.OnItemSelected, FilterListener {

    private static final String TAG = EditImageActivity.class.getSimpleName();
    private static final int CAMERA_REQUEST = 52;
    private static final int PICK_REQUEST = 53;
    PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;
    private PropertiesBSFragment mPropertiesBSFragment;
    private EmojiBSFragment mEmojiBSFragment;
    private StickerBSFragment mStickerBSFragment;
    private TextView mTxtCurrentTool;
    private RecyclerView mRvTools, mRvFilters;
    private EditingToolsAdapter mEditingToolsAdapter = new EditingToolsAdapter(this);
    private FilterViewAdapter mFilterViewAdapter = new FilterViewAdapter(this);
    private ConstraintLayout mRootView;
    private ConstraintSet mConstraintSet = new ConstraintSet();
    private boolean mIsFilterVisible;
    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    private boolean cropperCircleOverlay = false;
    private boolean freeStyleCropEnabled = true;
    private boolean showCropGuidelines = true;
    private boolean hideBottomControls = false;

    private ImageView imgUndo;
    private ImageView imgRedo;

    @Nullable
    @VisibleForTesting
    Uri mSaveImageUri;
    String selected_image =null;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_edit_image);

        selected_image = getIntent().getExtras().getString("image_path");
        boolean visible_gallery = getIntent().getExtras().getBoolean("visible_gallery");
        boolean visible_camera = getIntent().getExtras().getBoolean("visible_camera");
        initViews(visible_gallery, visible_camera);

        Bitmap bitmap = getBitmapFromUrl(selected_image);

        if (bitmap == null) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("closed_code", 0);
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
            return;
        }
        mPhotoEditorView.getSource().setImageBitmap(bitmap);


        handleIntentImage(mPhotoEditorView.getSource());

        mPropertiesBSFragment = new PropertiesBSFragment();
        mEmojiBSFragment = new EmojiBSFragment();

        ArrayList<Integer> stickerList = new ArrayList<>();
        for (int i = 0; i < 119; i++) {
            stickerList.add(getResources().getIdentifier("image_" + i, "drawable", getPackageName()));
        }

        mStickerBSFragment = new StickerBSFragment(stickerList);
        mStickerBSFragment.setStickerListener(this);
        mEmojiBSFragment.setEmojiListener(this);
        mPropertiesBSFragment.setPropertiesChangeListener(this);

        LinearLayoutManager llmTools = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvTools.setLayoutManager(llmTools);
        mRvTools.setAdapter(mEditingToolsAdapter);

        LinearLayoutManager llmFilters = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRvFilters.setLayoutManager(llmFilters);
        mRvFilters.setAdapter(mFilterViewAdapter);

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                //.setDefaultTextTypeface(mTextRobotoTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build();
        mPhotoEditor.setOnPhotoEditorListener(this);
    }


    private String getTmpDir(Activity activity) {
        String tmpDir = activity.getCacheDir() + "/photo-editor";
        new File(tmpDir).mkdir();

        return tmpDir;
    }

    private void startCropping() {
        System.out.println(selected_image);
        selected_image = selected_image.replace("file://", "");
        Uri uri = Uri.fromFile(new File(selected_image));
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(100);
        options.setCircleDimmedLayer(cropperCircleOverlay);
        options.setFreeStyleCropEnabled(freeStyleCropEnabled);
        options.setShowCropGrid(showCropGuidelines);
        options.setHideBottomControls(hideBottomControls);
        options.setAllowedGestures(
                UCropActivity.ALL, // When 'scale'-tab active
                UCropActivity.ALL, // When 'rotate'-tab active
                UCropActivity.ALL  // When 'aspect ratio'-tab active
        );


        UCrop uCrop = UCrop
                .of(uri, Uri.fromFile(new File(this.getTmpDir(this), UUID.randomUUID().toString() + ".jpg")))
                .withOptions(options);

        uCrop.start(this);
    }
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected String getPath(final Uri uri) {
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(this, uri)) {
            // ExternalStorageProvider
            if (GalleryUtils.isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }
            }
            // DownloadsProvider
            else if (GalleryUtils.isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return GalleryUtils.getDataColumn(this, contentUri, null, null);
            }
            // MediaProvider
            else if (GalleryUtils.isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return GalleryUtils.getDataColumn(this, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return GalleryUtils.getDataColumn(this, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    private void handleIntentImage(ImageView source) {
        Intent intent = getIntent();
        if (intent != null) {
            String intentType = intent.getType();
            if (intentType != null && intentType.startsWith("image/")) {
                Uri imageUri = intent.getData();
                if (imageUri != null) {
                    source.setImageURI(imageUri);
                }
            }
        }
    }

    private void initViews(boolean visible_gallery, boolean visible_camera) {
        ImageView imgCamera;
        ImageView imgGallery;
        ImageView imgSave;
        ImageView imgClose;
        ImageView imgShare;
        ImageView imgDone;

        mPhotoEditorView = findViewById(R.id.photoEditorView);
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool);
        mRvTools = findViewById(R.id.rvConstraintTools);
        mRvFilters = findViewById(R.id.rvFilterView);
        mRootView = findViewById(R.id.rootView);


        imgUndo = findViewById(R.id.imgUndo);
        imgUndo.setOnClickListener(this);

        imgRedo = findViewById(R.id.imgRedo);
        imgRedo.setOnClickListener(this);
        imgUndo.setAlpha(0.3f);
        imgRedo.setAlpha(0.3f);


        imgCamera = findViewById(R.id.imgCamera);
        imgCamera.setOnClickListener(this);

        imgGallery = findViewById(R.id.imgGallery);
        imgGallery.setOnClickListener(this);

        imgSave = findViewById(R.id.imgSave);
        imgSave.setOnClickListener(this);

        imgClose = findViewById(R.id.imgClose);
        imgClose.setOnClickListener(this);

        imgShare = findViewById(R.id.imgShare);
        imgShare.setOnClickListener(this);

        imgDone = findViewById(R.id.imgDone);
        imgDone.setOnClickListener(this);

        if(!visible_gallery){
            imgGallery.setVisibility(View.GONE);
        }
        if(!visible_camera){
            imgCamera.setVisibility(View.GONE);
        }

    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode) {
        TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
            @Override
            public void onDone(String inputText, int colorCode) {
                final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                styleBuilder.withTextColor(colorCode);

                mPhotoEditor.editText(rootView, inputText, styleBuilder);
                mTxtCurrentTool.setText(R.string.label_text);
            }
        });
    }
    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        imgUndo.setAlpha(1f);
        Log.d(TAG, "onAddViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
        boolean undoEnable = numberOfAddedViews != 0;
        imgUndo.setAlpha(undoEnable ?  1f : 0.3f);
        imgRedo.setAlpha(1f);
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [" + viewType + "]");
        imgRedo.setAlpha(0.3f);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imgUndo) {
            mPhotoEditor.undo();
        } else if (id == R.id.imgRedo) {
            boolean redoEnable = mPhotoEditor.redo();
            imgRedo.setAlpha(redoEnable ?  1f : 0.3f);
        } else if (id == R.id.imgSave) {
            saveImage();
        } else if (id == R.id.imgClose) {
            onBackPressed();
        } else if (id == R.id.imgShare) {
            shareImage();
        } else if (id == R.id.imgCamera) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else if (id == R.id.imgGallery) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST);
        } else if (id == R.id.imgDone) {
            onCompleteEdit();
        }
    }

    private void shareImage() {
        if (mSaveImageUri == null) {
            showSnackbar(getString(R.string.msg_save_image_to_share));
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        File myFile = new File(mSaveImageUri.getPath());

        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(myFile));
        startActivity(Intent.createChooser(intent, getString(R.string.msg_share_image)));
    }

    private void onCompleteEdit(){
        String confirm_message = getIntent().getExtras().getString("confirm_message");
        if(confirm_message == null || confirm_message.equals("")){
            saveAndReturn();
            return;
        }
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(confirm_message)
                .setConfirmText("Yes")
                .setCancelText("No")
                .showCancelButton(true)
                .showContentText(false)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        saveAndReturn();
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }
    @SuppressLint("MissingPermission")
    private void saveAndReturn(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageName = "/IMG_" + timeStamp + ".jpg";
        final String newPath = getCacheDir() + imageName;
        File file = new File(newPath);
        try {
            file.createNewFile();
            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(true)
                    .build();
            mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                @Override
                public void onSuccess(@NonNull String imagePath) {
                    mSaveImageUri = Uri.fromFile(new File(imagePath));
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("imagePath", newPath);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("closed_code", 1);
                    setResult(Activity.RESULT_CANCELED, returnIntent);
                    finish();
                }
            });
        }catch (Exception err){
            err.printStackTrace();
        }
    }
    public Bitmap getBitmapFromUrl(String url){
        if (url.contains("content://")) {
            url = getPath(Uri.parse(url));
        }

        if(url == null || url.isEmpty()){
          return null;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            return BitmapFactory.decodeFile(url, options);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public Bitmap markImageByBitmap(Bitmap source, Bitmap watermark){
        Bitmap bmp;
        int w, h;
        Canvas c;
        Paint paint;
        Matrix matrix;
        float scale;
        RectF r;
        w = source.getWidth();
        h = source.getHeight();
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        try {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
            c = new Canvas(bmp);
            c.drawBitmap(source, 0, 0, paint);
            scale = (float) (((float) w * 0.40) / (float) watermark.getWidth());
            matrix = new Matrix();
            matrix.postScale(scale, scale);
            r = new RectF(0, 0, watermark.getWidth(), watermark.getHeight());
            matrix.mapRect(r);
            matrix.postTranslate((w - r.width()), (h - r.height()));
            c.drawBitmap(watermark, matrix, paint);
            watermark.recycle();
        }catch (Exception err){
            err.printStackTrace();
        }
        return bmp;
    }
    public void addWatermark(final Bitmap bg, final String uri) {
        if(isFrescoImg(uri) == false){
            int resourceId = getResources().getIdentifier(uri, "drawable", getPackageName());
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceId);
            if (bitmap != null) {
                Bitmap newBmp = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
                Bitmap bmp_mark = markImageByBitmap(bg, newBmp);
                saveToFile(bmp_mark);
            }
            hideLoading();
            return;
        }
        ImageRequest imageRequest = ImageRequest.fromUri(uri);
        DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest, getApplicationContext());
        Executor executor = Executors.newSingleThreadExecutor();
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            public void onNewResultImpl(Bitmap bitmap) {
                if (bitmap != null) {
                    Bitmap newBmp = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
                    Bitmap bmp_mark = markImageByBitmap(bg, newBmp);
                    saveToFile(bmp_mark);
                }
                hideLoading();
            }
            @Override
            public void onFailureImpl(DataSource dataSource) {
                hideLoading();
            }
            @Override
            public void onFailure(DataSource<CloseableReference<CloseableImage>> dataSource) {
                hideLoading();
            }
        }, executor);
    }
    private Boolean isFrescoImg(String uri) {
        return uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("file://") || (uri.startsWith("data:") && uri.contains("base64") && (uri.contains("img") || uri.contains("image")));
    }

    @SuppressLint("MissingPermission")
    private void saveImage() {
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading("Saving...");
            SaveSettings saveSettings = new SaveSettings
                    .Builder()
                    .setClearViewsEnabled(false)
                    .setCompressQuality(100)
                    .setTransparencyEnabled(true)
                    .setCompressFormat(Bitmap.CompressFormat.PNG)
                    .build();
            mPhotoEditor.saveAsBitmap(saveSettings, new OnSaveBitmap() {
                @Override
                public void onBitmapReady(Bitmap saveBitmap) {
                    String watermark = getIntent().getExtras().getString("watermark");
                    boolean draw_watermark = getIntent().getExtras().getBoolean("draw_watermark");
                    if(draw_watermark){
                        addWatermark(saveBitmap, watermark);
                        return;
                    }
                    saveToFile(saveBitmap);
                    hideLoading();
                }
                @Override
                public void onFailure(Exception e) {
                    e.printStackTrace();
                    hideLoading();
                }
            });
        }
    }
    private void saveToFile(Bitmap saveBitmap){
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), System.currentTimeMillis() + ".png");
            FileOutputStream fOut = new FileOutputStream(file);
            saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            mSaveImageUri = Uri.fromFile(file);
//            mPhotoEditorView.getSource().setImageURI(mSaveImageUri);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(mSaveImageUri);
            sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST:
                    mPhotoEditor.clearAllViews();
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    mPhotoEditorView.getSource().setImageBitmap(photo);
                    break;
                case PICK_REQUEST:
                    try {
                        mPhotoEditor.clearAllViews();
                        Uri uri = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        mPhotoEditorView.getSource().setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case UCrop.REQUEST_CROP:
                    if (data != null) {
                        final Uri resultUri = UCrop.getOutput(data);
                        if (resultUri != null) {
                            try {
                                selected_image = resultUri.toString();
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver() , resultUri);
                                mPhotoEditorView.getSource().setImageBitmap(bitmap);
                            } catch (Exception ex) {
                                System.out.println("NO IMAGE DATA FOUND");
                            }
                        } else {
                            System.out.println("NO IMAGE DATA FOUND");
                        }
                    } else {
                        System.out.println("NO RESULT");
                    }
                    break;
            }
        }
    }

    @Override
    public void onColorChanged(int colorCode) {
        mPhotoEditor.setBrushColor(colorCode);
        mTxtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onOpacityChanged(int opacity) {
        mPhotoEditor.setOpacity(opacity);
        mTxtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onBrushSizeChanged(int brushSize) {
        mPhotoEditor.setBrushSize(brushSize);
        mTxtCurrentTool.setText(R.string.label_brush);
    }

    @Override
    public void onEmojiClick(String emojiUnicode) {
        mPhotoEditor.addEmoji(emojiUnicode);
        mTxtCurrentTool.setText(R.string.label_emoji);
    }

    @Override
    public void onStickerClick(Bitmap bitmap) {
        mPhotoEditor.addImage(bitmap);
        mTxtCurrentTool.setText(R.string.label_sticker);
    }

    @Override
    public void isPermissionGranted(boolean isGranted, String permission) {
        if (isGranted) {
            saveImage();
        }
    }

    @Override
    public void onFilterSelected(PhotoFilter photoFilter) {
        mPhotoEditor.setFilterEffect(photoFilter);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onToolSelected(ToolType toolType) {
        switch (toolType) {
            case CROP:
                startCropping();
                break;
            case BRUSH:
                mPhotoEditor.setBrushDrawingMode(true);
                mPhotoEditor.setBrushColor(getColor(R.color.blue_color_picker));
                mTxtCurrentTool.setText(R.string.label_brush);
                showBottomSheetDialogFragment(mPropertiesBSFragment);
                break;
            case TEXT:
                TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this, "", getColor(R.color.blue_color_picker));
                textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
                    @Override
                    public void onDone(String inputText, int colorCode) {
                        final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                        styleBuilder.withTextColor(colorCode);

                        mPhotoEditor.addText(inputText, styleBuilder);
                        mTxtCurrentTool.setText(R.string.label_text);
                    }
                });
                break;
            case ERASER:
                mPhotoEditor.brushEraser();
                mTxtCurrentTool.setText(R.string.label_eraser_mode);
                break;
            case FILTER:
                mTxtCurrentTool.setText(R.string.label_filter);
                showFilter(true);
                break;
            case EMOJI:
                showBottomSheetDialogFragment(mEmojiBSFragment);
                break;
            case STICKER:
                showBottomSheetDialogFragment(mStickerBSFragment);
                break;
        }
    }

    private void showBottomSheetDialogFragment(BottomSheetDialogFragment fragment) {
        if (fragment == null || fragment.isAdded()) {
            return;
        }
        fragment.show(getSupportFragmentManager(), fragment.getTag());
    }


    void showFilter(boolean isVisible) {
        mIsFilterVisible = isVisible;
        mConstraintSet.clone(mRootView);

        if (isVisible) {
            mConstraintSet.clear(mRvFilters.getId(), ConstraintSet.START);
            mConstraintSet.connect(mRvFilters.getId(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.START);
            mConstraintSet.connect(mRvFilters.getId(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END);
        } else {
            mConstraintSet.connect(mRvFilters.getId(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID, ConstraintSet.END);
            mConstraintSet.clear(mRvFilters.getId(), ConstraintSet.END);
        }

        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(350);
        changeBounds.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
        TransitionManager.beginDelayedTransition(mRootView, changeBounds);

        mConstraintSet.applyTo(mRootView);
    }

    private void onCloseEdit(){
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure to close?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .showCancelButton(true)
                .showContentText(false)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("closed_code", 2);
                        setResult(Activity.RESULT_CANCELED, returnIntent);
                        finish();
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }
    @Override
    public void onBackPressed() {
        if (mIsFilterVisible) {
            showFilter(false);
            mTxtCurrentTool.setText(R.string.app_name);
        } else {
            onCloseEdit();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
