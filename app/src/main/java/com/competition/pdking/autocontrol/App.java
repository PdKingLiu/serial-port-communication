package com.competition.pdking.autocontrol;

import android.app.Application;

import android_serialport_api.SerialPort;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liupeidong
 * Created on 2019/5/8 11:21
 */
public class App extends Application {

    private Map<String, SerialPort> serialPortMap = new HashMap<String, SerialPort>();  //串口列表

    public SerialPort getSerialPort(String path) throws SecurityException,  //打开并读取串口状态参数
            IOException, InvalidParameterException {
        return getSerialPort(path, 115200);
    }

    public SerialPort getSerialPort(String path, int baudrate)     //打开串口并返回串口参数
            throws SecurityException, IOException, InvalidParameterException {
        System.out.println("MyApplication启动");
        SerialPort mSerialPort = serialPortMap.get(path);
        if (mSerialPort == null) {
            /* Check parameters检查参数 */
            if ((path.length() == 0) || (baudrate == -1)) {
                throw new InvalidParameterException();
            }
            /* Open the serial port打开串口 使用指定的端口名、波特率和奇偶校验位初始化 */
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
            serialPortMap.put(path, mSerialPort);
        }
        return mSerialPort;

    }
}
