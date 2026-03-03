package com.example.testkey;

import java.io.IOException;

import android.os.Bundle;
import android.os.RemoteException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.KeyEvent;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import hdx.HdxUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

 

//import android.os.ILedService;
//import android.os.ServiceManager;
import android.os.IBinder;
//import android.os//.SystemProperties;
import android.text.TextUtils;

import com.example.extern_led8_display_for_sc200_gc111.R;

import java.util.Random;
import android.os.Build;
import android.graphics.Bitmap;
import android.telephony.TelephonyManager;
import android.widget.ImageView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.util.Log;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import android.graphics.Color;
import android.widget.TextView;
import android.os.Handler;

@SuppressLint("NewApi")
public class MainActivity extends Activity {

	 Object proxy = null;
	 Method ledCtrl = null;
	 IHelloService svr;
  Button  button5_camera_led;
	 int flag=1;
    int flag_led=2;
	 final static String TAG="TestKey";
    private static final String TAG_QR = "MainActivity";
    private ImageView qrCodeImageView;
    private TextView qrContentTextView;
    private String deviceInfo = "";
    private static final int CHECK_INTERVAL = 500; // 500ms
    private Handler mHandler = new Handler();
    private boolean isChecking = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "test_led8__version_4");

        // Initialize views
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        qrContentTextView = findViewById(R.id.qrContentTextView);
        
        // Start checking device info
        startDeviceInfoCheck();

        // Camera LED button
        button5_camera_led = (Button) findViewById(R.id.button5_camera_led);
        button5_camera_led.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "button5_camera_led()2 ");
                HdxUtil.SetCameraBacklightness(flag_led);
                if (flag_led == 2) {
                    flag_led = 3;
                } else {
                    flag_led = 2;
                }
            }
        });

        // Button2 - Show 8888
        Button ButtonCodeDemo2 = (Button) findViewById(R.id.button2);
        ButtonCodeDemo2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                byte data[] = new byte[]{'8','.', '8','.', '8','.', '8'};
                HdxUtil.SetLed8Display(data);
            }
        });

        // Button3 - LED8 Test
        final Button ButtonCodeDemo3 = (Button) findViewById(R.id.button3);
        ButtonCodeDemo3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "ButtonCodeDemo3 flag:" + flag);
                byte data[] = new byte[]{indexddd, indexddd, indexddd, indexddd};
                HdxUtil.SetLed8Display(data);
                ButtonCodeDemo3.setText("" + indexddd + "" + indexddd + "" + indexddd + "" + indexddd);
                indexddd++;
                if (indexddd == 10) {
                    indexddd = 1;
                }
            }
        });

        // SIM card button
        Button button4_sim = (Button) findViewById(R.id.button4_sim);
        button4_sim.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "button4_sim()2 ");
                HdxUtil.SwitchSimCard(flag);
                if (flag == 1) {
                    flag = 0;
                } else {
                    flag = 1;
                }
            }
        });
    }
	byte indexddd=0;
	
	void test_hide()
	{
		   try {
		       //iLedService = ILedService.Stub.asInterface(ServiceManager.getService("led"));
		       Method getService = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
		       Object ledService = getService.invoke(null, "hide handler hdx");
		       IBinder binder=(IBinder)ledService;
		       svr = IHelloService.Stub.asInterface(binder);
		       /*
		       Method asInterface = Class.forName("android.os.ILedService$Stub").getMethod("asInterface", IBinder.class);
		       proxy = asInterface.invoke(null, ledService);
		       ledCtrl = Class.forName("android.os.ILedService$Stub$Proxy").getMethod("ledCtrl", int.class, int.class);
		       ledCtrl.invoke(proxy, 2, 1);*/
		       
		   } catch (NoSuchMethodException e) {
		       e.printStackTrace();
		   } catch (ClassNotFoundException e) {
		       e.printStackTrace();
		   } catch (IllegalAccessException e) {
		       e.printStackTrace();
		   } catch (InvocationTargetException e) {
		       e.printStackTrace();
		   }		
		
		
	
	}
	
	
	@SuppressLint("WrongConstant")
	void test()
	{
		
		WindowManager manager = this.getWindowManager();
		DisplayMetrics outMetrics = new DisplayMetrics();
		manager.getDefaultDisplay().getMetrics(outMetrics);
		int width2 = outMetrics.widthPixels;
		int height2 = outMetrics.heightPixels;

		
		 DisplayMetrics metric = new DisplayMetrics();
	        getWindowManager().getDefaultDisplay().getMetrics(metric);
	        int width = metric.widthPixels;  // 灞忓箷瀹藉害锛堝儚绱狅級
	        int height = metric.heightPixels;  // 灞忓箷楂樺害锛堝儚绱狅級
	        float density = metric.density;  // 灞忓箷瀵嗗害锛�0.75 / 1.0 / 1.5锛�
	        int densityDpi = metric.densityDpi;  // 灞忓箷瀵嗗害DPI锛�120 / 160 / 240锛�
		
		
		
		Log.e("",  width+"---"+height);
		
		Log.e("",   width2+"---"+height2);
		Log.e("",   density+"---"+densityDpi);
		String cmd = "bctest" ;
		
		Log.e(cmd, "cmd====================="+cmd);
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		Toast.makeText(MainActivity.this," lcd width:"+ width+"lcd height:"+height+"  version:v2", 0).show();
		Toast.makeText(MainActivity.this, width2+"---"+height2, 0).show();
		Toast.makeText(MainActivity.this, density+"---"+densityDpi, 0).show();	

		
	}
	
	
	
	TestMine edit =new TestMine();
	TestMine set  =new TestMine();
 

	
	
	class TestMine
	{
		
		void setText(String str)
		{
			Log.e("testkey", str);
			
		}
		void remove(int event)
		{
			
			Log.e("remove", ""+event);
		}
	}
	@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction()==KeyEvent.ACTION_DOWN) {
            edit.setText("");
            final Resources res = getResources();
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_F1:
                    set.remove(KeyEvent.KEYCODE_F1);
                    edit.setText("F1");
                    break;
                case KeyEvent.KEYCODE_F2:
                    set.remove(KeyEvent.KEYCODE_F2);
                    edit.setText("F2");
                    break;
                case KeyEvent.KEYCODE_F3:
                    set.remove(KeyEvent.KEYCODE_F3);
                    edit.setText("F3");
                    break;
                case KeyEvent.KEYCODE_F12:
                    edit.setText("res.getString(R.string.scan_left)");
                    break;
                case KeyEvent.KEYCODE_F11:
                    edit.setText("res.getString(R.string.scan)");
                    break;
                case KeyEvent.KEYCODE_CALL:
                    edit.setText("res.getString(R.string.call)");
                    break;
                case KeyEvent.KEYCODE_ENDCALL:
                    edit.setText("res.getString(R.string.end_call)");
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    edit.setText("res.getString(R.string.up)");
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    edit.setText("res.getString(R.string.down)");
                    break;
                case KeyEvent.KEYCODE_ENTER:
                    edit.setText("res.getString(R.string.enter)");
                    break;
                case KeyEvent.KEYCODE_HOME:
                    edit.setText("res.getString(R.string.home)");
                    break;
                case KeyEvent.KEYCODE_DEL:
                    edit.setText("res.getString(R.string.del)");
                    break;
                case KeyEvent.KEYCODE_0:
                    edit.setText("0");
                    break;
                case KeyEvent.KEYCODE_1:
                    edit.setText("1");
                    break;
                case KeyEvent.KEYCODE_2:
                    edit.setText("2");
                    break;
                case KeyEvent.KEYCODE_3:
                    edit.setText("3");
                    break;
                case KeyEvent.KEYCODE_4:
                    edit.setText("4");
                    break;
                case KeyEvent.KEYCODE_5:
                    edit.setText("5");
                    break;
                case KeyEvent.KEYCODE_6:
                    edit.setText("6");
                    break;
                case KeyEvent.KEYCODE_7:
                    edit.setText("7");
                    break;
                case KeyEvent.KEYCODE_8:
                    edit.setText("8");
                    break;
                case KeyEvent.KEYCODE_9:
                    edit.setText("9");
                    break;
                case KeyEvent.KEYCODE_POUND:
                    edit.setText("#");
                    break;
                case KeyEvent.KEYCODE_STAR:
                    edit.setText("*");
                    break;
                case KeyEvent.KEYCODE_PERIOD:
                    edit.setText(".");
                    break;
                case KeyEvent.KEYCODE_BACK:
                    edit.setText("res.getString(R.string.back)");
                    return super.dispatchKeyEvent(event);
                    //break;
                case KeyEvent.KEYCODE_ESCAPE:
                    edit.setText("res.getString(R.string.esc)");
                    break;
                case KeyEvent.KEYCODE_POWER:
                    edit.setText("res.getString(R.string.power)");
                    break;
                case KeyEvent.KEYCODE_F10:
                    edit.setText("fn");
                    break;
                case KeyEvent.KEYCODE_F9:
                    edit.setText("res.getString(R.string.letter)");
                    break;
                case KeyEvent.KEYCODE_CAMERA:
                    edit.setText("res.getString(R.string.camera)");
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    edit.setText("res.getString(R.string.vol_dec)");
                    break;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    edit.setText("res.getString(R.string.vol_inc)");
                    break;
                default:
                    break;
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }	

    public static void shutDown() {
    	
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                	Log.d(TAG,"shutDown");
                    //获得ServiceManager类
                    Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
                    //获得ServiceManager的getService方法
                    Method getService = ServiceManager.getMethod("getService", String.class);
                    //调用getService获取RemoteService
                    Object oRemoteService = getService.invoke(null, Context.POWER_SERVICE);
                    //获得IPowerManager.Stub类
                    Class<?> cStub = Class.forName("android.os.IPowerManager$Stub");
                    //获得asInterface方法
                    Method asInterface = cStub.getMethod("asInterface", IBinder.class);
                    //调用asInterface方法获取IPowerManager对象
                    Object oIPowerManager = asInterface.invoke(null, oRemoteService);
                    //获得并调用shutdown()方法
                    Method shutdown;
                    //String model = DeviceUtils.getModel();
                   // if (model.equals(Constant.MODEL_QD21_WX) || model.equals(Constant.MODEL_QD21_L)) {
                        shutdown = oIPowerManager.getClass().getMethod("shutdown", boolean.class, String.class, boolean.class);
                        shutdown.invoke(oIPowerManager, false, "", true);
                   // } else {
                    //    shutdown = oIPowerManager.getClass().getMethod("shutdown", boolean.class, boolean.class);
                     //   shutdown.invoke(oIPowerManager, false, true);
                  //  }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
	static UsbDevice getUsbDevice(UsbManager um) {
		
		HashMap<String, UsbDevice> lst = um.getDeviceList();
		
		Iterator<UsbDevice> deviceIterator = lst.values().iterator();
		while (deviceIterator.hasNext()) {
			UsbDevice dev = (UsbDevice) deviceIterator.next();
			
			Log.d(TAG, "usb device : " + String.format("%1$04X:%2$04X", dev.getVendorId(), dev.getProductId()));

			
			if (!um.hasPermission(dev)) {
				 
				Log.d(TAG,"hav not  permission ");	
				 
			}
			else
			Log.d(TAG,"hav permission ");	
			
		}
		
		return null;
	}

    @SuppressLint("HardwareIds")
    private void generateQRCode() {
        StringBuilder info = new StringBuilder();
        StringBuilder info2 = new StringBuilder();

        // Get serial number from system property
        String serialNumber = getSystemProperty("ro.serialno", "");
        info.append("SN: ").append(serialNumber).append("\n");
        info2.append("SN: ").append(serialNumber).append(",");

        // Get IMEIs from system property
        String imeisStr = getSystemProperty("persist.apr.imeis", "");
        if (!TextUtils.isEmpty(imeisStr)) {
            String[] imeis = imeisStr.split(",");
            if (imeis.length > 0) {
                String imei1 = imeis[0];
                info.append("IMEI1: ").append(imei1).append("\n");
                info2.append("IMEI1: ").append(imei1).append(",");
                
                if (imeis.length > 1) {
                    String imei2 = imeis[1].split("@")[0]; // Remove @sync suffix
                    info.append("IMEI2: ").append(imei2);
                    info2.append("IMEI2: ").append(imei2);
                }
            }
        }
        
        deviceInfo = info.toString();
        
        // Parse the deviceInfo to check if valid
        String[] lines = deviceInfo.split("\n");
        String imei1 = "", imei2 = "";
        
        for (String line : lines) {
            if (line.startsWith("IMEI1:")) {
                imei1 = line.substring(6).trim();
            } else if (line.startsWith("IMEI2:")) {
                imei2 = line.substring(6).trim();
            }
        }
        deviceInfo= info2.toString()+"\n";
        // Check if both IMEIs are valid
        if (!TextUtils.isEmpty(imei1) && imei1.length() > 5 
            && !TextUtils.isEmpty(imei2) && imei2.length() > 5) {
            // Success - show info and QR code
            qrContentTextView.setText( "@"+info.toString());
            createQRCode(deviceInfo);
            qrCodeImageView.setVisibility(View.VISIBLE);
        } else {
            // Failed - show "getting" message and hide QR code
            qrContentTextView.setText("正在获取...");
            qrCodeImageView.setVisibility(View.INVISIBLE);
        }
    }

    private String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting system property: " + e.getMessage());
            return defaultValue;
        }
    }

    private void createQRCode(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            qrCodeImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e(TAG_QR, "Error generating QR code: " + e.getMessage());
        }
    }

    private void startDeviceInfoCheck() {
        if (isChecking) {
            return;
        }
        isChecking = true;
        qrContentTextView.setText("正在获取...");
        checkDeviceInfo();
    }

    private final Runnable mCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkDeviceInfo();
        }
    };

    private void checkDeviceInfo() {
        // Get device info using existing reflection method
        generateQRCode();
        
        // Parse the deviceInfo to check if valid
        String[] lines = deviceInfo.split("\n");
        String imei1 = "", imei2 = "";
        
        for (String line : lines) {
            if (line.startsWith("IMEI1:")) {
                imei1 = line.substring(6).trim();
            } else if (line.startsWith("IMEI2:")) {
                imei2 = line.substring(6).trim();
            }
        }
        
        // Check if both IMEIs are valid
        if (!TextUtils.isEmpty(imei1) && imei1.length() > 5 
            && !TextUtils.isEmpty(imei2) && imei2.length() > 5) {
            // Success - stop checking
            isChecking = false;
        } else {
            // Not successful yet - schedule next check
            mHandler.postDelayed(mCheckRunnable, CHECK_INTERVAL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mCheckRunnable);
        isChecking = false;
    }

}
