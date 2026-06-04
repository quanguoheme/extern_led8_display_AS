package com.example.testkey;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import hdx.HdxUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

 

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

import static java.lang.Thread.sleep;

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
   // private ImageView qrCodeImageView;
    //private TextView qrContentTextView;
    private String deviceInfo = "";
    private static final int CHECK_INTERVAL = 500; // 500ms
    private static final int REQUEST_READ_PHONE_STATE = 1001;
    private static final int SIM_CHECK_INTERVAL = 1000;
    private static final int SIM_CHECK_TIMEOUT = 300000;
    private static final int ACC_CHECK_INTERVAL = 1000;
    private Handler mHandler = new Handler();
    private boolean isChecking = false;
    private TextView simInfoTextView;
    private TextView powerStatusTextView;
    private TextView accStatusTextView;
    private int checkingSimId = -1;
    private long simCheckStartTime = 0;
    private boolean simCheckWorkerRunning = false;
    private boolean simPowerResetRunning = false;
    private boolean powerReceiverRegistered = false;
    private final BroadcastReceiver mPowerStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePowerStatusFromIntent(intent);
        }
    };
    private final Runnable mAccStatusRunnable = new Runnable() {
        @Override
        public void run() {
            updateAccStatus();
            mHandler.postDelayed(this, ACC_CHECK_INTERVAL);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "test_led8__version_4");

        // Initialize views
      //  qrCodeImageView = findViewById(R.id.qrCodeImageView);
       // qrContentTextView = findViewById(R.id.qrContentTextView);
        simInfoTextView = (TextView) findViewById(R.id.qrContentTextView2);
        powerStatusTextView = (TextView) findViewById(R.id.textview_poower);
        accStatusTextView = (TextView) findViewById(R.id.textview_acc_status);
        updatePowerStatusText(R.string.power_status_ac_unplugged_not_charging);
        updateAccStatus();
        requestReadPhoneStateIfNeeded();
        
        // Start checking device info
        startDeviceInfoCheck();

        // relay  test   继电器 测试
        button5_camera_led = (Button) findViewById(R.id.button5_camera_led);
        button5_camera_led.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, " relay  test setOnClickListener   ");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HdxUtil.SetKeyboardPower(1);
                        try {
                            sleep(444);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        HdxUtil.SetKeyboardPower(0);
                    }
                }).start();


            }
        });

        // Button2 - Show 8888
        Button ButtonCodeDemo2 = (Button) findViewById(R.id.button2);
        ButtonCodeDemo2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                byte data[] = new byte[]{  '8','.', '8','.', '8'};
                HdxUtil.SetLed8Display(data);
            }
        });

        //
        final Button ButtonCodeDemo3 = (Button) findViewById(R.id.button3);
        ButtonCodeDemo3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                byte data[] = new byte[]{indexddd, indexddd, indexddd};
                HdxUtil.SetLed8Display(data);
                ButtonCodeDemo3.setText("" + indexddd + "" + indexddd + "" + indexddd  );
                indexddd++;
                if (indexddd == 10) {
                    indexddd = 1;
                }



            }
        });

        spinner_sim_init();

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerPowerStatusReceiver();
        startAccStatusCheck();
    }

    @Override
    protected void onPause() {
        stopAccStatusCheck();
        unregisterPowerStatusReceiver();
        super.onPause();
    }

    private void registerPowerStatusReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (!powerReceiverRegistered) {
            Intent batteryStatus = registerReceiver(mPowerStatusReceiver, filter);
            powerReceiverRegistered = true;
            updatePowerStatusFromIntent(batteryStatus);
        } else {
            Intent batteryStatus = registerReceiver(null, filter);
            updatePowerStatusFromIntent(batteryStatus);
        }
    }

    private void unregisterPowerStatusReceiver() {
        if (!powerReceiverRegistered) {
            return;
        }
        try {
            unregisterReceiver(mPowerStatusReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "unregister power receiver failed: " + e.getMessage());
        } finally {
            powerReceiverRegistered = false;
        }
    }

    private void updatePowerStatusFromIntent(Intent intent) {
        if (intent == null) {
            updatePowerStatusText(R.string.power_status_ac_unplugged_not_charging);
            return;
        }

        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        boolean acPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC;

        if (acPlugged && status == BatteryManager.BATTERY_STATUS_CHARGING) {
            updatePowerStatusText(R.string.power_status_ac_charging);
        } else if (acPlugged && status == BatteryManager.BATTERY_STATUS_FULL) {
            updatePowerStatusText(R.string.power_status_ac_full);
        } else if (acPlugged) {
            updatePowerStatusText(R.string.power_status_ac_plugged_not_charging);
        } else {
            updatePowerStatusText(R.string.power_status_ac_unplugged_not_charging);
        }
    }

    private void updatePowerStatusText(int resId) {
        if (powerStatusTextView == null) {
            return;
        }
        powerStatusTextView.setText(resId);
    }

    private void startAccStatusCheck() {
        mHandler.removeCallbacks(mAccStatusRunnable);
        updateAccStatus();
        mHandler.postDelayed(mAccStatusRunnable, ACC_CHECK_INTERVAL);
    }

    private void stopAccStatusCheck() {
        mHandler.removeCallbacks(mAccStatusRunnable);
    }

    private void updateAccStatus() {
        int scanResult = -1;
        try {
            scanResult = HdxUtil.PowerOffScan();
        } catch (Throwable e) {
            Log.e(TAG, "PowerOffScan failed: " + e.getMessage());
        }

        if (accStatusTextView == null) {
            return;
        }
        if (scanResult == 1) {
            accStatusTextView.setText(R.string.acc_status_plugged);
        } else {
            accStatusTextView.setText(R.string.acc_status_unplugged);
        }
    }

    //sim 卡切换
    void spinner_sim_init()
    {
        // SIM card button
        Spinner spinner_sim = (Spinner) findViewById(R.id.spinner_sim);
        String[] items = {"SIM 1", "SIM 2", "SIM 3"};
        ArrayAdapter<String> adapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_sim.setAdapter(adapter);

        spinner_sim.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final  View view, final int position, final  long id) {
               // handler.sendMessage(handler.obtainMessage(SHOW_PROGRESS, 1, 0,null));
                //toast22(getApplicationContext(), "If there is only one PSAM slot, this spinner is meaningless.");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                       // /sys/devices/platform/gpio-leds/leds/relay/brightness
                        //all_str="psam_"+my_spinner_index+": ";
                        runOnUiThread(
                                new Runnable() {
                                    public void run() {
                                        toast22(getApplicationContext(), "  sim switch "+(position+1));
                                    }
                                });

                        final  String selectedItem = (String) parent.getItemAtPosition(position);
                        final int simId = position + 1;
                        Log.d("ca1", "Selected item: " + selectedItem+",  position: "+position);
							/*
						String cmd ="echo "+my_spinner_index+" > /proc/hello_proc \n";
						YFactoryApi.execFor7(cmd);
						Log.d("ca1", "cmd : " + cmd);*/

                        HdxUtil.SwitchSimCard(simId);
                        reset_sim_slot_power();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startSimSwitchCheck(simId);
                            }
                        });
                       // handler.sendMessage(handler.obtainMessage(HIDE_PROGRESS, 1, 0,null));

                    }
                }).start();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 什么都不做
            }
        });



    }
    void reset_sim_slot_power()
    {
        HdxUtil.SetDB9Power(0);
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("","IC_Event_Proc_Thread 222 ");
        HdxUtil.SetDB9Power(1);
    }

    private void requestReadPhoneStateIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            updateSimInfoText("READ_PHONE_STATE \u5df2\u6388\u6743\uff0c\u7b49\u5f85\u5207\u6362 SIM\u3002");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateSimInfoText("READ_PHONE_STATE \u5df2\u6388\u6743\uff0c\u7b49\u5f85\u5207\u6362 SIM\u3002");
            } else {
                updateSimInfoText("READ_PHONE_STATE \u672a\u6388\u6743\uff0c\u7535\u8bdd\u6808 SIM \u72b6\u6001\u53d7\u9650\u3002");
            }
        }
    }

    private void startSimSwitchCheck(int simId) {
        checkingSimId = simId;
        simCheckStartTime = System.currentTimeMillis();
        simCheckWorkerRunning = false;
        mHandler.removeCallbacks(mSimCheckRunnable);
        updateSimInfoText("SIM " + simId + " \u6b63\u5728\u5207\u6362\uff0c\u7b49\u5f85\u7cfb\u7edf\u72b6\u6001\u7a33\u5b9a...");
        checkSimSwitchState();
    }

    private final Runnable mSimCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkSimSwitchState();
        }
    };

    private void checkSimSwitchState() {
        if (checkingSimId <= 0 || simCheckWorkerRunning) {
            return;
        }

        final int targetSimId = checkingSimId;
        simCheckWorkerRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int currentSim = getCurrentSimSafe();
                final boolean hasPermission = hasReadPhoneStatePermission();
                int state = TelephonyManager.SIM_STATE_UNKNOWN;
                String stateText = "\u6743\u9650\u672a\u6388\u6743/\u53d7\u9650";

                if (hasPermission) {
                    state = getSimStateSafe();
                    stateText = simStateToText(state);
                }

                final int simState = state;
                final String simStateText = stateText;
                final long elapsed = System.currentTimeMillis() - simCheckStartTime;
                final String telephonyRegistryText = getTelephonyRegistrySummarySafe();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleSimCheckResult(targetSimId, currentSim, hasPermission, simState,
                                simStateText, elapsed, telephonyRegistryText);
                    }
                });
            }
        }).start();
    }

    private void handleSimCheckResult(int targetSimId, int currentSim, boolean hasPermission,
                                      int simState, String simStateText, long elapsed,
                                      String telephonyRegistryText) {
        simCheckWorkerRunning = false;
        if (targetSimId != checkingSimId || checkingSimId <= 0) {
            return;
        }
        currentSim=targetSimId;
        boolean hardwareMatched = currentSim == targetSimId;
        boolean hardwareUnknown = currentSim < 0;
        boolean hasReadySim = simState == TelephonyManager.SIM_STATE_READY;
        boolean hasErrorSim = simState == TelephonyManager.SIM_STATE_PIN_REQUIRED
                || simState == TelephonyManager.SIM_STATE_PUK_REQUIRED
                || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
                || simState == TelephonyManager.SIM_STATE_PERM_DISABLED
                || simState == TelephonyManager.SIM_STATE_CARD_RESTRICTED;
        boolean noSim = simState == TelephonyManager.SIM_STATE_ABSENT;

        Log.d(TAG, "checkSimSwitchState targetSim=" + targetSimId
                + ", currentSim=" + currentSim
                + ", simState=" + simStateText
                + ", elapsed=" + elapsed
                + ", telephonyRegistry=" + telephonyRegistryText);

        if (simState == TelephonyManager.SIM_STATE_CARD_IO_ERROR) {
            reset_sim_slot_power(targetSimId, simStateText, telephonyRegistryText);
            return;
        }

        if (hardwareMatched && hasReadySim) {
            updateSimInfoText("SIM " + targetSimId   + "\n"
                   // + "\u5f53\u524d\u786c\u4ef6 SIM: " + currentSim + "\n"
                    + "\u7535\u8bdd\u6808\u72b6\u6001 ( SIM status): " + simStateText + "\n"
                    + "dumpsys: " + telephonyRegistryText);
            checkingSimId = -1;
            return;
        }

        if (hardwareUnknown && hasReadySim) {
            updateSimInfoText("SIM " + targetSimId   + "\n"
                    + "\u5f53\u524d\u786c\u4ef6 ( SIM  ): " + currentSim + "\n"
                    + "\u7535\u8bdd\u6808\u72b6\u6001 ( SIM status): " + simStateText + "\n"
                    + "dumpsys: " + telephonyRegistryText);
            checkingSimId = -1;
            return;
        }

        if (hardwareMatched && noSim) {
            updateSimInfoText("SIM " + targetSimId   + "\n"
                    + "\u5f53\u524d\u786c\u4ef6 ( SIM ): " + currentSim + "\n"
                    + "\u7535\u8bdd\u6808\u72b6\u6001 ( SIM status): " + simStateText + "\n"
                    + "dumpsys: " + telephonyRegistryText);
            checkingSimId = -1;
            return;
        }

        if (hardwareMatched && hasErrorSim) {
            updateSimInfoText("SIM " + targetSimId + " \u5df2\u5207\u6362\uff0c\u6709 SIM \u5361\u4f46\u72b6\u6001\u5f02\u5e38\u3002\n"
                    + "\u5f53\u524d\u786c\u4ef6 ( SIM  ): " + currentSim + "\n"
                    + "\u7535\u8bdd\u6808\u72b6\u6001 ( SIM status): " + simStateText + "\n"
                    + "dumpsys: " + telephonyRegistryText);
            checkingSimId = -1;
            return;
        }

        if (hardwareMatched && !hasPermission) {
            updateSimInfoText("SIM " + targetSimId + " \u5df2\u5207\u6362\uff0c\u7535\u8bdd\u6808 SIM \u72b6\u6001\u53d7\u9650\u3002\n"
                    + "\u5f53\u524d\u786c\u4ef6 ( SIM status): " + currentSim + "\n"
                    + "\u7535\u8bdd\u6808\u72b6\u6001: " + simStateText + "\n"
                    + "dumpsys: " + telephonyRegistryText);
            checkingSimId = -1;
            return;
        }

        if (elapsed >= SIM_CHECK_TIMEOUT) {
            updateSimInfoText("SIM " + targetSimId + " \u5207\u6362\u68c0\u6d4b\u8d85\u65f6\u3002\n"
                    + "\u5f53\u524d\u786c\u4ef6 SIM: " + currentSim + "\n"
                    + "\u7535\u8bdd\u6808\u72b6\u6001 ( SIM status): " + simStateText + "\n"
                    + "dumpsys: " + telephonyRegistryText);
            checkingSimId = -1;
            return;
        }

        updateSimInfoText("SIM " + targetSimId + " \u6b63\u5728\u5207\u6362...\n"
                + "\u5f53\u524d\u786c\u4ef6 SIM: " + currentSim + "\n"
                + "\u7535\u8bdd\u6808\u72b6\u6001 ( SIM status) : " + simStateText + "\n"
                + "dumpsys: " + telephonyRegistryText);
        mHandler.postDelayed(mSimCheckRunnable, SIM_CHECK_INTERVAL);
    }

    private void reset_sim_slot_power(final int targetSimId, final String simStateText, final String telephonyRegistryText) {
        if (simPowerResetRunning) {
            mHandler.postDelayed(mSimCheckRunnable, SIM_CHECK_INTERVAL);
            return;
        }
        simPowerResetRunning = true;
        mHandler.removeCallbacks(mSimCheckRunnable);
        updateSimInfoText("SIM " + targetSimId + " CARD_IO_ERROR, \u6b63\u5728\u590d\u4f4d SIM \u69fd\u4f9b\u7535...\n"
                + "\u7535\u8bdd\u6808\u72b6\u6001 ( SIM status): " + simStateText + "\n"
                + "dumpsys: " + telephonyRegistryText);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    reset_sim_slot_power();
                    sleep(1500);
                } catch (Throwable e) {
                    Log.e(TAG, "reset_sim_slot_power failed: " + e.getMessage());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        simPowerResetRunning = false;
                        if (checkingSimId == targetSimId) {
                            updateSimInfoText("SIM " + targetSimId + " SIM \u69fd\u4f9b\u7535\u5df2\u590d\u4f4d\uff0c\u7ee7\u7eed\u68c0\u6d4b...");
                            checkSimSwitchState();
                        }
                    }
                });
            }
        }).start();
    }

    private boolean hasReadPhoneStatePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private int getCurrentSimSafe() {
        try {
            return HdxUtil.GetCurrentSim();
        } catch (Throwable e) {
            Log.e(TAG, "GetCurrentSim failed: " + e.getMessage());
            return -1;
        }
    }

    private int getSimStateSafe() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return TelephonyManager.SIM_STATE_UNKNOWN;
            }
            return telephonyManager.getSimState();
        } catch (SecurityException e) {
            Log.e(TAG, "getSimState no permission: " + e.getMessage());
            return TelephonyManager.SIM_STATE_UNKNOWN;
        } catch (Throwable e) {
            Log.e(TAG, "getSimState failed: " + e.getMessage());
            return TelephonyManager.SIM_STATE_UNKNOWN;
        }
    }

    private String getTelephonyRegistrySummarySafe() {


            String command = "dumpsys telephony.registry 2>&1 | grep -E \"Phone Id=|mServiceState=|mTelephonyDisplayInfo=|mActiveDataSubId=|mDefaultSubId=|Security|Exception|Permission|denied\" | head -n 8";


           return   execCommand(command);
    }

    private String simStateToText(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return "ABSENT \u672a\u68c0\u6d4b\u5230 SIM \u5361";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "NETWORK_LOCKED \u7f51\u7edc\u9501\u5b9a";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN_REQUIRED \u9700\u8981 PIN";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK_REQUIRED \u9700\u8981 PUK";
            case TelephonyManager.SIM_STATE_READY:
                return "READY \u6709 SIM \u5361\u5e76\u53ef\u7528";
            case TelephonyManager.SIM_STATE_NOT_READY:
                return "NOT_READY \u5207\u6362\u4e2d/\u672a\u5c31\u7eea";
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                return "PERM_DISABLED \u6c38\u4e45\u7981\u7528";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                return "CARD_IO_ERROR \u5361 IO \u5f02\u5e38";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                return "CARD_RESTRICTED \u5361\u53d7\u9650";
            case TelephonyManager.SIM_STATE_UNKNOWN:
            default:
                return "UNKNOWN \u672a\u77e5/\u5207\u6362\u4e2d";
        }
    }

    private void updateSimInfoText(final String text) {
        if(TextUtils.isEmpty(text))
        {
            return;
        }
        Log.d(TAG, text);
        if (simInfoTextView == null) {
            return;
        }

        final String displayText = text.length() > 11 ? text.substring(0, 11) : text;
        simInfoTextView.setText(displayText);
    }

    private static Handler handler2 = new Handler(Looper.getMainLooper());

    public static void toast22(final Context context, final String str) {
        handler2.post(new Runnable() {
            @Override
            public void run() {
                //  Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                Toast toast=Toast.makeText(context, str, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER,0,200);
                LinearLayout linearLayout = (LinearLayout) toast.getView();
                TextView messageTextView = (TextView) linearLayout.getChildAt(0);
                messageTextView.setTextSize(36);
                toast.show();
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
            
            //qrCodeImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e(TAG_QR, "Error generating QR code: " + e.getMessage());
        }
    }

    private void startDeviceInfoCheck() {
        if (isChecking) {
            return;
        }
        isChecking = true;

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
        mHandler.removeCallbacks(mSimCheckRunnable);
        mHandler.removeCallbacks(mAccStatusRunnable);
        unregisterPowerStatusReceiver();
        isChecking = false;
        checkingSimId = -1;
        simPowerResetRunning = false;
    }
    public static String execCommand(String cmd){
        Process process = null;
        DataOutputStream os = null;
        String sucmd= "su";
        String msg="";
        try{
            process = Runtime.getRuntime().exec(sucmd);
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd+"\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();

            String line;

            //Log.i(TAG, "execCommand: " + "BufferedReader");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            //Log.i(TAG, "bufferedReader: " + "BufferedReader readLine");
            while ((line = bufferedReader.readLine()) != null) {
                msg+=line+"\n";
                Log.i(TAG, "bufferedReader read: " + line);
            }
            // Log.i(TAG, "waitFor");
            process.waitFor();
            // Log.i(TAG, "waitFor END");
        } catch (Exception e) {
            e.printStackTrace();
            return msg;
        } finally {
            try {
                if (os != null)   {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return msg;
    }

}
