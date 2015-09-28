package com.danielkim.soundrecorder.activities;import android.app.Activity;import android.content.Context;import android.content.Intent;import android.hardware.Camera;import android.media.MediaPlayer;import android.os.Bundle;import android.os.Environment;import android.util.Log;import android.view.SurfaceHolder;import android.view.SurfaceView;import android.view.View;import android.view.WindowManager;import android.widget.Button;import android.widget.FrameLayout;import android.widget.ImageButton;import android.widget.Toast;import com.danielkim.soundrecorder.R;import java.io.IOException;public class CameraActivity extends Activity {    private Camera mCamera;    private CameraPreview mPreview;    String newString;    private String mFilePath = null;    private MediaPlayer mMediaPlayer = null;    private static final String LOG_TAG = "PlaybackFragment";    @Override    public void onCreate(Bundle savedInstanceState) {        super.onCreate(savedInstanceState);        setContentView(R.layout.activity_camera);        mCamera = getCameraInstance();        Intent iin = getIntent();        Bundle b = iin.getExtras();        newString = (String) b.get("filename");        mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();        mFilePath += "/SoundRecorder/" + newString;        final Camera.PictureCallback mPicture = new Camera.PictureCallback() {            @Override            public void onPictureTaken(byte[] data, Camera camera) {                Intent intent = new Intent(CameraActivity.this, EditActivity.class);                intent.putExtra(EditActivity.EXTRA_BYTES, data);                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);                startActivity(intent);            }        };        // Create the Camera preview view        mPreview = new CameraPreview(this, mCamera);        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);        preview.addView(mPreview);        // Add a listener to the capture button        ImageButton captureButton = (ImageButton) findViewById(R.id.button_capture);        captureButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_camera_icon));        captureButton.setOnClickListener(                new View.OnClickListener() {                    @Override                    public void onClick(View v) {                        mCamera.takePicture(null, null, mPicture);                    }                }        );        Button soundBiteButton = (Button) findViewById(R.id.button2);        soundBiteButton.setOnClickListener(new View.OnClickListener() {            @Override            public void onClick(View v) {                startPlaying();            }        });    }    private void startPlaying() {        mMediaPlayer = new MediaPlayer();        try {            mMediaPlayer.setDataSource(mFilePath);            mMediaPlayer.prepare();            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {                @Override                public void onPrepared(MediaPlayer mp) {                    mMediaPlayer.start();                }            });        } catch (IOException e) {            Log.e(LOG_TAG, "prepare() failed");        }    }    @Override    protected void onPause() {        super.onPause();        finish();        mPreview.stopPreview();    }    /**     * A safe way to get an instance of the Camera object.     *     * Returns null if camera is unavailable.     */    private static Camera getCameraInstance() {        Camera c = null;        try {            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);        } catch (Exception ignore) {            // Camera is not available (in use or does not exist). Do nothing.        }        return c;    }    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {        private SurfaceHolder mHolder;        private Camera mCamera;        public CameraPreview(Context context, Camera camera) {            super(context);            mCamera = camera;            // Install a SurfaceHolder.Callback so we get notified when the            // underlying surface is created and destroyed.            mHolder = getHolder();            mHolder.addCallback(this);            // deprecated setting, but required on Android versions prior to 3.0            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);        }        public void surfaceCreated(SurfaceHolder holder) {            // The Surface has been created, now tell the camera where to draw the preview.            try {                if (mCamera != null) {                    mCamera.setDisplayOrientation(90);                    mCamera.setPreviewDisplay(holder);                    mCamera.startPreview();                }            } catch (IOException ignore) {                // Do nothing            }        }        public void surfaceDestroyed(SurfaceHolder holder) {            if (mCamera != null) {                mCamera.stopPreview();            }        }        private void stopPreview() {            if (mCamera != null) {                mCamera.stopPreview();                mCamera.release();                mCamera = null;            }        }        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {            // If your preview can change or rotate, take care of those events here.            // Make sure to stop the preview before resizing or reformatting it.            if (mHolder.getSurface() == null) {                // preview surface does not exist                return;            }            // Stop preview before making changes            try {                mCamera.stopPreview();            } catch (Exception ignore) {                // Tried to stop a non-existent preview. Do nothing.            }            // Set preview size and make any resize, rotate or reformatting changes here            // Start preview with new settings            try {                mCamera.setPreviewDisplay(mHolder);                mCamera.startPreview();            } catch (Exception ignore) {                // Do nothing            }        }    }}