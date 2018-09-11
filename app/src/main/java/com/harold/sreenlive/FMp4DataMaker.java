package com.harold.sreenlive;

import android.media.MediaCodec;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;

public class FMp4DataMaker {

    private boolean bFirstFrame=true;

    private long firstFrameDTS = 0;
    private long firstFramePTS = 0;

    private long preFrameDTS = 0;
    private int totalDuration=0;

    private JSONObject lastFrame;

    public static String  generateMiniCodeC(ByteBuffer formatCsd0){
        String mimeCodec = "video/mp4; codecs=avc1.";

        //formatCsd0 跳过0x00 0x00 0x00 0x01 就是SPS
        formatCsd0.position(5);
        mimeCodec += Integer.toHexString(formatCsd0.get() & 0xFF);
        mimeCodec += Integer.toHexString(formatCsd0.get() & 0xFF);
        mimeCodec += Integer.toHexString(formatCsd0.get() & 0xFF);
        return mimeCodec;
    }

    public static ByteBuffer generateAvccData(ByteBuffer formatCsd0, ByteBuffer formatCsd1){
        ByteBuffer buffer = ByteBuffer.allocate(3+ formatCsd0.remaining() + formatCsd1.remaining());

        buffer.put((byte)0x01);//configurationVersion
        buffer.put(formatCsd0.get(4+1));//AVCProfileIndication SPS[1]
        //buffer.put((byte)0x0);//profile_compatibility
        buffer.put(formatCsd0.get(4+2));//profile_compatibility
        buffer.put(formatCsd0.get(4+3));//AVCLevelIndication SPS[3]
        buffer.put((byte)0xff);//lengthSizeMinusOne:前面6位为reserved，后面2位（0b11）为：lengthSizeMinusOne，表示3，那么用来表示size的字节就有3+1=4个

        buffer.put((byte)0xe1);//numOfSequenceParameterSets:前面3位是reserved,后面5bit是numOfSequenceParameterSets，表示有1个
        formatCsd0.position(4);//跳过0x00 0x00 0x00 0x01 就是SPS
        buffer.putShort((short)formatCsd0.remaining());
        buffer.put(formatCsd0);

        buffer.put((byte)0x1);//1个PPS
        formatCsd1.position(4);//跳过0x00 0x00 0x00 0x01 就是PPS
        buffer.putShort((short)formatCsd1.remaining());
        buffer.put(formatCsd1);


        formatCsd0.flip();
        formatCsd1.flip();
        buffer.flip();
        return buffer;
    }

    public JSONObject generateVideoInitData(int width, int height, int timescale, String avcC){
    //public JSONObject generateVideoInitData(){

        JSONObject object = new JSONObject();
        try{
            object.put("cmd", "init");
            object.put("id", 1);
            object.put("type", "video");;
            object.put("codecWidth", width);
            object.put("codecHeight", height);
            object.put("duration",this.totalDuration);
            object.put("timescale", timescale);

            object.put("presentWidth", width);
            object.put("presentHeight", height);

            object.put("avcc",avcC);
        }catch (Exception e){
            e.printStackTrace();
        }
        return object;
    }

    public long getTotalDuration(){
        return System.currentTimeMillis() - this.firstFrameDTS;
    }

    public JSONObject generateVideoFrameData(byte[] frameData, MediaCodec.BufferInfo bufferInfo){

        JSONObject object = new JSONObject();
        boolean bKeyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;

        long dts = 0;
        long pts = 0;
        long duration = 0;
        long curMilliTime = System.currentTimeMillis();
        if(bKeyFrame && this.bFirstFrame){//第一个I帧
            this.bFirstFrame = false;
            this.firstFrameDTS = curMilliTime;
            this.preFrameDTS = curMilliTime;
            this.firstFramePTS = bufferInfo.presentationTimeUs/1000;
        }else{//B帧或者P帧
            dts = curMilliTime - this.firstFrameDTS;
            pts = bufferInfo.presentationTimeUs/1000 - this.firstFramePTS;

            this.totalDuration = (int)dts;
            duration = curMilliTime - this.preFrameDTS;
            this.preFrameDTS = curMilliTime;
        }

        try{
            object.put("cmd", "media");
            //object.put("dts", dts);
            object.put("dts", pts);
            object.put("pts", pts);
            //object.put("cts", pts-dts);
            object.put("cts", 0);
            object.put("duration", 0);//占位方便调试查看
            object.put("isKeyFrame",bKeyFrame);
            object.put("originalDts", dts);
            object.put("size",frameData.length);
            //object.put("size",byteBuffer.remaining());

            //byte[] byteArr = new byte[byteBuffer.remaining()];
            //byteBuffer.get(byteArr,0,byteArr.length);
            object.put("units",new String(Base64.encodeToString(frameData, Base64.NO_WRAP)));

            JSONObject objectFlag = new JSONObject();
            objectFlag.put("isLeading", 0);
            objectFlag.put("dependsOn", bKeyFrame ? 2 : 1);
            objectFlag.put("isDependedOn",  bKeyFrame ? 1 : 0); ;
            objectFlag.put("hasRedundancy", 0); ;
            objectFlag.put("isNonSync", bKeyFrame ? 1 : 0);
            object.put("flags", objectFlag);

            /*JSONArray jsonArr = new JSONArray();
            JSONObject object1 = new JSONObject();
            JSONObject object2 = new JSONObject();
            object1.put("dts",100);
            object1.put("pts",100);
            object2.put("dts",100);
            object2.put("pts",100);
            jsonArr.put(object1);
            jsonArr.put(object2);
            object.put("frames",jsonArr);*/
        }catch (Exception e){
            e.printStackTrace();
        }

        JSONObject result;
        if(bFirstFrame){
            result =  new JSONObject();
        }else{
            result = this.lastFrame;
            try {
                result.put("duration", duration);
            }catch(Exception e){
            }
        }
        this.lastFrame = object;
        return result;
    }

    public void generateAudioInitData(){
        //TODO:
    }

    public void generateAudioFrameData(){
        //TODO:
    }
}
