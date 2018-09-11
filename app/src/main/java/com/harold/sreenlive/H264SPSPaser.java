package com.harold.sreenlive;

import android.util.Log;

public class H264SPSPaser {
    private static final String TAG = "H264SPSPaser";
    private static int startBit = 0;

    public  static void parse(byte[] sps){

        int nStartBit = 0;

        Log.d(TAG, "SPS: flag:" + H264SPSPaser.u(sps, 8, nStartBit));
        nStartBit +=8;

        short profile_idc = H264SPSPaser.u(sps, 8, nStartBit);
        Log.d(TAG, "SPS: profile_idc:" + profile_idc);
        nStartBit +=8;

        Log.d(TAG, "SPS: constrained_set0_flag:" + H264SPSPaser.u(sps, 1, nStartBit));
        nStartBit+=1;
        Log.d(TAG, "SPS: constrained_set1_flag:" + H264SPSPaser.u(sps, 1, nStartBit));
        nStartBit+=1;
        Log.d(TAG, "SPS: constrained_set2_flag:" + H264SPSPaser.u(sps, 1, nStartBit));
        nStartBit+=1;
        Log.d(TAG, "SPS: constrained_set3_flag:" + H264SPSPaser.u(sps, 1, nStartBit));
        nStartBit+=1;
        Log.d(TAG, "SPS: reserved:" + H264SPSPaser.u(sps, 4, nStartBit));
        nStartBit+=4;

        Log.d(TAG, "SPS: level_idc:" + H264SPSPaser.u(sps, 8, nStartBit));
        nStartBit+=8;

        Log.d(TAG, "SPS: seq_parameter_set_id:" + H264SPSPaser.ue(sps, nStartBit));
        //nStartBit+=8;

        if(profile_idc==100){//还有110 122等等

            short chroma_format_idc = H264SPSPaser.ue(sps, startBit);
            Log.d(TAG, "SPS: chroma_format_idc:" + chroma_format_idc);

            if(chroma_format_idc==3){
                Log.d(TAG, "SPS: separate_colour_plane_flag:" + H264SPSPaser.u(sps, 1, startBit));
                startBit += 1;
                Log.d(TAG, "\t\tstartBit:"+startBit);
            }

            Log.d(TAG, "SPS: bit_depth_luma_minus8:" +  H264SPSPaser.ue(sps, startBit));
            Log.d(TAG, "SPS: bit_depth_chroma_minus8:" +  H264SPSPaser.ue(sps, startBit));
            Log.d(TAG, "SPS: qqprime_y_zero_transform_bypass_flag:" + H264SPSPaser.u(sps, 1, startBit));
            startBit += 1;
            Log.d(TAG, "\t\tstartBit:"+startBit);
            Log.d(TAG, "SPS: seq_scaling_matrix_present_flag:" + H264SPSPaser.u(sps, 1, startBit));
            startBit += 1;
            Log.d(TAG, "\t\tstartBit:"+startBit);
        }

        Log.d(TAG, "SPS: log2_max_frame_num_minus4:" + H264SPSPaser.ue(sps, startBit));
        //Log.d(TAG, "startBit:"+startBit);

        short pic_order_cnt_type = H264SPSPaser.ue(sps, startBit);
        Log.d(TAG, "SPS: pic_order_cnt_type:" + pic_order_cnt_type);
        if(pic_order_cnt_type==0){
            Log.d(TAG, "SPS: log2_max_pic_order_cnt_lsb_minus4:" + H264SPSPaser.ue(sps, startBit));
        }else if(pic_order_cnt_type==1){
            //...
        }
        //Log.d(TAG, "startBit:"+startBit);

        Log.d(TAG, "SPS: max_num_ref_frames:" + H264SPSPaser.ue(sps, startBit));
        //Log.d(TAG, "startBit:"+startBit);

        Log.d(TAG, "SPS: gaps_in_frame_num_value_allowed_flag:" + H264SPSPaser.u(sps, 1, startBit));
        startBit += 1;
        Log.d(TAG, "startBit:"+startBit);

        Log.d(TAG, "SPS: pic_width_in_mbs_minus1:" + H264SPSPaser.ue(sps, startBit));
        //Log.d(TAG, "startBit:"+startBit);

        Log.d(TAG, "SPS: pic_height_in_mbs_minus1:" + H264SPSPaser.ue(sps, startBit));
        //Log.d(TAG, "startBit:"+startBit);

        short frame_mbs_only_flag = H264SPSPaser.u(sps, 1, startBit);
        Log.d(TAG, "SPS: frame_mbs_only_flag:" + frame_mbs_only_flag);
        startBit += 1;
        Log.d(TAG, "\t\tstartBit:"+startBit);
        if(frame_mbs_only_flag==0){
            Log.d(TAG, "SPS: mb_adaptive_frame_field_flag:" + H264SPSPaser.u(sps, 1, startBit));
            startBit += 1;
            Log.d(TAG, "\t\tstartBit:"+startBit);
        }

        Log.d(TAG, "SPS: direct_8x8_inference_flag:" + H264SPSPaser.u(sps, 1, startBit));
        startBit += 1;
        Log.d(TAG, "\t\tstartBit:"+startBit);

        short frame_croppig_flag =  H264SPSPaser.u(sps, 1, startBit);
        Log.d(TAG, "SPS: frame_croppig_flag:" + frame_croppig_flag);
        startBit += 1;
        Log.d(TAG, "\t\tstartBit:"+startBit);

        if(frame_croppig_flag==1){
            Log.d(TAG, "SPS: frame_cropping_rect_left_offset:" + H264SPSPaser.ue(sps, startBit));
            Log.d(TAG, "SPS: frame_cropping_rect_right_offset:" + H264SPSPaser.ue(sps, startBit));
            Log.d(TAG, "SPS: frame_cropping_rect_top_offset:" + H264SPSPaser.ue(sps, startBit));
            Log.d(TAG, "SPS: frame_cropping_rect_bottom_offset:" + H264SPSPaser.ue(sps, startBit));
        }

        Log.d(TAG, "SPS: vui_parameters_present_flag:" + H264SPSPaser.u(sps, 1, startBit));
        startBit += 1;
        Log.d(TAG, "\t\tstartBit:"+startBit);

        //int width = (H264SPSPaser.ue(sps,34) + 1)*16;
        //int height = (H264SPSPaser.ue(sps,-1) + 1)*16;
        //Log.d(TAG, "width:"+width+" height:"+height);
    }

