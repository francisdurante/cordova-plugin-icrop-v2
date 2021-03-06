package com.isathyam.icrop;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.model.AspectRatio;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class CropPlugin extends CordovaPlugin {
    private CallbackContext callbackContext;
    private Uri inputUri;
    private Uri outputUri;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
      if (action.equals("cropImage")) {
          String imagePath = args.getString(0);
          String sourceType = args.getString(1);
          JSONObject jsonObject = new JSONObject(sourceType);
          String targetSourceType  = jsonObject.getString("sourceType");
          String type = jsonObject.getString("type");


          this.inputUri = targetSourceType.equals("1") ? Uri.parse(imagePath) : Uri.parse("file:///"+imagePath);

          this.outputUri = Uri.fromFile(new File(getTempDirectoryPath() + "/" + System.currentTimeMillis()+ "-cropped.jpg"));

          PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
          pr.setKeepCallback(true);
          callbackContext.sendPluginResult(pr);
          this.callbackContext = callbackContext;
          UCrop.Options option = new UCrop.Options();
          option.setStatusBarColor(Color.BLACK);
          option.setActiveWidgetColor(Color.GRAY);
          option.setLogoColor(Color.GRAY);
          option.setRootViewBackgroundColor(Color.BLACK);
          option.setCompressionQuality(100);
          option.setShowCropGrid(true);
          if("profile".equals(type)) {
            option.setAspectRatioOptions(2,
              new AspectRatio("1x1", 1, 1),
              new AspectRatio("3x4", 3, 4),
              new AspectRatio("F!eek size", 9, 16),
              new AspectRatio("3x2", 3, 2),
              new AspectRatio("16:9", 16, 9)
            );
          }else
          {
            option.setAspectRatioOptions(0,
              new AspectRatio("16:9", 16, 9));
            option.setFreeStyleCropEnabled(false);
          }
          cordova.setActivityResultCallback(this);
          UCrop.of(this.inputUri, this.outputUri)
                  .withOptions(option)
                  .start(cordova.getActivity());

          return true;
      }
      return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == Activity.RESULT_OK) {
                Uri imageUri = UCrop.getOutput(intent);
                this.callbackContext.success("file://" + imageUri.getPath()); // + "?" + System.currentTimeMillis()
                this.callbackContext = null;
            } else if (resultCode == UCrop.RESULT_ERROR) {
                try {
                    JSONObject err = new JSONObject();
                    err.put("message", "Error on cropping");
                    err.put("code", String.valueOf(resultCode));
                    this.callbackContext.error(err);
                    this.callbackContext = null;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                try {
                    JSONObject err = new JSONObject();
                    err.put("message", "User cancelled");
                    err.put("code", "userCancelled");
                    this.callbackContext.error(err);
                    this.callbackContext = null;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private String getTempDirectoryPath() {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Android/data/" + cordova.getActivity().getPackageName() + "/cache/");
        }
        // Use internal storage
        else {
            cache = cordova.getActivity().getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }

    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();

        if (this.inputUri != null) {
            state.putString("inputUri", this.inputUri.toString());
        }

        if (this.outputUri != null) {
            state.putString("outputUri", this.outputUri.toString());
        }

        return state;
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {

        if (state.containsKey("inputUri")) {
            this.inputUri = Uri.parse(state.getString("inputUri"));
        }

        if (state.containsKey("outputUri")) {
            this.inputUri = Uri.parse(state.getString("outputUri"));
        }

        this.callbackContext = callbackContext;
    }
}
