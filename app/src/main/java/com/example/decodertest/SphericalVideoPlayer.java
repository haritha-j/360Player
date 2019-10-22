/**
 * Copyright 2016-present, Facebook, Inc.
 * All rights reserved.
 * <p>
 * Licensed under the Creative Commons CC BY-NC 4.0 Attribution-NonCommercial
 * License (the "License"). You may obtain a copy of the License at
 * https://creativecommons.org/licenses/by-nc/4.0/.
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.decodertest;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
//import android.widget.Toast;

import com.example.decodertest.gles.EGLRenderTarget;
import com.example.decodertest.gles.GLHelpers;
import com.example.decodertest.gles.SphericalSceneRenderer;

import java.io.IOException;

import static android.content.Context.SENSOR_SERVICE;
import static com.example.decodertest.MainActivity.toast;

/**
 * A TextureView that can playback 360 video with support for drag to rotate.
 */
public class SphericalVideoPlayer extends TextureView implements SensorEventListener {
    private static final String TAG = SphericalVideoPlayer.class.getSimpleName();
    private static final String RENDER_THREAD_NAME = "360RenderThread";

    private MediaPlayer videoPlayerInternal;
    private RenderThread renderThread;

    private String videoPath;

    private boolean readyToPlay;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;
    private float EPSILON = (float) 0.002;

    private SensorManager sensorManager;
    private WindowManager mWindowManager;

    private Sensor mRotationSensor ;
    private final float[] mRotationMatrix = new float[16];
    private long setPosition=0;
    private Surface mSurface;
    MainActivity parent;


    @Override

    public void onSensorChanged(SensorEvent sensorEvent) {

        if (renderThread == null) {
            return ;
        }

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);



        int worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        int worldAxisForDeviceAxisY = SensorManager.AXIS_Z;


