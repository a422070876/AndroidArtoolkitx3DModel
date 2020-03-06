package com.hyq.hm.test.ar3dmodel;

import android.app.Activity;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.animation.Animator;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.collada.ColladaLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.stl.STLLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoaderTask;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class loads a 3D scena as an example of what can be done with the app
 *
 * @author andresoviedo
 */
public class SceneLoader implements LoaderTask.Callback {

    private Activity activity;
    private Uri paramUri;
    /**
     * List of data objects containing info for building the opengl objects
     */
    private List<Object3DData> objects = new ArrayList<>();
    /**
     * Animate model (dae only) or not
     */
    private boolean doAnimation = true;
    /**
     * Animator
     */
    private Animator animator = new Animator();
    /**
     * time when model loading has started (for stats)
     */
    private long startTime;

    public SceneLoader(Activity activity,Uri paramUri) {
        this.activity = activity;
        this.paramUri = paramUri;
    }


    public void init() {
        if (paramUri == null){
            return;
        }
        startTime = SystemClock.uptimeMillis();
        Uri uri = paramUri;
        Log.i("Object3DBuilder", "Loading model " + uri + ". async and parallel..");
        if (uri.toString().toLowerCase().endsWith(".obj")) {
            new WavefrontLoaderTask(activity, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".stl")) {
            Log.i("Object3DBuilder", "Loading STL object from: "+uri);
            new STLLoaderTask(activity, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".dae")) {
            Log.i("Object3DBuilder", "Loading Collada object from: "+uri);
            new ColladaLoaderTask(activity, uri, this).execute();
        }
    }
    /**
     * Hook for animating the objects before the rendering
     */
    public void onDrawFrame() {

        if (objects.isEmpty()) return;

        if (doAnimation) {
            for (int i=0; i<objects.size(); i++) {
                Object3DData obj = objects.get(i);
                animator.update(obj, false);
            }
        }
    }


    synchronized void addObject(Object3DData obj) {
        List<Object3DData> newList = new ArrayList<Object3DData>(objects);
        newList.add(obj);
        this.objects = newList;
    }


    public synchronized List<Object3DData> getObjects() {
        return objects;
    }





    public boolean isDoAnimation() {
        return doAnimation;
    }




    /**
     * Whether to draw using textures
     */
    private boolean drawTextures = true;
    public boolean isDrawTextures() {
        return drawTextures;
    }


    @Override
    public void onStart(){
        ContentUtils.setThreadActivity(activity);
        if(listener != null){
            listener.onStart();
        }
    }

    @Override
    public void onLoadComplete(List<Object3DData> datas) {
        // TODO: move texture load to LoaderTask
        for (Object3DData data : datas) {
            if (data.getTextureData() == null && data.getTextureFile() != null) {
                Log.i("LoaderTask","Loading texture... "+data.getTextureFile());
                try (InputStream stream = ContentUtils.getInputStream(data.getTextureFile())){
                    if (stream != null) {
                        data.setTextureData(IOUtils.read(stream));
                    }
                } catch (IOException ex) {
                    data.addError("Problem loading texture " + data.getTextureFile());
                }
            }
        }

        // TODO: move error alert to LoaderTask
        List<String> allErrors = new ArrayList<>();
        for (Object3DData data : datas) {
            addObject(data);
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()){
            Log.e("SceneLoader", allErrors.toString());
//            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        Log.i("LoaderTask","Build complete (" + elapsed + ")");
        ContentUtils.setThreadActivity(null);
        if(listener != null){
            listener.onLoadComplete(this);
        }
    }

    @Override
    public void onLoadError(Exception ex) {
        Log.e("SceneLoader", ex.getMessage(), ex);
        ContentUtils.setThreadActivity(null);
        if(listener != null){
            listener.onLoadError(ex);
        }
    }
    /**
     * Enable or disable blending (transparency)
     */
    private boolean isBlendingEnabled = true;
    public boolean isBlendingEnabled() {
        return isBlendingEnabled;
    }
    /**
     * Force transparency
     */
    private boolean isBlendingForced = false;
    public boolean isBlendingForced() {
        return isBlendingForced;
    }

    /**
     * Whether to draw using colors or use default white color
     */
    private boolean drawColors = true;
    public boolean isDrawColors() {
        return drawColors;
    }
    /**
     * Light toggle feature: whether to draw using lights
     */
    private boolean drawLighting = true;
    public boolean isDrawLighting() {
        return drawLighting;
    }

    private OnSceneLoaderListener listener;

    public void setOnSceneLoaderListener(OnSceneLoaderListener listener) {
        this.listener = listener;
    }

    public interface OnSceneLoaderListener {

        void onStart();

        void onLoadError(Exception ex);

        void onLoadComplete(SceneLoader scene);
    }
}
