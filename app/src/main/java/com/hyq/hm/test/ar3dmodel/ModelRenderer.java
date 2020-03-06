package com.hyq.hm.test.ar3dmodel;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.drawer.DrawerFactory;
import org.andresoviedo.android_3d_model_engine.model.Object3D;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.android.GLUtil;
import org.artoolkitx.arx.arxj.ARController;
import org.artoolkitx.arx.arxj.Trackable;
import org.artoolkitx.arx.arxj.rendering.ARRenderer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ModelRenderer extends ARRenderer {

    private final static String TAG = ModelRenderer.class.getName();
    /**
     * Add 0.5f to the alpha component to the global shader so we can see through the skin
     */
    private static final float[] BLENDING_FORCED_MASK_COLOR = {1.0f, 1.0f, 1.0f, 0.5f};


    // width of the screen
    private int width;
    // height of the screen
    private int height;

    /**
     * Drawer factory to get right renderer/shader based on object attributes
     */
    private DrawerFactory drawer;

    // The loaded textures
    private Map<Object, Integer> textures = new HashMap<>();

    // 3D matrices to project our 3D world
    private final float[] lightPosInWorldSpace = new float[4];
    private final float[] cameraPosInWorldSpace = new float[3];
    /**
     * Whether the info of the model has been written to console log
     */
    private Map<Object3DData, Boolean> infoLogged = new HashMap<>();

    /**
     * Did the application explode?
     */
    private boolean fatalException = false;

    private SceneLoader scene;


    private static final Trackable trackables[] = new Trackable[]{
            new Trackable("hiro", 80.0f),
            new Trackable("kanji", 80.0f)
    };
    private int trackableUIDs[] = new int[trackables.length];
    /**
     * Markers can be configured here.
     */
    @Override
    public boolean configureARScene() {
        int i = 0;
        for (Trackable trackable : trackables) {
            trackableUIDs[i] = ARController.getInstance().addTrackable("single;Data/" + trackable.getName() + ".patt;" + trackable.getWidth());
            if (trackableUIDs[i] < 0) return false;
            i++;
        }
        return true;
    }



    /**
     * Construct a new renderer for the specified surface view
     *            the 3D window
     */
    public ModelRenderer(Context context, SceneLoader scene) {
        this.scene = scene;
        // This component will draw the actual models using OpenGL
        try {
            drawer = new DrawerFactory(context);
        } catch (IllegalAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private float[] backgroundColor = new float[]{0f, 0f, 0f, 1.0f};



    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        super.onSurfaceCreated(unused,config);
        // Set the background frame color
        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        // Use culling to remove back faces.
        // Don't remove back faces so we can see them
        // GLES20.glEnable(GLES20.GL_CULL_FACE);


    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        super.onSurfaceChanged(unused,width,height);
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw() {
        super.draw();
        if(fatalException){
            return;
        }
        GLES20.glViewport(0, 0, width, height);
        GLES20.glScissor(0, 0, width, height);
        // Draw background color
        // Enable depth testing for hidden-surface elimination.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Enable not drawing out of view port
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        if (scene == null) {
            // scene not ready
            return;
        }

        for (int trackableUID : trackableUIDs) {
            // If the trackable is visible, apply its transformation, and render a cube
            float[] modelViewMatrix = new float[16];
            if (ARController.getInstance().queryTrackableVisibilityAndTransformation(trackableUID, modelViewMatrix)) {
                float[] projectionMatrix = ARController.getInstance().getProjectionMatrix(10.0f, 10000.0f);
                onDrawFrame(projectionMatrix,modelViewMatrix);
            }
        }


    }
    private void onDrawFrame(float[] projectionMatrix,float[] modelViewMatrix){
        try {
            float[] colorMask = null;
            if (scene.isBlendingEnabled()) {
                // Enable blending for combining colors when there is transparency
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                if (scene.isBlendingForced()){
                    colorMask = BLENDING_FORCED_MASK_COLOR;
                }
            } else {
                GLES20.glDisable(GLES20.GL_BLEND);
            }

            // animate scene
            scene.onDrawFrame();


            this.onDrawFrame(modelViewMatrix, projectionMatrix,colorMask, cameraPosInWorldSpace);
        }catch (Exception ex){
            Log.e("ModelRenderer", "Fatal exception: "+ex.getMessage(), ex);
            fatalException = true;
        }
    }
    private void onDrawFrame(float[] viewMatrix, float[] projectionMatrix,float[] colorMask,float[] cameraPosInWorldSpace) {
        if (scene.getObjects().isEmpty()){
            return;
        }
        // draw all available objects
        List<Object3DData> objects = scene.getObjects();
        for (int i=0; i<objects.size(); i++) {
            Object3DData objData = null;
            try {
                objData = objects.get(i);
                if (!objData.isVisible()) continue;
                Object3D drawerObject = drawer.getDrawer(objData, scene.isDrawTextures(), scene.isDrawLighting(),
                        scene.isDoAnimation(), scene.isDrawColors());
                if (drawerObject == null){
                    continue;
                }
                float scale = 20;
                objData.setScale(new float[]{scale,scale,scale});
                if (!infoLogged.containsKey(objData)) {
                    Log.v("ModelRenderer","Drawing model: "+objData.getId());
                    infoLogged.put(objData, true);
                }
                // load model texture
                Integer textureId = textures.get(objData.getTextureData());
                if (textureId == null && objData.getTextureData() != null) {
                    Log.i("ModelRenderer","Loading texture '"+objData.getTextureFile()+"'...");
                    ByteArrayInputStream textureIs = new ByteArrayInputStream(objData.getTextureData());
                    textureId = GLUtil.loadTexture(textureIs);
                    textureIs.close();
                    textures.put(objData.getTextureData(), textureId);
                    Log.i("GLUtil", "Loaded texture ok. id: "+textureId);
                }
                if (textureId == null){
                    textureId = -1;
                }
                drawerObject.draw(objData, projectionMatrix, viewMatrix,
                        textureId, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);


            } catch (Exception ex) {
                Log.e("ModelRenderer","There was a problem rendering the object '"+objData.getId()+"':"+ex.getMessage(),ex);
            }
        }
    }
}
