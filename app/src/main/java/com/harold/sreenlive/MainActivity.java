package com.harold.sreenlive;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MEDIA_PROJECTION_CODE = 1;
    private static final int REQUEST_PERMISIONS = 2;

    private static final String LOG_TAG = "MainActivity";

    private Button mBtn1;
    private boolean mbRecording = false;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private MediaCodec mEncoder;
    private Surface mSurface;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private MediaMuxer mMediaMuxer;
    private int mVideoTrackIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
        mMediaProjectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);

        mBtn1 = (Button)findViewById(R.id.button1);
        mBtn1.setOnClickListener(onClickListenerTakePhoto);
    }

    private View.OnClickListener onClickListenerTakePhoto = new View.OnClickListener(){

        public void onClick(View v){
            if (mbRecording==false){
                startScreenRecord();
            }else{
                stopScreenRecord();
            }
        }
    };

    private void startScreenRecord(){
        Log.d(LOG_TAG, "startScreenRecord-----------------------------------");
        mbRecording = true;
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION_CODE);
        mBtn1.setText("停止录屏");
    }

    private void stopScreenRecord() {
        Log.d(LOG_TAG, "stopScreenRecord-----------------------------------");
        mbRecording = false;
        mBtn1.setText("开始录屏");

        if(mVirtualDisplay != null){
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if(mEncoder != null){
            mEncoder.release();
            mEncoder = null;
        }

        if(mMediaProjection != null){
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if(mMediaMuxer != null){
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    private void requestPermissions(){
        String[] permissions =  new String[]{
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISIONS);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        Log.d(LOG_TAG, "onRequestPermissionsResult---------------------------------");
        if(requestCode == REQUEST_PERMISIONS){
            for(int i=0; i < permissions.length; i++ ){
                 Log.d(LOG_TAG, "申请的权限为"+permissions[i]+"的结果为"+grantResults[i]);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_MEDIA_PROJECTION_CODE && resultCode == RESULT_OK){
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode,data);
            if(null == mMediaProjection){
                Log.e(LOG_TAG, "MediaProjection object is null");
                return;
            }
            Log.d(LOG_TAG, "MediaProjection object is ok");

            File path = getSaveDir();
            Log.d(LOG_TAG, "output file path is :"+path.getAbsolutePath());
            if(!path.exists() && !path.mkdirs()){
                Log.e(LOG_TAG, "dir not exist!");
                return;
            }

            initEncoder();
            initMediaMuxer(path.getAbsolutePath()+"/1.mp4");

            startCapture();
        }
    }

    protected void startCapture(){
        //DisplayMetrics metrics = new DisplayMetrics();
        //getWindowManager().getDefaultDisplay()
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "MainScreen",
                600,
                800,
                 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mSurface,
                null,
                null
                );

        Log.d(LOG_TAG, "create Virtual Display");

       /* while (true){
            try {
                Thread.sleep(1000);
            }catch(Exception e){

            }

            //int eobIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 10000);
            //Log.d(LOG_TAG, Long.toString(eobIndex));
        }*/
    }

    protected void initEncoder(){
        try {

            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 600, 800);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 300*1000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

            mEncoder = MediaCodec.createEncoderByType("video/avc");
            mEncoder.setCallback(mCodecCallback);//must before configure
            mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            mSurface = mEncoder.createInputSurface();
            mEncoder.start();
            Log.d(LOG_TAG, "MediaCodec is started");

        }catch (IOException e){
            Log.e(LOG_TAG, "no media codec found!");
        }
    }

    private MediaCodec.Callback mCodecCallback = new MediaCodec.Callback(){

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            Log.d(LOG_TAG, "onInputBufferAvailable");
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            Log.d(LOG_TAG, "onOutputBufferAvailable " + index);
            muxVideo(index, info);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            Log.d(LOG_TAG, "onOutputFormatChanged" + format);
            mVideoTrackIndex = mMediaMuxer.addTrack(format);
            mMediaMuxer.start();
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.d(LOG_TAG, "onError");
         }
    };

    private void muxVideo(int index, MediaCodec.BufferInfo buffer){
        ByteBuffer encodedVideo = mEncoder.getOutputBuffer(index);

        //ByteBuffer outBuffer = mEncoder.getOutputBuffer(index);
        //MediaFormat bufferFormat = mEncoder.getOutputFormat(index);

        mMediaMuxer.writeSampleData(mVideoTrackIndex, encodedVideo, buffer);
        Log.d(LOG_TAG, "muxer write data"+ buffer + encodedVideo);

        mEncoder.releaseOutputBuffer(index, false);

    }

    private void initMediaMuxer(String filePath){
        try {
            mMediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }catch(Exception e){
            Log.e(LOG_TAG, "media muxer not created!", e);
        }
    }

    protected File getSaveDir(){
        return  new File(Environment.getExternalStorageDirectory(), "ScreenLive");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
