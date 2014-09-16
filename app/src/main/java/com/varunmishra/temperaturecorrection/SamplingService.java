package com.varunmishra.temperaturecorrection;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;


import com.varunmishra.temperaturecorrection.Provider.Provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class SamplingService extends Service implements SensorEventListener {
    public static final String TAG = SamplingService.class.getName();
    public static final int SCREEN_OFF_RECEIVER_DELAY = 500;
    int memTotal, pID;
    Vector<String> memFree, buffers, cached, active, inactive, swapTotal, dirty;
    Vector<Float> cPUTotalP, cPUAMP, cPURestP;
    private String x;

    private String[] a;
    private long workT, totalT, workAMT;
    private long total, totalBefore, work, workBefore, workAM, workAMBefore;
    private boolean FIRSTTIMEREAD_FLAG = true;
    private BufferedReader readStream;
   SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyHHmmss");
   
    private SensorManager mSensorManager = null;
    private WakeLock mWakeLock = null;
    public static float pluggedin=0,phonebat=0,batterypercent=0,rawtemp=0,temp=0;
    int tempflag=0;
    int count1=0;
    String format="";
    /*
     * Register this as a sensor event listener.
     */
    private Runnable readRunnable = new Runnable() {
        public void run() {
            read(); // We call here read() because to draw the graphic we need at less 2 read values.
            while (readThread == Thread.currentThread()) {
                try {
                    read();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    // The Runnable that reads the values will be run from a separated thread. The performance is better.

    private Thread readThread = new Thread(readRunnable, "readThread");


    private Runnable writeRunnable = new Runnable() {
        public void run() {
            while (writeThread == Thread.currentThread()) {
                try {

                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    // The Runnable that reads the values will be run from a separated thread. The performance is better.
    private Thread writeThread = new Thread(writeRunnable, "readThread");


    private void registerListener() {

    		if(mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) !=null){
    		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
    		tempflag=1;

    		}
        this.registerReceiver(this.batteryInfoReceiver,	new IntentFilter(Intent.ACTION_BATTERY_CHANGED));



    }

    /*
     * Un-register this as a sensor event listener.
     */
    private void unregisterListener() {
        this.unregisterReceiver(this.batteryInfoReceiver);

        mSensorManager.unregisterListener(this);
    }

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive("+intent+")");

            if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }
             
            Runnable runnable = new Runnable() {
                public void run() {
                    Log.i(TAG, "Runnable executing.");
                    unregisterListener();
                    registerListener();
                }
            };

            new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
        }
    };

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged().");
    }

    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "onSensorChanged()."+count1);
        
        count1++;
         format = s.format(new Date());
        if(event.sensor.getType()== Sensor.TYPE_AMBIENT_TEMPERATURE)
	 {
         temp=event.values[0];
         rawtemp=event.values[1];
		 if (count1==1) writeThread.start();
	 }

        ContentValues values = new ContentValues();
        values.put(com.varunmishra.temperaturecorrection.Provider.Provider.id,format);
        values.put(Provider.name, temp);
        Uri uri = getContentResolver().insert(Provider.CONTENT_URI, values);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        memFree = new Vector<String>(440);
        buffers = new Vector<String>(440);
        cached = new Vector<String>(440);
        active = new Vector<String>(440);
        inactive = new Vector<String>(440);
        swapTotal = new Vector<String>(440);
        dirty = new Vector<String>(440);
        cPUTotalP = new Vector<Float>(440);
        cPUAMP = new Vector<Float>(440);
        cPURestP = new Vector<Float>(440);

        // We create the notifications for the status bar.


        pID = Process.myPid();
        readThread.start();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        PowerManager manager =
            (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
     
       
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    


    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        unregisterListener();
        mWakeLock.release();

        try {
            readThread.interrupt();
            writeThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        readThread = null;
        writeThread=null;
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
      
        	Intent intent1 = new Intent(this, TemperatureCorrectionMain.class);
        	PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent1, 0);

        	// build notification
        	// the addAction re-use the same intent to keep the example short
        	Notification n  = new Notification.Builder(this)
        	        .setContentTitle("Temperature Correction")
        	        .setContentText("Sampling")
        	        .setSmallIcon(R.drawable.ic_launcher)
        	        .setContentIntent(pIntent)
        	        .setAutoCancel(true)
        	       
        	       .build();
        	    
        	
        
       
        
        startForeground(Process.myPid(), n);
        registerListener();
        mWakeLock.acquire();

        return START_STICKY;
    }

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            phonebat= intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
            batterypercent = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            pluggedin = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
            phonebat=phonebat/10;


        }
    };
    private void read() {
        try {
            readStream = new BufferedReader(new FileReader("/proc/meminfo"));
            x = readStream.readLine();
            while (x!=null) {

		/* When the limit TOTAL_INTERVALS is surpassed by some vector we have to remove all
		 * the surpassed elements because, if not, the capacity of the vector will be increase x2. */
                while (memFree.size()>=440) memFree.remove(memFree.size()-1);
                while (buffers.size()>=440) buffers.remove(buffers.size()-1);
                while (cached.size()>=440) cached.remove(cached.size()-1);
                while (active.size()>=440) active.remove(active.size()-1);
                while (inactive.size()>=440) inactive.remove(inactive.size()-1);
                while (swapTotal.size()>=440) swapTotal.remove(swapTotal.size()-1);
                while (dirty.size()>=440) dirty.remove(dirty.size()-1);
                while (cPUTotalP.size()>=440) cPUTotalP.remove(dirty.size()-1);
                while (cPUAMP.size()>=440) cPUAMP.remove(dirty.size()-1);
                while (cPURestP.size()>=440) cPURestP.remove(dirty.size()-1);

                // We read the memory values. The percents are calculated in the AnotherMonitor class.
                if (FIRSTTIMEREAD_FLAG && x.startsWith("MemTotal:")) memTotal = Integer.parseInt(x.split("[ ]+", 3)[1]); FIRSTTIMEREAD_FLAG = false;
                if ( x.startsWith("MemFree:")) memFree.add(0, x.split("[ ]+", 3)[1]);
                if (x.startsWith("Buffers:")) buffers.add(0, x.split("[ ]+", 3)[1]);
                if ( x.startsWith("Cached:")) cached.add(0, x.split("[ ]+", 3)[1]);
                if ( x.startsWith("Active:")) active.add(0, x.split("[ ]+", 3)[1]);
                if ( x.startsWith("Inactive:")) inactive.add(0, x.split("[ ]+", 3)[1]);
                if ( x.startsWith("SwapTotal:")) swapTotal.add(0, x.split("[ ]+", 3)[1]);
                if ( x.startsWith("Dirty:")) dirty.add(0, x.split("[ ]+", 3)[1]);
                x = readStream.readLine();
            }

	    /* We read and calculate the CPU usage percents. It is possible that negative values or values higher than 100% appear.
	    Get more information about how it is done on http://stackoverflow.com/questions/1420426
	    To see what is each number, see http://kernel.org/doc/Documentation/filesystems/proc.txt */

            readStream = new BufferedReader(new FileReader("/proc/stat"));
            a = readStream.readLine().split("[ ]+", 9);
            work = Long.parseLong(a[1]) + Long.parseLong(a[2]) + Long.parseLong(a[3]);
            total = work + Long.parseLong(a[4]) + Long.parseLong(a[5]) + Long.parseLong(a[6]) + Long.parseLong(a[7]);

            readStream = new BufferedReader(new FileReader("/proc/"+pID+"/stat"));
            a = readStream.readLine().split("[ ]+", 18);
            workAM = Long.parseLong(a[13]) + Long.parseLong(a[14]) + Long.parseLong(a[15]) + Long.parseLong(a[16]);

            if (totalBefore != 0) {
                workT = work - workBefore;
                totalT = total - totalBefore;
                workAMT = workAM - workAMBefore;
                cPUTotalP.add(0, workT*100/(float)totalT);
                cPUAMP.add(0, workAMT*100/(float)totalT);
                cPURestP.add(0, (workT - workAMT)*100/(float)totalT);
            }
            workBefore = work;
            totalBefore = total;
            workAMBefore = workAM;

            readStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}