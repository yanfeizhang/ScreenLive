package com.harold.sreenlive;

import android.util.Base64;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;

public class Util {
    /*
    *  获取当前设备的ip地址
     */
    public static String getIpAddressString() {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                    .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netI
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    /*
     *  将bytebuffer进行base64编码，以方便传输
     */
    public static String base64Encode(ByteBuffer byteBuffer) {

        byte[] byteArr = new byte[byteBuffer.remaining()];
        byteBuffer.get(byteArr,0,byteArr.length);

        byteBuffer.flip();

        return Base64.encodeToString(byteArr, Base64.NO_WRAP);
    }
    /*
     *  打印ByteBuffer，用来调试
     */
    public static int printByteBuffer(ByteBuffer buf) {
        //buf.flip();
        int len=0;
        String str="";
        while(buf.hasRemaining()){
            str = str + " " + Integer.toHexString(buf.get() & 0xFF);
            len++;
        }

        buf.flip();
        Log.d("Util-printByteBuffer", str);
        return len;
    }

    /*
     *  打印Byte数组，用来调试
     */
    public static void printByteArray(byte[] buf) {
        Log.d("Util-printByteArray", Arrays.toString(buf));
    }


    /*
     * int 转换为byte数组
     */
    public static byte[] intToByteArray(int a){
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /*
     * 根据字符串初始化byte数组：目前用户输入外部SPS供分析使用
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
