package com.danielkim.soundrecorder.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.danielkim.soundrecorder.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class EditActivity extends ActionBarActivity {

    public static final String EXTRA_BYTES = "BMP";

    private static final int MAX_NUMBER_OF_FACES = 10;
    private static final float GLASSES_SCALE_CONSTANT = 2.5f;
    private static final float HAT_SCALE_CONSTANT = 1.5f;
    private static final float HAT_OFFSET = 2.5f;
    private static final float TIE_SCALE_CONSTANT = 1f;
    private static final float TIE_OFFSET = 2.2f;

    private int NUMBER_OF_FACE_DETECTED;

    private FaceDetector.Face[] detectedFaces;

    private Bitmap mBitmap;
    private Bitmap mSticker;
    private Bitmap mOriginalBitmap;
    private ImageView mImageView;
    private ImageButton mUndoButton;
    private ArrayList<StickerPointF> mStickersAdded = new ArrayList<StickerPointF>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        byte[] bytes = getIntent().getByteArrayExtra(EXTRA_BYTES);
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inPreferredConfig = Bitmap.Config.RGB_565;
        bfo.inScaled = false;
        bfo.inDither = false;
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bfo);

        // Rotate the bitmap
        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0,
                bmp.getWidth(), bmp.getHeight(), matrix, true);
        bmp.recycle();

        int width = rotatedBitmap.getWidth();
        int height = rotatedBitmap.getHeight();
        detectedFaces = new FaceDetector.Face[MAX_NUMBER_OF_FACES];
        FaceDetector faceDetector = new FaceDetector(width, height, MAX_NUMBER_OF_FACES);
        NUMBER_OF_FACE_DETECTED = faceDetector.findFaces(rotatedBitmap, detectedFaces);


        mBitmap = rotatedBitmap;
        mImageView = (ImageView) findViewById(R.id.imageView1);
        mImageView.setImageDrawable(new BitmapDrawable(mBitmap));

        // Setup the sticker

        // Setup the undo button
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        mImageView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                float x = event.getX();
                                float y = event.getY();

                                // If the user has selected a sticker, draw it on the picture
                                if (mSticker != null) {
                                    if (mStickersAdded.size() == 0) {
                                        // Save the old bitmap in case user decides to undo
                                        mOriginalBitmap = mBitmap.copy(mBitmap.getConfig(), true);
                                    }
                                    drawStickerOnBitmap(mSticker, x, y);
                                    mStickersAdded.add(new StickerPointF(mSticker, x, y));
                                }
                                break;
                            default:
                                break;
                        }
                        return false;
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_save:
                File newDir = new File(Environment.getExternalStorageDirectory(), "PetPics");
                newDir.mkdirs();
                Random gen = new Random();
                int n = 10000;
                n = gen.nextInt(n);
                String fotoname = "Photo-"+ n +".jpg";
                File file = new File (newDir, fotoname);
                if (file.exists ()) file.delete ();
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                    out.close();

                    Toast.makeText(getApplicationContext(), "Saved to PetPics in the Gallery", Toast.LENGTH_LONG).show();
                    getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                } catch (Exception e) {

                }
                break;

            case R.id.menu_item_share:
                ByteArrayOutputStream bytesArray = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytesArray);
                File f = new File(Environment.getExternalStorageDirectory()
                        + File.separator + "temporary_file.jpg");
                try {
                    f.createNewFile();
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(bytesArray.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                startActivity(Intent.createChooser(shareIntent,
                        getResources().getText(R.string.send_to)));
                return true;
            default:
        }
                return super.onOptionsItemSelected(item);

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * Draws sticker at specific point on the image
     */
    private void drawStickerOnBitmap(Bitmap sticker, float x, float y) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        Canvas canvas = new Canvas(mBitmap);

        x = (x / mImageView.getWidth()) * canvas.getWidth();
        y = (y / mImageView.getHeight()) * canvas.getHeight();

        canvas.drawBitmap(sticker, x - sticker.getWidth() / 2,
                y - sticker.getHeight() / 2, paint);
        mImageView.invalidate();
        mUndoButton.setVisibility(View.VISIBLE);
    }

    /**
     * Creates a button for each sticker the user can select to decorate the picture
     */

    /**
     * Helper method to scale each object (hat, glasses, tie) to the size of the face
     */
    private Bitmap scaleObjectToFace(FaceDetector.Face face, Bitmap object, float scaleConstant) {
        float newWidth = face.eyesDistance() * scaleConstant;
        float scaleFactor = newWidth / object.getWidth();
        return Bitmap.createScaledBitmap(object, Math.round(newWidth),
                Math.round(object.getHeight() * scaleFactor), false);
    }

    /**
     * Method iterates through the faces and decorates each with
     * a properly sized and placed hat, glasses, and tie
     */


    private class StickerPointF {
        public Bitmap sticker;
        public float x;
        public float y;

        private StickerPointF(Bitmap sticker, float x, float y){
            this.x = x;
            this.y = y;
            this.sticker = sticker;
        }
    }
}