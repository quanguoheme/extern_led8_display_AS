
 
package hdx;

import android.util.Log;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class HdxUtil{
    private static final String TAG = "HdxUtil";
    public static final int CARDTYPE_IC=1;
    public static final int CARDTYPE_PSAM=2;
    public static final int SERIAL_FUNCTION_PRINTER=0;
    public static final int SERIAL_FUNCTION_IDCARD=1;
    public static final int SERIAL_FUNCTION_KEYBOARD=2;
    public static final int SERIAL_FUNCTION_SCAN=3;
    public static final int SERIAL_FUNCTION_FINGER=4;
    public static final int RED_LCD=0;
    public static final int GREEN_LCD=1;
    public static final int BLUE_LCD=2;
    public static final int FILTER_IR=0;
    public static final int FILTER_NORMAL=1;
    private Context mContext=null;
    public HdxUtil(Context context)
    {
    	mContext=context;
    }
    public static int EnableBuzze(int enable)
    {
         return buzzerControl(enable);
    }
    
    public static int SetCameraBacklightness(int br)
    {
        return cameraBacklightControl(br);
    }
    public native static int SetIDCARDPower(int enable);
    public native static int SetVeinPower(int enable);
    public native static String GetBaseDeviceID(String pdev);
    public native static int SetRfidPower(int enable);
    public native static int SetPrinterPower(int enable);
    public native static int GetCurrentSim();
    public native static int GetKey138Status();
    public native static int TriggerScan();
    public native static int TriggerScan2();
    public native static int PowerOffScan();
    private native static int buzzerControl(int enable);
    public native static int SwitchSimCard(int id);
    public native static int SwitchICCard(int id);

    public native static int EnableKeyboard(int enable);
    public native static int SwitchSerialFunction(int fucn);
    public native static int SetV12Power(int enable);
    public native static int SetDB9Power(int enable);
    public native static int SetDB9Power2(int enable);
    public native static int SetFingerPower(int enable);
    public native static int SetWifiApPower(int enable);
    public native static int SetKeyboardPower(int enable);
    public native static int SetPSAMPower(int enable);
    public native static String GetPrinterPort();
    public native static int ledControl(int led,int brightness);
    public native static int customLed(int led,int enable);
    public native static int SwitchFilter(int status);
    private native static int cameraBacklightControl(int br);
    //for led8 for gc111
    public native static int SetLed8Display(byte data[]);
    public native static int SetLed8DisplayString(String data);
    public native static int SetGreedLed(int enable);
    public native static int SetRedLed(int enable);    
    static {
    	System.loadLibrary("hdxutil");
    }
}
