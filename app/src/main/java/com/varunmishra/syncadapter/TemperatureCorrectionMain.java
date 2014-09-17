package com.varunmishra.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.varunmishra.syncadapter.Provider.Provider;

public class TemperatureCorrectionMain extends Activity  implements OnClickListener {
    public static final String TAG = TemperatureCorrectionMain.class.getName();
    public static Button start, stop;
    public static TextView t, temp1, temp2, humidity1, humidity2, pressure1, pressure2;
    public static final String SCHEME = "content://";
    // Content provider authority
    // Path for the content provider table
    public static final String TABLE_PATH = "temperature";

    public static final String AUTHORITY = "com.varunmishra.syncadapter.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "com.varunmishra.syncadapter.accounts.GenericAccountService";
    // The account name
    public static final String ACCOUNT = "sync";
    // Instance fields
    Account mAccount;
    Uri mUri;
    // A content resolver for accessing the provider
    ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        start = (Button) findViewById(R.id.button1);
        stop = (Button) findViewById(R.id.button2);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        t = (TextView) findViewById(R.id.textView1);
        temp1 = (TextView) findViewById(R.id.textView3);
        temp2 = (TextView) findViewById(R.id.textView4);
        humidity1 = (TextView) findViewById(R.id.textView6);
        humidity2 = (TextView) findViewById(R.id.textView7);
        pressure1 = (TextView) findViewById(R.id.textView9);
        pressure2 = (TextView) findViewById(R.id.textView10);
        mAccount = CreateSyncAccount(this);

        mUri = new Uri.Builder()
                .scheme(SCHEME)
                .authority(AUTHORITY)
                .path(TABLE_PATH)
                .build();

        Handler h=new Handler();
        TableObserver observer = new TableObserver(null);
        mResolver = getContentResolver();
       // mResolver.registerContentObserver(mUri, true, observer);
        //mResolver.requestSync(mAccount,AUTHORITY,Bundle.EMPTY);
       // mResolver.registerContentObserver(mUri, true, observer);
          mResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

        if (isMyServiceRunning()) {
            refreshdisplay();
            start.setEnabled(false);
        } else {
            t.setText("Idle");
            start.setEnabled(true);
        }
    }
    public class TableObserver extends ContentObserver {


        public TableObserver(Handler handler) {
            super(handler);
        }

        /*
                         * Define a method that's called when data in the
                         * observed content provider changes.
                         * This method signature is provided for compatibility with
                         * older platforms.
                         */
        @Override
        public void onChange(boolean selfChange) {
            /*
             * Invoke the method signature available as of
             * Android platform version 4.1, with a null URI.
             */
            onChange(selfChange, null);
        }
        /*
         * Define a method that's called when data in the
         * observed content provider changes.
         */
        @Override
        public void onChange(boolean selfChange, Uri changeUri) {
            ContentResolver.requestSync(mAccount, AUTHORITY, null);
        }
    }
    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call
             * context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
        }
        return newAccount;
    }



    public static void refreshdisplay()
    {


        temp1.setText("Current Temp - " + SamplingService.temp);
    	humidity1.setText("Raw Temp" + SamplingService.rawtemp);
    	pressure1.setText("Battery Temp" + SamplingService.phonebat);

    }
    public void ref()
    {
        ContentValues values = new ContentValues();
        values.put(Provider.name, SamplingService.temp);
        Uri uri = getContentResolver().insert(Provider.CONTENT_URI, values);

    }
    private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (SamplingService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
    @Override
    public void onResume() {
        super.onResume();

       
    }

	@Override
	public void onClick(View src) {
		// TODO Auto-generated method stub
		 switch (src.getId()) {
		    case R.id.button1:
		      Log.d(TAG, "onClick: starting srvice");
		      startService(new Intent(this, SamplingService.class));
		      
		        
		        	start.setEnabled(false);
		        refreshdisplay();
		      break;
		    case R.id.button2:
		      Log.d(TAG, "onClick: stopping srvice");
		      stopService(new Intent(this, SamplingService.class));
		      t.setText("Idle");
	        	start.setEnabled(true);
		      break;
		    	
	      
		 }
	}

}
