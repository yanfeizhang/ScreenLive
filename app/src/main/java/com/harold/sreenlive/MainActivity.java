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
import android.provider.MediaStore;
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MEDIA_PROJECTION_CODE = 1;
    private static final int REQUEST_PERMISIONS = 2;

    private static final String LOG_TAG = "MainActivity";

    private Button mBtn1;
    private Button mBtn2;
    private boolean mbRecording = false;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private int width;
    private int height;
    private int bitRate;
    private int frameRate;
    private int iFrameInterval;

    private MediaCodec mEncoder;
    private String mAvcc="";
    private byte[] mSpsPps;

    private Surface mSurface;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private MediaMuxer mMediaMuxer;
    private int mVideoTrackIndex;

    private  FMp4DataMaker mFMp4DataMaker;
    private JSONArray videoFrameDatas;

    private SimpleServerThread wsServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.width = 360;
        this.height = 480;
        this.bitRate = 300*1000;
        this.frameRate = 5;
        this.iFrameInterval = 1;

        requestPermissions();
        mMediaProjectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);

        mBtn1 = (Button)findViewById(R.id.button1);
        mBtn1.setOnClickListener(onClickListenerTakePhoto);

        mBtn2 = (Button)findViewById(R.id.button2);
        mBtn2.setOnClickListener(onClickListenerSendVideo);

        //this.mUtil = new Util();
        this.mFMp4DataMaker = new FMp4DataMaker();
        this.videoFrameDatas = new JSONArray();
        //String str = this.mFMp4DataMaker.generateVideoInitData();
        //Log.d(LOG_TAG, str);

        this.wsServer = new SimpleServerThread();
        this.wsServer.start();
    }

    private View.OnClickListener onClickListenerTakePhoto = new View.OnClickListener(){

        public void onClick(View v){
            if (mbRecording==false){
                startScreenRecord();
            }else{
                stopScreenRecord();
                sendData();
            }
        }
    };

    private View.OnClickListener onClickListenerSendVideo = new View.OnClickListener(){

        public void onClick(View v){
                sendData();
        }
    };

    private void startScreenRecord(){
        Log.d(LOG_TAG, "startScreenRecord-----------------------------------");
        mbRecording = true;
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION_CODE);
        mBtn1.setText("停止录屏");
    }

    private void sendData(){
        Log.d(LOG_TAG, "sendData:");

        //把init数据打到发送线程里
        JSONObject obj=this.mFMp4DataMaker.generateVideoInitData(this.width,this.height,1000,mAvcc);
        this.wsServer.data = obj.toString();
        //Log.d(LOG_TAG, "video init data:"+obj.toString());

        try{
            Thread.sleep(100);
        }catch(Exception e){

        }

        //把frame信息打到发送线程里
        this.wsServer.data = videoFrameDatas.toString();
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
                this.width,
                this.height,
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
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", this.width, this.height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, this.bitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, this.frameRate);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, this.iFrameInterval);


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
            Log.d(LOG_TAG, "onOutputBufferAvailable- muxVideo " + index);

            if(null!=mEncoder){
                muxVideo(index, info);
            }
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            Log.d(LOG_TAG, "onOutputFormatChanged" + format);
            mVideoTrackIndex = mMediaMuxer.addTrack(format);
            Log.d(LOG_TAG, "media format:"+format.toString());

            ByteBuffer formatCsd0 = format.getByteBuffer("csd-0");
            ByteBuffer formatCsd1 = format.getByteBuffer("csd-1");
            /*Log.d("formatCsd0:", ""+formatCsd0.remaining());
            Util.printByteBuffer(formatCsd0);
            Log.d("formatCsd1", ""+formatCsd1.remaining());
            Util.printByteBuffer(formatCsd1);*/

            //Parse SPS
            /*formatCsd0.position(4);
            byte[] sps = new byte[formatCsd0.remaining()];
            formatCsd0.get(sps);
            Log.d(LOG_TAG, "sps:");
            Util.printByteArray(sps);
            formatCsd0.flip();

            byte[] okSps = Util.hexStringToByteArray("67640015acd94170f797c044000003000400000300103c58b658");
            Util.printByteArray(okSps);
            H264SPSPaser.parse(okSps);*/

            ByteBuffer avcC = mFMp4DataMaker.generateAvccData(formatCsd0, formatCsd1);
            mAvcc = Util.base64Encode(avcC);

            Log.d("avcC", ""+avcC.remaining());
            Util.printByteBuffer(avcC);

            Log.d("mAvcc", mAvcc);

            String mimeCodec = FMp4DataMaker.generateMiniCodeC(formatCsd0);
            Log.d("mimeCodec", ""+mimeCodec);

            mMediaMuxer.start();
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.d(LOG_TAG, "onError");
         }
    };

    private void muxVideo(int index, MediaCodec.BufferInfo buffer){
        ByteBuffer encodedVideo = mEncoder.getOutputBuffer(index);

        boolean bConfigFrame = (buffer.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) !=0;
        if(bConfigFrame) {//SPS,PPS

            //记录配置信息，供fpm4 的header使用
            mSpsPps = new byte[buffer.size];
            encodedVideo.get(mSpsPps);

            Log.d(LOG_TAG, "bConfigFrame~~~~");
            Util.printByteArray(mSpsPps);
            Util.printByteBuffer(encodedVideo);
            //把init数据打到发送线程里
            //JSONObject obj=this.mFMp4DataMaker.generateVideoInitData(this.width,this.height,3000,1000,mAvcc);
            //this.wsServer.data = obj.toString();
            //Log.d(LOG_TAG, "video init data:"+obj.toString());
        }else{
            boolean bKeyFrame = (buffer.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;

            byte[] frameData = new byte[buffer.size];
            encodedVideo.get(frameData);

            /*if(bKeyFrame){
                byte[] keyFrameData = new byte[mSpsPps.length+frameData.length];
                System.arraycopy(mSpsPps,0,keyFrameData,0,mSpsPps.length);
                System.arraycopy(frameData,0,keyFrameData,mSpsPps.length,frameData.length);
                frameData=keyFrameData;
            }*/

            Log.d(LOG_TAG, "frameData.length:"+frameData.length);
            byte[] frameLen = Util.intToByteArray(frameData.length-4);
            //Util.printByteArray(frameLen);

            //Util.printByteArray(frameData);
            frameData[0] = frameLen[0];
            frameData[1] = frameLen[1];
            frameData[2] = frameLen[2];
            frameData[3] = frameLen[3];
            Util.printByteArray(frameData);
            JSONObject obj=this.mFMp4DataMaker.generateVideoFrameData(frameData, buffer);
            //Util.printByteBuffer(encodedVideo);

            //把数据打到发送线程里
            //if(bKeyFrame && this.videoFrameDatas.length()>0){
                //this.wsServer.data = videoFrameDatas.toString();
                //Log.d(LOG_TAG, "video init data:"+videoFrameDatas.toString());
            //}

            if(null ==obj) {
                Log.d(LOG_TAG,"skip first~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            }else{
                this.videoFrameDatas.put(obj);
                Log.d(LOG_TAG,"saved~~~~~~~~~~");
                Log.d(LOG_TAG, "video frame data:"+obj.toString());
            }
        }

        //Log.d(LOG_TAG, "flag:"+buffer.flags+" offset:"+buffer.flags+" timeus:"+(buffer.presentationTimeUs/1000)+" size:"+buffer.size);
        //Log.d(LOG_TAG, encodedVideo)
        //ByteBuffer outBuffer = mEncoder.getOutputBuffer(index);
        //MediaFormat bufferFormat = mEncoder.getOutputFormat(index);

        mMediaMuxer.writeSampleData(mVideoTrackIndex, encodedVideo, buffer);
       // Log.d(LOG_TAG, "muxer write data");
        //Log.d(LOG_TAG, "encodedVideo:"+ encodedVideo);
        //Log.d(LOG_TAG, "buffer:"+ buffer);

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
