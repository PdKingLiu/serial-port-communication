package com.competition.pdking.autocontrol;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android_serialport_api.SerialPort;

public class MainActivity extends AppCompatActivity {

    private App myApplication;
    private SerialPort serialPort;
    private InputStream inputStreamChuanKou;//串口输入流
    private OutputStream outputStreamChuanKou;//串口输出流
    private InputStream inputStreamLight;//串口输入流
    private TextView tvT;
    private TextView tvLight;
    private TextView tvWet;
    private String TAG = "Lpp";
    private List<Socket> sockets;

    private TextView tvLightIsOpen;
    private TextView tvFanIsOpen;

    private ImageView ivLightIsOpen;
    private ImageView ivFanIsOpen;

    private ServerSocket serverSocket;

    private boolean lightIsOpen = false;
    private boolean fanIsOpen = false;

    private boolean isFirstFan = true;
    private boolean isFirstLight = true;

    private int TMax = 28;
    private int lightMin = 1700;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {   //对接收到的数据进行解析处理
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message msg) {
            byte[] data = msg.getData().getByteArray("data");
            if (data[3] == (byte) 0x03 && data[4] == (byte) 0x01) {    //接收到串口上传来的温湿度数据
                byte byteXH = data[5];
                byte byteXL = data[6];
                byte byteYH = data[7];
                byte byteYL = data[8];
                tvT.setText(String.valueOf((byteXH * 256 + byteXL) / 100));
                tvWet.setText(String.valueOf((byteYH * 256 + byteYL) / 100));
                int T = Integer.parseInt(String.valueOf((byteXH * 256 + byteXL) / 100));
                Log.d(TAG, "T: " + T);
                if (isFirstFan) {
                    if (T >= TMax) {
                        fanIsOpen = true;
                        openFan(fanIsOpen);
                        tvFanIsOpen.setText("已打开");
                        ivFanIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_open));
                    } else {
                        fanIsOpen = false;
                        openFan(fanIsOpen);
                        tvFanIsOpen.setText("已关闭");
                        ivFanIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_close));
                    }
                    isFirstFan = false;
                } else {
                    if (T >= TMax) {
                        if (!fanIsOpen) {
                            fanIsOpen = true;
                            openFan(fanIsOpen);
                            tvFanIsOpen.setText("已打开");
                            ivFanIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_open));
                        }
                    } else {
                        if (fanIsOpen) {
                            fanIsOpen = false;
                            openFan(fanIsOpen);
                            tvFanIsOpen.setText("已关闭");
                            ivFanIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_close));
                        }
                    }
                }
            }
            if (data[3] == (byte) 0x02 && data[4] == (byte) 0x01) {
                byte byteXH = data[5];
                byte byteXL = data[6];
                tvLight.setText(String.valueOf((byteXH * 256 + byteXL)));
                int light = Integer.parseInt(String.valueOf((byteXH * 256 + byteXL)));
                Log.d(TAG, "light: " + light);
                if (isFirstLight) {
                    if (light <= lightMin) {
                        lightIsOpen = true;
                        openLight(lightIsOpen);
                        tvLightIsOpen.setText("已打开");
                        ivLightIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_open));
                    } else {
                        lightIsOpen = false;
                        openLight(lightIsOpen);
                        tvLightIsOpen.setText("已关闭");
                        ivLightIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_close));
                    }
                    isFirstLight = false;
                } else {
                    if (light <= lightMin) {
                        if (!lightIsOpen) {
                            lightIsOpen = true;
                            openLight(lightIsOpen);
                            tvLightIsOpen.setText("已打开");
                            ivLightIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_open));
                        }
                    } else {
                        if (lightIsOpen) {
                            lightIsOpen = false;
                            openLight(lightIsOpen);
                            tvLightIsOpen.setText("已关闭");
                            ivLightIsOpen.setImageDrawable(getDrawable(R.mipmap.icon_close));
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitleMode(getWindow());
        tvT = findViewById(R.id.tv_wd);
        tvLight = findViewById(R.id.tv_light);
        tvWet = findViewById(R.id.tv_sd);
        tvFanIsOpen = findViewById(R.id.tv_fan_is_open);
        tvLightIsOpen = findViewById(R.id.tv_light_is_open);
        ivFanIsOpen = findViewById(R.id.iv_fan_is_open);
        ivLightIsOpen = findViewById(R.id.iv_light_is_open);
        myApplication = (App) getApplication();
        sockets = new ArrayList<>();
        initT();
        initLight();
    }

    private void initLight() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(7777);
                    while (true) {
                        Socket socket = serverSocket.accept();
                        Log.d(TAG, "while (true): " + "addSocket");
                        sockets.add(socket);
                        if (socket.isConnected()) {
                            inputStreamLight = socket.getInputStream();
                            loadData(socket);
                        }
                        Log.d("Lpp", "initLight socket.isConnected(): " + socket.isConnected());
                    }
                } catch (IOException e) {
                    Log.d("Lpp", "initLight e.getMessage(): " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void loadData(final Socket socket) {
        Log.d("Lpp", "loadData: ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Lpp", "run: ");
                int len = 0;
                byte[] date = new byte[16];
                try {
                    while ((len = inputStreamLight.read(date)) != -1) {
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putByteArray("data", date);
                        message.setData(bundle);
                        handler.sendMessage(message);
                        Log.d("Lpp", "loadData date.toString(): " + toHexString(date));
                    }
                } catch (Exception e) {
                    Log.d("Lpp", "loadData e.getMessage(): " + e.getMessage());
                } finally {
                    try {
                        if (socket != null) {
                            if (!socket.isInputShutdown()) {
                                socket.shutdownInput();
                            }
                            if (!socket.isOutputShutdown()) {
                                socket.shutdownOutput();
                            }
                            if (!socket.isClosed()) {
                                socket.close();
                            }
                        }
                        if (serverSocket != null) {
                            if (!serverSocket.isClosed()) {
                                if (serverSocket != null && !serverSocket.isClosed()) {
                                    serverSocket.close();
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void initT() {
        try {
            serialPort = myApplication.getSerialPort("/dev/ttyAMA5");
            inputStreamChuanKou = serialPort.getInputStream();
            outputStreamChuanKou = serialPort.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {   //创建串口通信进程
                byte[] date = new byte[16];
                int len = -1;
                try {
                    while ((len = inputStreamChuanKou.read(date)) != -1) {
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putByteArray("data", date);
                        message.setData(bundle);
                        handler.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void openFan(boolean isOpen) {   //控制继电器
        if (isOpen) {
            byte[] cmdoff = {(byte) 0xCC, (byte) 0xEE, (byte) 0x01, (byte) 0x18,
                    (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF};
            try {
                outputStreamChuanKou.write(cmdoff);
                outputStreamChuanKou.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            byte[] cmdoff = {(byte) 0xCC, (byte) 0xEE, (byte) 0x01, (byte) 0x18,
                    (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF};
            try {
                outputStreamChuanKou.write(cmdoff);
                outputStreamChuanKou.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //16进制转String
    static final byte[] HEX_TABLE = {0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB,
            0xC, 0xD, 0xE, 0xF};
    static final char[] HEX_CHAR_TABLE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
            'B', 'C', 'D', 'E', 'F'};

    public static String toHexString(byte[] data) throws Exception {
        if (data == null || data.length == 0) return null;
        byte[] hex = new byte[data.length * 2];
        int index = 0;
        for (byte b : data) {
            int v = b & 0xFF;
            hex[index++] = (byte) HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = (byte) HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex);
    }

    @Override
    protected void onDestroy() {
        try {
            for (Socket socket : sockets) {
                if (socket != null) {
                    if (!socket.isInputShutdown()) {
                        socket.shutdownInput();
                    }
                    if (!socket.isOutputShutdown()) {
                        socket.shutdownOutput();
                    }
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                }
            }
            if (serverSocket != null) {
                if (!serverSocket.isClosed()) {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    String operation = "";

    public void openLight(boolean isOpen) {
        byte[] cmd = {(byte) 0xCC, (byte) 0xEE, (byte) 0x01, (byte) 0x09,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF};
        if (operation.equalsIgnoreCase("1")) {// 红灯开
            cmd[4] = (byte) 0x01;
        } else if (operation.equalsIgnoreCase("2")) {// 红灯关
            cmd[4] = (byte) 0x02;
        } else if (operation.equalsIgnoreCase("3")) {// 黄灯开
            cmd[4] = (byte) 0x03;
        } else if (operation.equalsIgnoreCase("4")) {// 黄灯关
            cmd[4] = (byte) 0x04;
        } else if (operation.equalsIgnoreCase("5")) {// 绿灯开
            cmd[4] = (byte) 0x05;
        } else if (operation.equalsIgnoreCase("6")) {// 绿灯关
            cmd[4] = (byte) 0x06;
        } else if (operation.equalsIgnoreCase("7")) {// 白灯开
            cmd[4] = (byte) 0x07;
        } else if (operation.equalsIgnoreCase("8")) {// 白灯关
            cmd[4] = (byte) 0x08;
        } else if (operation.equalsIgnoreCase("9")) {// 全部灯开
            cmd[4] = (byte) 0x0c;
        } else if (operation.equalsIgnoreCase("10")) {// 全部灯关
            cmd[4] = (byte) 0x0d;
        }
        if (isOpen) {
            cmd[4] = (byte) 0x0c;
        } else {
            cmd[4] = (byte) 0x0d;
        }
        for (Socket socket : sockets) {
            send(cmd, socket);
        }
    }

    public void send(byte[] bytes, Socket socket) {     //发送数据函数，供主程序调用
        try {
            if (socket != null) {
                OutputStream outputStream = socket.getOutputStream();
                Log.d(TAG, "send: " + outputStream);
                if (outputStream != null) {
                    outputStream.write(bytes);
                    outputStream.flush();
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "send: " + e.getMessage());
        }
    }


    public static void setTitleMode(Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = window.getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