        int screenRotation = mWindowManager.getDefaultDisplay().getRotation();
        if (screenRotation == Surface.ROTATION_0) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_X;
            worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
        } else if (screenRotation == Surface.ROTATION_90) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
        } else if (screenRotation == Surface.ROTATION_180) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
        } else if (screenRotation == Surface.ROTATION_270) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
            worldAxisForDeviceAxisY = SensorManager.AXIS_X;
        }



       /* int worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        int worldAxisForDeviceAxisY = SensorManager.AXIS_Y;

        int screenRotation = mWindowManager.getDefaultDisplay().getRotation();
        if (screenRotation == Surface.ROTATION_0) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_X;
            worldAxisForDeviceAxisY = SensorManager.AXIS_Y;
        } else if (screenRotation == Surface.ROTATION_90) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_Y;
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
        } else if (screenRotation == Surface.ROTATION_180) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Y;
        } else if (screenRotation == Surface.ROTATION_270) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Y;
            worldAxisForDeviceAxisY = SensorManager.AXIS_X;
        }*/

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix);

        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);

        // Convert radians to degrees
        float pitch = orientation[1] * -57;
        float roll = orientation[2] * -57;
        float yaw = orientation[0]*-57;

        //pitch = Math.abs(pitch);
        pitch+=90;
        yaw*=(-1);

       // Log.d("Debug_phi","Axis X "+pitch+" Roll "+roll + " yaw "+ yaw);

        Message msg = Message.obtain();
        msg.what = RenderThread.MSG_ON_GYRO_CHANGE;
        msg.obj = new GyroDeltaHolder(yaw, pitch);
        renderThread.handler.sendMessage(msg);

        /*if (sensorEvent.sensor == mRotationSensor) {
            if (sensorEvent.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(sensorEvent.values, 0, truncatedRotationVector, 0, 4);
                update(truncatedRotationVector);
            } else {
                update(event.values);
            }
        }*/


            // Axis of the rotation sample, not normalized yet.
            /*===float axisX = sensorEvent.values[0];
            float axisY = sensorEvent.values[1];
            float axisZ = sensorEvent.values[2];=====/
            //Log.d("Debug", "lon : " + axisX + "lat : " + axisY);

            /*====Message msg = Message.obtain();
            msg.what = RenderThread.MSG_ON_GYRO_CHANGE;
            msg.obj = new GyroDeltaHolder(axisX, axisY);
            renderThread.handler.sendMessage(msg);=====*/

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class GyroDeltaHolder {

        float deltaX, deltaY;

        GyroDeltaHolder(float dx, float dy) {
           deltaX = dx;
           deltaY = dy;

        }
    }

    private class ScrollDeltaHolder {
        float deltaX, deltaY;

        ScrollDeltaHolder(float dx, float dy) {
            deltaX = dx;
            deltaY = dy;
        }
    }

    //Simple gusture listner
    private SimpleOnGestureListener dragListener = new SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (renderThread == null) {
                return false;
            }

            Message msg = Message.obtain();
            msg.what = RenderThread.MSG_ON_SCROLL;
            msg.obj = new ScrollDeltaHolder(distanceX, distanceY);
            renderThread.handler.sendMessage(msg);
            return true;
        }

    };


    private GestureDetector gestureDetector;

    public SphericalVideoPlayer(Context context) {
        this(context, null);

        /*sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Matrix.setIdentityM(mRotationMatrix,0);*/

    }

    public SphericalVideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

        /*sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mRotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Matrix.setIdentityM(mRotationMatrix,0);*/
    }

    public SphericalVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gestureDetector = new GestureDetector(getContext(), dragListener);


        mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;
        mRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensorManager.registerListener(this, mRotationSensor, sensorManager.SENSOR_DELAY_GAME);

        //a
        //Matrix.setIdentityM(mRotationMatrix, 0);
        //sensorManager.registerListener(this, mRotationVectorSensor, 10000);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    public Surface initRenderThread(SurfaceTexture surface, int width, int height, long currentPosition, int frameHeight, int frameWidth, MainActivity main) {
        parent = main;
        setPosition = currentPosition;
        renderThread = new RenderThread(RENDER_THREAD_NAME);
        renderThread.start();
        Message msg = Message.obtain();
        msg.what = RenderThread.MSG_SURFACE_AVAILABLE;
        msg.obj = surface;
        msg.arg1 = width;
        msg.arg2 = height;
        renderThread.handler.sendMessage(msg);
        Log.d(TAG, "haritha - render thread initialized");
        //haritha - create the new surface here
        mSurface = renderThread.getVideoDecodeSurface(frameHeight, frameWidth);
        return mSurface;
    }


    public void setVideoURIPath(String path) {
        videoPath = path;
    }

    public void playWhenReady() {
        // Wait for render surface creation to start preparing the video.
        readyToPlay = true;
    }
