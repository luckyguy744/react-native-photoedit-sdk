package com.sunny.photoeditsdk;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.sunny.photoeditor.EditImageActivity;

public class PhotoeditSdkModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public PhotoeditSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @Override
    public String getName() {
        return "PhotoeditSdk";
    }

    private static final int PHOTO_EDITOR_REQUEST = 1;

    private Callback mDoneCallback;
    private Callback mCancelCallback;

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            if (requestCode == PHOTO_EDITOR_REQUEST) {

                if (mDoneCallback != null) {

                    if (resultCode == Activity.RESULT_CANCELED) {
                        try{
                            mCancelCallback.invoke(intent.getExtras().getString("closed_code"));
                        }catch (Exception err){
                            mCancelCallback.invoke(-1);
                        }
                    } else {
                        mDoneCallback.invoke(intent.getExtras().getString("imagePath"));
                    }

                }

                mCancelCallback = null;
                mDoneCallback = null;
            }
        }
    };

    @ReactMethod
    public void Edit(final ReadableMap props, final Callback onDone, final Callback onCancel) {
        String path = props.getString("path");
        String watermark = props.getString("watermark");
        boolean visible_gallery = props.getBoolean("visible_gallery");
        boolean visible_camera = props.getBoolean("visible_camera");
        boolean draw_watermark = props.getBoolean("draw_watermark");
        String confirm_message = props.getString("confirm_message");

        Intent intent = new Intent(getCurrentActivity(), EditImageActivity.class);
        intent.putExtra("image_path", path);
        intent.putExtra("visible_gallery", visible_gallery);
        intent.putExtra("visible_camera", visible_camera);
        intent.putExtra("watermark", watermark);
        intent.putExtra("draw_watermark", draw_watermark);
        intent.putExtra("confirm_message", confirm_message);


        mCancelCallback = onCancel;
        mDoneCallback = onDone;

        getCurrentActivity().startActivityForResult(intent, PHOTO_EDITOR_REQUEST);
    }
}