    /*
     * 从数据流data中第StartBit位开始读，读bitCnt位，以无符号整形返回
     */
    public static short u(byte[] data,int bitCnt,int StartBit){
        short ret = 0;
        int start = StartBit;
        for(int i = 0;i < bitCnt;i++){
            ret<<=1;
            /*Log.d(TAG, "u:~~");
            Log.d(TAG, ""+start/8 );
            Log.d(TAG, ""+data[start / 8] );
            Log.d(TAG, ""+(0x80 >> (start%8)) );
            Log.d(TAG, ""+(data[start / 8] & (0x80 >> (start%8))));*/
            if ((data[start / 8] & (0x80 >> (start%8))) != 0)
            {
                ret += 1;
            }
            start++;
        }
        return ret;
    }
    /*
     * 无符号指数哥伦布编码
     * leadingZeroBits = −1;
     * for( b = 0; !b; leadingZeroBits++ )
     *    b = read_bits( 1 )
     * 变量codeNum 按照如下方式赋值：
     * codeNum = 2^leadingZeroBits − 1 + read_bits( leadingZeroBits )
     * 这里read_bits( leadingZeroBits )的返回值使用高位在先的二进制无符号整数表示。
     */
    public static short ue(byte[] data,int StartBit){
        short ret = 0;
        int leadingZeroBits = -1;
        int tempStartBit = (StartBit == -1)?startBit:StartBit;//如果传入-1，那么就用上次记录的静态变量
        for( int b = 0; b != 1; leadingZeroBits++ ){//读到第一个不为0的数，计算前面0的个数
            b = u(data,1,tempStartBit++);
        }
        Log.d(TAG,"\t\tue leadingZeroBits = " + leadingZeroBits + ",Math.pow(2, leadingZeroBits) = " + Math.pow(2, leadingZeroBits) + ",tempStartBit = " + tempStartBit);
        ret = (short) (Math.pow(2, leadingZeroBits) - 1 + u(data,leadingZeroBits,tempStartBit));
        startBit = tempStartBit + leadingZeroBits;
        Log.d(TAG,"\t\tue startBit = " + startBit);
        return ret;
    }
    /*
     * 有符号指数哥伦布编码
     * 9.1.1 有符号指数哥伦布编码的映射过程
     *按照9.1节规定，本过程的输入是codeNum。
     *本过程的输出是se(v)的值。
     *表9-3中给出了分配给codeNum的语法元素值的规则，语法元素值按照绝对值的升序排列，负值按照其绝对
     *值参与排列，但列在绝对值相等的正值之后。
     *表 9-3－有符号指数哥伦布编码语法元素se(v)值与codeNum的对应
     *codeNum 语法元素值
     *	0 		0
     *	1		1
     *	2		−1
     *	3		2
     *	4		−2
     *	5		3
     *	6		−3
     *	k		(−1)^(k+1) Ceil( k÷2 )
     */
    public static int se(byte[] data,int StartBit){
        int ret = 0;
        short codeNum = ue(data,StartBit);
        ret = (int) (Math.pow(-1, codeNum + 1)*Math.ceil(codeNum/2));
        return ret;
    }
}