/*
    private void prepareVideo(String videoPath) {
        if (renderThread == null) {
            throw new IllegalStateException("RenderThread has not been initialized");
        }

        if (TextUtils.isEmpty(videoPath)) {
            throw new RuntimeException("Cannot begin playback: video path is empty");
        }

        try {
            videoPlayerInternal = new MediaPlayer();
            videoPlayerInternal.setSurface(renderThread.getVideoDecodeSurface());
            Log.d(TAG, "haritha - initialized media player and created surface");
            videoPlayerInternal.setAudioStreamType(AudioManager.STREAM_MUSIC);
            videoPlayerInternal.setDataSource(getContext(), Uri.parse(videoPath), null);
            videoPlayerInternal.setLooping(false);

            toast(getContext(), "Preparing video...");

            videoPlayerInternal.setOnPreparedListener(
                    new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            toast(getContext(), "Prepared video");
                            play();
                        }
                    });
            videoPlayerInternal.setOnBufferingUpdateListener(
                    new MediaPlayer.OnBufferingUpdateListener() {
                        @Override
                        public void onBufferingUpdate(MediaPlayer mp, int percent) {
                            toast(getContext(), "Buffered video" + percent + "%");
                        }
                    });
            videoPlayerInternal.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
            toast(getContext(), e.toString());
        }
    }

    public void play() {
        if (!videoPlayerInternal.isPlaying()) {
            videoPlayerInternal.seekTo((int)setPosition);
            Log.d("Debug"," "+setPosition);
            videoPlayerInternal.start();
        }
    }*/

    public void releaseResources() {
        renderThread.handler.sendEmptyMessage(RenderThread.MSG_SURFACE_DESTROYED);
        sensorManager.unregisterListener(this);
    }

    public long getCurrentPositionInternalPlayer(){
        return videoPlayerInternal.getCurrentPosition();
    }

    public RenderThread getRenderThread() {
        return renderThread;
    }

    /**
     * RenderThread waits for the SphericalVideoPlayer's SurfaceTexture to be
     * available then sets up a GL rendering context, creates an external
     * texture for the video decoder to output to, asks for vsync updates from
     * the Choreographer, attaches a frame available listener the video decode
     * SurfaceTexture, then begins video playback.
     * <p>
     * Drag events from the main thread will be forwarded to the RenderThread's
     * message queue so that it may update the view state.
     * <p>
     * SphericalSceneRenderer draws the 360 video scene each frame using the
     * latest latched video texture frame.
     */
    private class RenderThread extends HandlerThread {
        private static final int MSG_SURFACE_AVAILABLE = 0x1;
        private static final int MSG_VSYNC = 0x2;
        private static final int MSG_FRAME_AVAILABLE = 0x3;
        private static final int MSG_SURFACE_DESTROYED = 0x4;
        private static final int MSG_ON_SCROLL = 0x5;
        private static final int MSG_ON_GYRO_CHANGE = 0x6;

        private static final int SENSOR_TOUCH = 0x7;
        private static final int SENSOR_GYRO = 0x8;

        private static final float FOVY = 60f;
        private static final float Z_NEAR = 1f;
        private static final float Z_FAR = 1000f;
        private static final float DRAG_FRICTION = 0.1f;
        private static final float INITIAL_PITCH_DEGREES = 90.f;

        private Handler handler;
        private Choreographer.FrameCallback frameCallback = new ChoreographerCallback();


        private EGLRenderTarget eglRenderTarget;
        private SurfaceTexture videoSurfaceTexture;
        private int videoDecodeTextureId = -1;
        private int sensorType = SENSOR_TOUCH;

        private float[] videoTextureMatrix = new float[16];
        private float[] modelMatrix = new float[16];
        private float[] viewMatrix = new float[16];
        private float[] projectionMatrix = new float[16];
        private float[] rotationMatrix = new float[16];

        private float[] camera = new float[3];

        private float lon;
        private float lat;
        private float tempTheta;
        private float tempPhi;
        private boolean halfFPS = true;
        private int focusCount = 0;


        private boolean frameAvailable;
        private boolean pendingCameraUpdate;

        private SphericalSceneRenderer renderer;

        /**Sync the frame display and the rendering interface. This always tirggered with any call
         * back from the display refreshment and this is the firing point to start rendering the frame*/
        private class ChoreographerCallback implements Choreographer.FrameCallback {
            @Override
            public void doFrame(long frameTimeNanos) {
                Log.d(TAG, "haritha - Vsync msg sent");
                handler.sendEmptyMessage(MSG_VSYNC);
            }
        }

        public RenderThread(String name) {
            super(name);
            eglRenderTarget = new EGLRenderTarget();
        }

        @Override
        public synchronized void start() {
            super.start();
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_SURFACE_AVAILABLE:
                            Log.d(TAG, "haritha - surface available");
                            onSurfaceAvailable((SurfaceTexture) msg.obj, msg.arg1, msg.arg2);
                            break;
                        case MSG_VSYNC:
                            onVSync();
                            break;
                        case MSG_FRAME_AVAILABLE:
                            Log.d(TAG, "haritha - frame available message received");
                            onFrameAvailable();
                            break;
                        case MSG_SURFACE_DESTROYED:
                            onSurfaceDestroyed();
                            break;
                        case MSG_ON_SCROLL:
                            onScroll((ScrollDeltaHolder) msg.obj);
                            break;
                        case MSG_ON_GYRO_CHANGE:
                            onViewChange((GyroDeltaHolder) msg.obj);
                            break;

                    }
                }
            };
        }

        private Surface getVideoDecodeSurface(int frameHeight, int frameWidth) {
            if (!eglRenderTarget.hasValidContext()) {
                throw new IllegalStateException(
                        "Cannot get video decode surface without GL context");
            }

            videoDecodeTextureId = GLHelpers.generateExternalTexture();
            videoSurfaceTexture = new SurfaceTexture(videoDecodeTextureId);
            videoSurfaceTexture.setDefaultBufferSize(frameWidth, frameHeight);

            videoSurfaceTexture.setOnFrameAvailableListener(
                    new SurfaceTexture.OnFrameAvailableListener() {
                        @Override
                        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                            Log.d(TAG, "haritha - new frame avialble message sent");
                            handler.sendEmptyMessage(RenderThread.MSG_FRAME_AVAILABLE);
                        }
                    });
            return new Surface(videoSurfaceTexture);
        }

        private void onSurfaceAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Log.d(TAG, "haritha - onSurfaceAvailable w: " + width + " h: " + height);
            //width = 2009;
            //height = 897;
            /**Creating a render surface, window surface to render the objects. */
            eglRenderTarget.createRenderSurface(surfaceTexture);

            /**OPENGL ES do the rendering in a separate thread. Thre reason to use this is to
             * synchronize the frame refreshing rate of the display with the rendering interface.
             * But how are they connected????*/
            Choreographer.getInstance().postFrameCallback(frameCallback);


            /**Specifying the lower left corner to the viewport rectangle, in pixel */
            GLES20.glViewport(0, 0, width, height);
            GLHelpers.checkGlError("glViewport");

            float aspectRatio = (float) width / height;

            /**Define the projection matrix*/
            Matrix.perspectiveM(projectionMatrix, 0, FOVY, aspectRatio, Z_NEAR, Z_FAR);
            Matrix.setIdentityM(viewMatrix, 0);
            // Apply initial rotation
            Matrix.setRotateM(modelMatrix, 0, INITIAL_PITCH_DEGREES, 1, 0, 0);

            GLES20.glClearColor(1.0f, 0.f, 0.f, 1.f);

            renderer = new SphericalSceneRenderer(getContext());
            /*
            if (readyToPlay) {
                prepareVideo(videoPath);
            }*/
        }

        public void onVSync() {
            if (!eglRenderTarget.hasValidContext()) {
                return;
            }

            Log.d(TAG, "sync - vsync triggered");
            Choreographer.getInstance().postFrameCallback(frameCallback);
            Log.d(TAG, "haritha - frameAvailable is "+frameAvailable + " and cameraupdate is "+ pendingCameraUpdate);

            // We only redraw when there's a new video frame or if a drag event happened.
            if (!frameAvailable && !pendingCameraUpdate) {
                Log.d(TAG, "haritha - no updates, returning, frameAvailable is "+frameAvailable + " and cameraupdate is "+ pendingCameraUpdate);
                return;
            }

            eglRenderTarget.makeCurrent();
            // Have to be sure to balance onFrameAvailable and updateTexImage calls so that
            // the internal queue buffers will be freed. For this example, we can rely on the
            // display refresh rate being higher or equal to the video frame rate (sample is 30fps).
            videoSurfaceTexture.updateTexImage();
            videoSurfaceTexture.getTransformMatrix(videoTextureMatrix);
            //Log.d(TAG, "haritha - matrix"+videoTextureMatrix[0]+videoTextureMatrix[1]+videoTextureMatrix[3]+videoTextureMatrix[5]+videoTextureMatrix[7]+videoTextureMatrix[9]+videoTextureMatrix[11]+videoTextureMatrix[13]+videoTextureMatrix[15]);

            updateCamera();

            renderer.onDrawFrame(
                    videoDecodeTextureId,
                    videoTextureMatrix,
                    modelMatrix,
                    viewMatrix,
                    projectionMatrix,
                    rotationMatrix);

            eglRenderTarget.swapBuffers();

            if (frameAvailable) {
                frameAvailable = false;
            }

            if (pendingCameraUpdate) {
                pendingCameraUpdate = false;
            }
            //change from 60fps to 30fps
            if (halfFPS){
                halfFPS = false;
                parent.setRender();
            }
            else{
                halfFPS = true;
            }
            focusCount++;
            if(focusCount ==400){
                //parent.setFocus(2.3, 3,3);
                Log.d(TAG, "gyro - focus modified");
            }
        }

        private void updateCamera() {
            lat = Math.max(-85, Math.min(85, lat));
            float phi;
            float theta;

            switch (sensorType) {
                /*case (SENSOR_TOUCH):
                    //Log.d("Debug_phi","Lon:   "+lon+"Lat:  "+lat);
                    phi = (float) Math.toRadians(90 - lat);
                    theta = (float) Math.toRadians(lon);
                    Log.d("Debug_phi"," "+phi+"   Debug_phi "+theta);
                    camera[0] = (float) (100.f * Math.sin(phi) * Math.cos(theta));
                    camera[1] = (float) (100.f * Math.cos(phi));
                    camera[2] = (float) (100.f * Math.sin(phi) * Math.sin(theta));
                    break;*/

                case(SENSOR_GYRO):

                    phi = (float) (((180-tempPhi)*3.14)/180.0);
                    theta = (float) ((tempTheta*3.14)/180.0);
                    //Log.d(TAG, "angles - phi "+ phi + " theta " + theta);
                    camera[0] = (float) (100.f * Math.sin(phi) * Math.cos(theta));
                    camera[1] = (float) (100.f * Math.cos(phi));
                    camera[2] = (float) (100.f * Math.sin(phi) * Math.sin(theta));
                    parent.setFocus(phi, theta);

            }

            /*phi = (float) Math.toRadians(90 - lat);
            theta = (float) Math.toRadians(lon);
            camera[0] = (float) (100.f * Math.sin(phi) * Math.cos(theta));
            camera[1] = (float) (100.f * Math.cos(phi));
            camera[2] = (float) (100.f * Math.sin(phi) * Math.sin(theta));
*/

            Matrix.setLookAtM(
                    viewMatrix, 0,
                    camera[0], camera[1], camera[2],
                    0, 0, 0,
                    0, 1, 0
            );
        }

        private void onFrameAvailable() {
            frameAvailable = true;
        }

        private void onSurfaceDestroyed() {
            if (videoPlayerInternal != null) {
                videoPlayerInternal.stop();
                videoPlayerInternal.release();
                videoPlayerInternal = null;
            }

            if (videoDecodeTextureId != -1) {
                int[] textures = new int[1];
                textures[0] = videoDecodeTextureId;
                GLES20.glDeleteTextures(1, textures, 0);
                videoDecodeTextureId = -1;
            }

            if (videoSurfaceTexture != null) {
                videoSurfaceTexture.release();
                videoSurfaceTexture = null;
                frameAvailable = false;
            }

            pendingCameraUpdate = false;

            eglRenderTarget.release();
            renderer.release();
        }

        private void onScroll(ScrollDeltaHolder deltaHolder) {
            lon = (deltaHolder.deltaX) * DRAG_FRICTION + lon;
            lat = -(deltaHolder.deltaY) * DRAG_FRICTION + lat;
            pendingCameraUpdate = true;
            sensorType = SENSOR_TOUCH;
        }

        private void onViewChange(GyroDeltaHolder deltaHolder) {
            tempTheta = deltaHolder.deltaX;
            tempPhi= deltaHolder.deltaY;
            pendingCameraUpdate = true;
            sensorType = SENSOR_GYRO;
        }

    }

}
