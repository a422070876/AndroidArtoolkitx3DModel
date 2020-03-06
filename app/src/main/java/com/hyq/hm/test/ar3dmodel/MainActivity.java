package com.hyq.hm.test.ar3dmodel;


import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;

import org.andresoviedo.util.android.AndroidURLStreamHandlerFactory;
import org.andresoviedo.util.android.ContentUtils;
import org.artoolkitx.arx.arxj.ARActivity;
import org.artoolkitx.arx.arxj.rendering.ARRenderer;

import java.net.URL;

public class MainActivity extends ARActivity {
    static {
        System.setProperty("java.protocol.handler.pkgs", "org.andresoviedo.util.android");
        URL.setURLStreamHandlerFactory(new AndroidURLStreamHandlerFactory());
    }

    private SceneLoader scene;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ContentUtils.provideAssets(this);
        scene = new SceneLoader(this, Uri.parse("assets://" + getPackageName() + "/models/cowboy.dae"));
        scene.init();
    }

    /**
     * Provide our own ARSquareTrackingRenderer.
     */
    @Override
    protected ARRenderer supplyRenderer() {
        return new ModelRenderer(this,scene);
    }

    /**
     * Use the FrameLayout in this Activity's UI.
     */
    @Override
    protected FrameLayout supplyFrameLayout() {
        return (FrameLayout) this.findViewById(R.id.mainFrameLayout);
    }

}
