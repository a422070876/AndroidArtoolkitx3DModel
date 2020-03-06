package com.hyq.hm.test.ar3dmodel;

import android.app.Application;

import org.artoolkitx.arx.arxj.assets.AssetHelper;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AssetHelper assetHelper = new AssetHelper(getAssets());
        assetHelper.cacheAssetFolder(this, "Data");
        assetHelper.cacheAssetFolder(this, "cparam_cache");
    }
}
