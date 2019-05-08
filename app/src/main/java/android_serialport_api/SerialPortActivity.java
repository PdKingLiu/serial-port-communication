/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public abstract class SerialPortActivity {

	protected SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;

	private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while (!isInterrupted()) {
				try {
					byte[] buffer = new byte[512];
					if (mInputStream == null)
						return;
					// size = mInputStream.read(buffer);
					int readCount = 0; // 已经成功读取的字节的个数
					readCount += mInputStream.read(buffer, readCount,
							512 - readCount);
					// Log.e("收到的数据", readCount+"");
					if (readCount > 0) {
						onDataReceived(buffer, readCount);
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	protected void openSerial(File device, int baudate) {
		try {
			mSerialPort = getSerialPort(device.getAbsoluteFile(), baudate);
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
			mReadThread = new ReadThread();
			mReadThread.start();
		} catch (SecurityException e) {
			return;
		} catch (IOException e) {
			return;
		} catch (InvalidParameterException e) {
			return;
		}
	}

	protected void closeRead() {
		if (mReadThread != null) {
			mReadThread.interrupt();
		}
	}

	protected SerialPort getSerialPort(File device, int baud)
			throws SecurityException, IOException, InvalidParameterException {

		mSerialPort = null;
		String path;
		int baudrate;

		if (mSerialPort == null) {
			/* Read serial port parameters */
			path = device.getAbsolutePath();
			baudrate = baud;
			/* Check parameters */
			if ((path.length() == 0) || (baudrate == -1)) {
				throw new InvalidParameterException();
			}

			/* Open the serial port */
			mSerialPort = new SerialPort(new File(path), baudrate, 0);
			Log.v("debug", device.getAbsolutePath());
		}
		return mSerialPort;
	}

	protected void closeSerialPort() throws IOException {
		closeRead();
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
		if (mInputStream != null) {
			mInputStream.close();
		}
	}

	protected abstract void onDataReceived(final byte[] buffer, final int size);
}
