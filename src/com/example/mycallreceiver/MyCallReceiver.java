package com.example.mycallreceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.Config;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyCallReceiver extends BroadcastReceiver {
	
//	static{
//		System.loadLibrary("jnilibsvm");
//	}
//	
//	public native void jniSvmPredict(String cmd);
//	public native void jniSvmScale(String cmd);
//	public native void processAudio(String cmd);
	private static final String TAG = "Broadcast Debug";
	
	public static JSONObject callStart = null;
	public static JSONObject callEnd = null;
	public static int raw_counter = 0;

	
	private boolean isRecording = false;
	int sampleRate = 44100;
    private Thread recordingThread = null;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    
	File root = null;
	File dir = null;
    
	AudioManager	am	= null;
	AudioRecord record =null;
	MediaPlayer mediaPlayer = null;
	
	SensorManager mySensorManager = null;
	Sensor LightSensor = null;
	Sensor ProxiSensor = null;
	
	TelephonyManager telephonyManager = null;
	
	
	File SoundFile = null;
	FileOutputStream foutSound = null;
	OutputStreamWriter outwriterSound = null;
	
	
	File SoundDataFile = null;
	FileOutputStream foutSoundData = null;
	OutputStreamWriter outwriterSoundData = null;
	
	String device_ID;
	
//	File LightFile = null;
//	FileOutputStream foutLight = null;
//	OutputStreamWriter outwriterLight = null;
//	
//	File RGBFile = null;
//	FileOutputStream foutRGB = null;
//	OutputStreamWriter outwriterRGB = null;
//	
//	File WifiFile = null;
//	FileOutputStream foutWifi= null;
//	OutputStreamWriter outwriterWifi = null;
	
	File ProxiFile = null;
	FileOutputStream foutProxi = null;
	OutputStreamWriter outwriterProxi = null;
	
	File AllInfoFile = null;
	FileOutputStream foutAllInfo = null;
	OutputStreamWriter outwriterAllInfo = null;
	
//	File TestInputFile = null;
//	FileOutputStream foutTestInput = null;
//	OutputStreamWriter outwriterTestInput = null;
//	
//	
//	File ResultFile = null;
//	FileOutputStream foutResult = null;
//	OutputStreamWriter outwriterResult = null;
	
	
	WifiManager wifiManager;
	WifiInfo wifiInfo = null; 
	

	BufferedReader bufferedReader = null;
	
	File InputRGBFile = null;
	

	
	
	
    public static String incoming_number;
    private String current_state,previus_state,event;
    public static Boolean dialog= false;
    private Context context;
    private SharedPreferences sp,sp1;
    private SharedPreferences.Editor spEditor,spEditor1;
    
    private ArrayList<Double> lightValue = null;
    private ArrayList<Double> RValue = null;
    private ArrayList<Double> GValue = null;
    private ArrayList<Double> BValue = null;
    private ArrayList<Double> WValue = null;
    private ArrayList<Integer> TValue = null;
    private ArrayList<Integer> WifiValue = null;
    
    
    private float ReadProxi = 0;
    private int RecordFlag = 0;
    public int EndingCallFlag =0;
    
//    public String Cmd_svm_scale;
//    public String Cmd_svm_predict;
//    public String Cmd_get_feature;
//    
//    public int calculate_mode = 0;
    
    //  0- default; 1- RGB; 2 - night predict ; 3 - > 5000; 4 - audio test; 5 - no light and wifi
    public int Audio_flag = 0;
    public int Audio_start = 0;
  
    public int proxi_count = 0;
    public long proxi_time = 0;
    public long cur_proxi_time = 0;
    public int proxi_start = 0;
    public long stop_proxi_time = 0;
    public long stop_proxi_time_end = 0;
    public long stop_proxi_time_end2 = 0;
    public Thread proxiThread = null;
    public Thread proxiThread2 = null;
    public int register_light = 0;
    public int proxi_thread_start = 0;
    public int proxi_thread_start2 = 0;
    
    public int CallType = 0;
    
    
	

    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	
    	this.context = context;
    	event = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
    	incoming_number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
    	previus_state = getCallState(context);
        current_state = "IDLE";
        
        switch (event) {
        case "RINGING":
        	
            Log.d(TAG, "State : Ringing, incoming_number : " + incoming_number);

            
            if((previus_state.equals("IDLE")) || (previus_state.equals("FIRST_CALL_RINGING"))){
                current_state ="FIRST_CALL_RINGING";
            }
            if((previus_state.equals("OFFHOOK"))||(previus_state.equals("SECOND_CALL_RINGING"))){
                current_state = "SECOND_CALL_RINGING";
            }
            break;
        case "OFFHOOK":
            Log.d(TAG, "State : offhook, incoming_number : " + incoming_number);
            if((previus_state.equals("IDLE")) ||(previus_state.equals("FIRST_CALL_RINGING")) || previus_state.equals("OFFHOOK")){
                current_state = "OFFHOOK";
            }
            if(previus_state.equals("SECOND_CALL_RINGING")){
                current_state ="OFFHOOK";
            }
            
            break;
        case "IDLE":
            Log.d(TAG, "State : idle and  incoming_number : " + incoming_number);
            if((previus_state.equals("OFFHOOK")) || (previus_state.equals("SECOND_CALL_RINGING")) || (previus_state.equals("IDLE"))){
                current_state="IDLE"; 
            }    
            if(previus_state.equals("FIRST_CALL_RINGING")){
                current_state = "IDLE";
            }
            updateIncomingNumber("no_number",context);
            Log.d(TAG,"stored incoming number flushed");
            break;
        }
        
        if(!current_state.equals(previus_state)){
        	Log.d(TAG, "Updating  state from "+previus_state +" to "+current_state);
        	//Toast.makeText(context, "Updating  state from "+previus_state +" to "+current_state, Toast.LENGTH_LONG).show();
        	updateCallState(current_state,context);
        	if(((previus_state.equals("OFFHOOK")) && (current_state.equals("IDLE")))){
        		//Toast.makeText(context, "Updating  state from "+previus_state +" to "+current_state +"       register sensor", Toast.LENGTH_LONG).show();
        		init(context);
				callEnd = new JSONObject();
				EndingCallFlag = 1;
				registerProxiSensor();
				Log.d(TAG, "call end");
				long cur_time = System.currentTimeMillis();
				Log.d(TAG, String.valueOf(cur_time));
				Log.d(TAG, String.valueOf(EndingCallFlag));
				CallType = 2;
				


        	}
        	if (((previus_state.equals("IDLE")) && (current_state.equals("OFFHOOK"))))	
        	{
        		init(context);
        		callStart = new JSONObject();
        		raw_counter = 0;
        		RecordFlag =1;
        		EndingCallFlag = 3;
        		registerLightSensor2();
				long cur_time = System.currentTimeMillis();
				Log.d(TAG, String.valueOf(cur_time));
				Log.d(TAG, String.valueOf(EndingCallFlag));
        		CallType = 1;
        		Log.d(TAG, "outgoing call");

        	}
        	
//        	if ((previus_state.equals("FIRST_CALL_RINGING")) && (current_state.equals("OFFHOOK")))
//        	{
//        		init(context);
//        		EndingCallFlag = 0;
//        		registerLightSensor2();
//    			long curTime = System.currentTimeMillis();
//    			String curTimeStr = ""+curTime+";   ";
//    			Log.d(TAG, curTimeStr);
//                Intent i = new Intent("broadCastName");
//                // Data you need to pass to activity
//                Log.d("send start intent", "start send intent");
//                i.putExtra("message", "callStart"); 
//                context.sendBroadcast(i);
//        	}
        	
        	if ((previus_state.equals("IDLE")) && (current_state.equals("FIRST_CALL_RINGING")))
        	{
        		init(context);
        		callStart = new JSONObject();
        		raw_counter = 0;
        		EndingCallFlag = 2;
        		registerProxiSensor();
				long cur_time = System.currentTimeMillis();
				Log.d(TAG, String.valueOf(cur_time));
				Log.d(TAG, String.valueOf(EndingCallFlag));
        		CallType = 1;
        		Log.d(TAG, "incoming call");
        		
        	}	
        }   	   	
    }
    
    
 
    public void updateCallState(String state,Context context){
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = sp.edit();
        spEditor.putString("call_state", state);
        spEditor.commit();
        Log.d(TAG, "state updated");

    }
    public void updateIncomingNumber(String inc_num,Context context){
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = sp.edit();
        spEditor.putString("inc_num", inc_num);
        spEditor.commit();
        Log.d(TAG, "incoming number updated");
    }
    public String getCallState(Context context){
        sp1 = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1.getString("call_state", "IDLE");
        Log.d(TAG,"get previous state as :"+st);
        return st;
    }
    public String getIncomingNumber(Context context){
        sp1 = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1.getString("inc_num", "no_num");
        Log.i(TAG,"get incoming number as :"+st);
        return st;
    } 
    

   
    
    private void init(Context context){
    	    	
		root = android.os.Environment.getExternalStorageDirectory();
		dir = new File (root.getAbsolutePath() + "/CallDetection");
		dir.mkdirs();
		
		
		am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		mediaPlayer = MediaPlayer.create(context, R.raw.chirp22);
		//mediaPlayer.setVolume(0.5f, 0.5f);
		InputRGBFile = new File("/sys/devices/virtual/sensors/light_sensor/raw_data");
		

		
		
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        
        mySensorManager = (SensorManager)context.getSystemService(context.SENSOR_SERVICE);
        
        
    	telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
    	device_ID= telephonyManager.getDeviceId();
    	
    	
		SoundFile = new File(dir, "SoundRecord.txt");
    	if (!SoundFile.exists()){
    		try {
    			SoundFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
		SoundDataFile = new File(dir, "SoundData.txt");
    	if (!SoundDataFile.exists()){
    		try {
    			SoundDataFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	LightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);


		ProxiSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		if (ProxiSensor != null) {
			// textProxi_available.setText("Sensor PROXIMITY Available");
			ProxiFile = new File(dir, "ProxiRecord.txt");
			if (!ProxiFile.exists()) {
				try {
					ProxiFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else {
			// textProxi_available.setText("Sensor PROXIMITY NOT Available");
			// textTemper_reading.setText("Temperature(C): ");
		}
		
    	
    	
    	AllInfoFile = new File(dir,"AllInfoRecord.txt");
    	if (!AllInfoFile.exists()){
    		try {
    			AllInfoFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	Audio_start = 0;	
    	CallType = 0;
    	proxi_count = 0;
    	RecordFlag = 0;
    	proxiThread = null;
    	proxiThread2 = null;
    	register_light = 0;
    	proxi_thread_start = 0;
    	proxi_thread_start2 = 0;
    	callStart = null;
    	callEnd = null;
    	
		
    	Log.i(TAG, "init successfully");

    	
    }
    
 
    
    private void registerLightSensor() {
    	
	    lightValue = new ArrayList<Double>();
	    RValue = new ArrayList<Double>();
	    GValue = new ArrayList<Double>();
	    BValue = new ArrayList<Double>();
	    WValue = new ArrayList<Double>();
	    TValue = new ArrayList<Integer>();
	    WifiValue = new ArrayList<Integer>();	
	    long curTime = System.currentTimeMillis();
	    Log.d("create array", "create array "+String.valueOf(curTime));
    	
		if(LightSensor != null){
			
			if(AllInfoFile.exists()){
				  
				  try{
		    		
					  foutAllInfo = new FileOutputStream(AllInfoFile,true);
					  outwriterAllInfo= new OutputStreamWriter(foutAllInfo);
				
				  } catch(Exception e)
				  {

				  }
			}
			mySensorManager.registerListener(LightSensorListener, LightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

		if (proxiThread!= null)
		{
			if (!proxiThread.isInterrupted());
			{
				proxiThread.interrupt();
				proxiThread = null;
				Log.d("stop proxi thread", "stop proxi thread in register light");
				
			}
		}
		
    }
    
    
    private void registerLightSensor2() {
    	
	    lightValue = new ArrayList<Double>();
	    RValue = new ArrayList<Double>();
	    GValue = new ArrayList<Double>();
	    BValue = new ArrayList<Double>();
	    WValue = new ArrayList<Double>();
	    TValue = new ArrayList<Integer>();
	    WifiValue = new ArrayList<Integer>();	
    	
		if(LightSensor != null){
			
			
			if(AllInfoFile.exists()){
				  
				  try{
		    		
					  foutAllInfo = new FileOutputStream(AllInfoFile,true);
					  outwriterAllInfo= new OutputStreamWriter(foutAllInfo);
				
				  } catch(Exception e)
				  {

				  }
			}
			mySensorManager.registerListener(LightSensorListener, LightSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}

    }
    
    
    
    private void unregisterLightSensor()
    {
    	if(LightSensor != null){
			mySensorManager.unregisterListener(LightSensorListener,LightSensor);
			
			try {
				outwriterAllInfo.flush();
				outwriterAllInfo.close();
				foutAllInfo.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
    	}      	
	    lightValue.clear();
	    RValue.clear();
	    GValue.clear();
	    BValue.clear();
	    WValue.clear();
	    TValue.clear();
	    WifiValue.clear();
	   	
    }
    
    private void unregisterProxiSensor()
    {
    	
    	if(ProxiSensor != null){
			mySensorManager.unregisterListener(ProxiSensorListener,ProxiSensor);
			try {
				//outwriterProxi.flush();
				outwriterProxi.close();
				foutProxi.close();
			} catch (IOException e) {
					// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	} 
	
    }
    
    
    private void registerProxiSensor() {
    	
		if ( ProxiSensor != null){
			
			if(ProxiFile.exists()){
			  
				try{
	    		
					foutProxi = new FileOutputStream(ProxiFile,true);
					outwriterProxi = new OutputStreamWriter(foutProxi);
				} catch(Exception e)
				{
				}
			}
			mySensorManager.registerListener(ProxiSensorListener, ProxiSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}		
    }
        

    private void startEmitting(){
    	mediaPlayer.start();	
    }
    
    
    private void stopEmitting() {
    	mediaPlayer.pause();
    	mediaPlayer.release();
    	mediaPlayer = null;
    }   
    
   
    
    private void startRecording() {

		int min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		record = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT, min);
        record.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

        //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        int num = 0;
        short sData[] = new short[BufferElements2Rec];

        
        try {
        	foutSound = new FileOutputStream(SoundFile,true);
        	outwriterSound = new OutputStreamWriter(foutSound);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        try {
        	foutSoundData = new FileOutputStream(SoundDataFile);
        	outwriterSoundData = new OutputStreamWriter(foutSoundData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            num=record.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            if (num == BufferElements2Rec){
	            long curTime = System.currentTimeMillis();
	            String curTimeStr = ""+curTime+";   ";
	            Log.i(TAG,curTimeStr);
	            try {
	            	outwriterSound.append(curTimeStr);
	    		} catch (IOException e1) {
	    			// TODO Auto-generated catch block
	    			e1.printStackTrace();
	    		}
	        	String strI = ""+(sData.length);
	        	Log.i(TAG, strI);
	        	
	        	for ( int i=0; i<sData.length;i++){
	        		try {
	        			String tempS = Short.toString(sData[i])+"    ";
	        			outwriterSound.append(tempS);
	        			outwriterSoundData.append(tempS);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        	}
        		try {
        			String tempS = "\n";
        			outwriterSound.append(tempS);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
        	try {
        		outwriterSound.flush();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        	
        	try {
        		outwriterSoundData.flush();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            
            
        }
       
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != record) {
            isRecording = false;
            record.stop();
            record.release();
            record = null;

            recordingThread = null;
        }
    }
        
    private final SensorEventListener LightSensorListener= new SensorEventListener(){

    	@Override
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    		// TODO Auto-generated method stub
    	}
    	@Override
    	public void onSensorChanged(SensorEvent event) {
    		if(event.sensor.getType() == Sensor.TYPE_LIGHT){
    			
   			
    			double lightvalue = 0.0;
    			int Wifi_RSSI=0;
    			int UsefulFlag = 0;
    			int Detect_result = 0;
    			double Detect_confidence = 0.0;
    			String Str_Detect_result;
    			long timeSta = System.currentTimeMillis();
    			String curTimeStr = ""+timeSta+";   ";
    			Log.d("light sensor change:", curTimeStr);
    			int DaytimeFlag = 0;
    			Calendar rightNow = Calendar.getInstance();
				int Hours = rightNow.get(Calendar.HOUR_OF_DAY);
				int Minutes = rightNow.get(Calendar.MINUTE);
				Double CurrentTime = (double) Hours + (double) (Minutes/60);
				if ((CurrentTime > 7.5) && (CurrentTime < 20)){
					DaytimeFlag = 1;
				}
				
				
    			
    			if (lightValue.size()==5){
   				
    				RecordFlag =0;
    				double Light_Sum = 0;
    				double R_Sum = 0;
    				double G_Sum = 0;
    				double B_Sum = 0;
    				double W_Sum = 0;
    				double Wifi_Sum = 0;
    				for ( int i = 0; i<5; i++){
    					Light_Sum = Light_Sum + lightValue.get(i);
    					R_Sum = R_Sum + RValue.get(i);
    					G_Sum = G_Sum + GValue.get(i);
    					B_Sum = B_Sum + BValue.get(i);
    					W_Sum = W_Sum + WValue.get(i);
    					Wifi_Sum = Wifi_Sum + (double)WifiValue.get(i);	
    					
    				}
    				Light_Sum = Light_Sum/5;
    				R_Sum = R_Sum/5;
    				G_Sum = G_Sum/5;
    				B_Sum = B_Sum/5;
    				W_Sum = W_Sum/5;
    				Wifi_Sum = Wifi_Sum/5;
    				
    	            if (((Wifi_Sum<(-105))&& (Light_Sum < 3)) &&  ((Audio_start==0) && (EndingCallFlag !=3)))
    				//if ((Audio_start==0) && (EndingCallFlag !=3))
    	            {
    	            	Audio_start = 1;
//    	        		startRecording();
//    	        		try {
//    	        		    Thread.sleep(100);
//    	        		} catch (InterruptedException e) {
//    	        		    // TODO Auto-generated catch block
//    	        		    e.printStackTrace();
//    	        		}
//    	        		startEmitting();
//    	        		try {
//    	        		    Thread.sleep(800);
//    	        		} catch (InterruptedException e) {
//    	        		    // TODO Auto-generated catch block
//    	        		    e.printStackTrace();
//    	        		}
//    	        		stopEmitting();
//    	        		stopRecording();
    	                Audio_flag = 1;
    	            }
    				
    				String Light_RGB_Wifi = String.valueOf(Light_Sum)+" "+String.valueOf(R_Sum)+" "+String.valueOf(G_Sum)+" "+String.valueOf(B_Sum)+" "+String.valueOf(W_Sum)+" "+String.valueOf(Wifi_Sum);
    				   
    				Log.d("get Ave info","get Ave info");
    				Log.d("call type", String.valueOf(CallType));
             
                    sendFinalJSON(CallType,Light_RGB_Wifi,0);
                   
                    

                    
                    if (EndingCallFlag ==3){
                    	
                    	unregisterLightSensor();
                    	Log.d("unregister proximity", "unregister proximity"+String.valueOf(EndingCallFlag));
                    }
                    else{
                    	unregisterProxiSensor();
						unregisterLightSensor();
						Log.d("unregister light sensor", "unregister light"+String.valueOf(EndingCallFlag));
                    }
    			 					
    			}

    			
	            
	            lightvalue = (double) (event.values[0]);
	            Log.d(TAG, String.valueOf(lightvalue));
	            //lightValue.add(lightvalue);
				
    			SupplicantState supState; 
    			
    			wifiInfo = wifiManager.getConnectionInfo();
    			
    			supState = wifiInfo.getSupplicantState();

	            
	            Log.d(TAG, String.valueOf(wifiInfo.getRssi()));
	            Wifi_RSSI = wifiInfo.getRssi();
	            //WifiValue.add(Wifi_RSSI);
	       
    			try {
					bufferedReader = new BufferedReader(new FileReader(InputRGBFile));
				} catch (FileNotFoundException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
    			
    			StringBuilder finalString  = new StringBuilder();
    			
    			if (bufferedReader !=null)
    			{
    				String line;
    				try {
						while ((line = bufferedReader.readLine()) != null) {
							
							finalString.append(line);
							String[] tmp_RGB = line.split(",");
							int tmp_I = 3 - Integer.parseInt(tmp_RGB[5]);
							double tmp_time = Math.pow(4, tmp_I);
							double tmp_R = (Double.parseDouble(tmp_RGB[0])) * tmp_time;
							double tmp_G = (Double.parseDouble(tmp_RGB[1])) * tmp_time;
							double tmp_B = (Double.parseDouble(tmp_RGB[2])) * tmp_time;
							double tmp_W = (Double.parseDouble(tmp_RGB[3])) * tmp_time;
							
							if (lightvalue==0)
							{
								lightValue.add(lightvalue);
								WifiValue.add(Wifi_RSSI);
								RValue.add(tmp_R);
								GValue.add(tmp_G);
								BValue.add(tmp_B);
								WValue.add(tmp_W);
								Log.d(TAG, line);
								UsefulFlag = 1;
							}
							else
							{
								double flag = tmp_R / lightvalue;
								if (flag < 1) {
									lightValue.add(lightvalue);
									WifiValue.add(Wifi_RSSI);
									RValue.add(tmp_R);
									GValue.add(tmp_G);
									BValue.add(tmp_B);
									WValue.add(tmp_W);
									Log.d(TAG, line);
									UsefulFlag = 1;

								}
							}
							
							
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
				try {
					bufferedReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if ((UsefulFlag==1) && (RecordFlag==1)){
					String Light_RGB_Wifi = String.valueOf(lightvalue) +" "+finalString.toString()+" "+String.valueOf(Wifi_RSSI);
					Log.d("Light RGB wifi", Light_RGB_Wifi);
					writeJSON(outwriterAllInfo,timeSta,"rawData",Light_RGB_Wifi);
					
					Log.d("call add","calladd");
					sendJSON(CallType,timeSta,"rawData",Light_RGB_Wifi);

				}
				
    		}
    	}
    };
    
    private final SensorEventListener ProxiSensorListener= new SensorEventListener(){

    	@Override
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    		// TODO Auto-generated method stub
    
    	}

    	@Override
    	public void onSensorChanged(SensorEvent event) {
    		if(event.sensor.getType() == Sensor.TYPE_PROXIMITY){
    			
    			proxi_time = System.currentTimeMillis();
    			cur_proxi_time = proxi_time;
    			stop_proxi_time = proxi_time + 1500;
    			stop_proxi_time_end = cur_proxi_time + 1500;
    			stop_proxi_time_end2 = cur_proxi_time + 4000;
    			String curTimeStr = ""+proxi_time+";   ";
    			ReadProxi = event.values[0];
    			
    			Log.d("proxi count",String.valueOf(proxi_count));
    			Log.d("proxi value", String.valueOf(ReadProxi));
    			Log.d("proxi time",String.valueOf(proxi_time));
    			Log.d("cur proxi time",String.valueOf(cur_proxi_time));
    			Log.d("stop_proxi_time_end",String.valueOf(stop_proxi_time_end));
    			Log.d("stop_proxi_time",String.valueOf(stop_proxi_time));
    			Log.d("RecordFlag",String.valueOf(RecordFlag));
    			Log.d("Endingcallflag", String.valueOf(EndingCallFlag));
    			
    			if (ReadProxi<1)
    			{
    			
    			}
    			
        		if (ReadProxi>1)
        		{	
        			if (( EndingCallFlag == 1))
        			{
        				if (RecordFlag ==1){
            				if (proxiThread!= null)
            				{
            					if (!proxiThread.isInterrupted());
            					{
            						proxiThread.interrupt();
            						proxiThread = null;
            						Log.d("stop thread", "stop proxithread registed light sensor");     						
            					}
            				}
        					
        				}
						if (RecordFlag == 0) {
			    			if (proxi_thread_start ==0)
			    			{
			    				proxi_thread_start = 1;
			    				Log.d("start proxithread","first start proxithread");
			    				proxiThread = new Thread() {

			    					public void run() {
			    						while(!Thread.interrupted())
			    					    {
				    						while (true )
				    						{
				    							if ((cur_proxi_time > stop_proxi_time_end) && (ReadProxi>1))
				    							{
					    							Log.d("cur proxi time in EndingCallFlag 1", String.valueOf(cur_proxi_time));
					    							Log.d("stop_proxi_time end in EndingCallFlag 1", String.valueOf(stop_proxi_time_end));
					    							Log.d("readProxi",String.valueOf(ReadProxi));
				    								break;
				    							}
				    							cur_proxi_time = System.currentTimeMillis();
//				    							Log.d("cur proxi time in thread", String.valueOf(cur_proxi_time));
//				    							Log.d("stop_proxi_time in thread", String.valueOf(stop_proxi_time));
				    						
				    						}

				    						RecordFlag = 1;
				    						Log.d("RecordFlag in thread",String.valueOf(RecordFlag));
				    						if (register_light==0)
				    						{
				    							register_light = 1;
				    							Log.d("register light in first thread","register light in first thread");
				    							registerLightSensor();

				    						}
				    			

			    					    }

			    				
			    					}
			    				};
			    				proxiThread.start();
			    			}							
						}
        			}
        			

        			if (EndingCallFlag == 2)
        			{
        				if (RecordFlag ==1){
            				if (proxiThread!= null)
            				{
            					if (!proxiThread.isInterrupted());
            					{
            						proxiThread.interrupt();
            						proxiThread = null;
            						Log.d("stop thread", "stop proxithread registed light sensor");     						
            					}
            				}
        					
        				}
						if (RecordFlag == 0) {
			    			if (proxi_thread_start ==0)
			    			{
			    				proxi_thread_start = 1;
			    				Log.d("start proxithread","first start proxithread");
			    				proxiThread = new Thread() {

			    					public void run() {
			    						while(!Thread.interrupted())
			    					    {
				    						while (true )
				    						{
				    							if ((cur_proxi_time > stop_proxi_time) && (ReadProxi>1))
				    							{
					    							Log.d("cur proxi time in thread", String.valueOf(cur_proxi_time));
					    							Log.d("stop_proxi_time in thread", String.valueOf(stop_proxi_time));
					    							Log.d("readProxi",String.valueOf(ReadProxi));
				    								break;
				    							}
				    							cur_proxi_time = System.currentTimeMillis();
//				    							Log.d("cur proxi time in thread", String.valueOf(cur_proxi_time));
//				    							Log.d("stop_proxi_time in thread", String.valueOf(stop_proxi_time));
				    						
				    						}

				    						RecordFlag = 1;
				    						Log.d("RecordFlag in thread",String.valueOf(RecordFlag));
				    						if (register_light==0)
				    						{
				    							register_light = 1;
				    							Log.d("register light in first thread","register light in first thread");
				    							registerLightSensor();

				    						}
				    			

			    					    }

			    				
			    					}
			    				};
			    				proxiThread.start();
			    			}							
						}
        				
        			}	
        		}
        		
        		proxi_count = proxi_count + 1;
	            try {
	            	outwriterProxi.append(curTimeStr);
	    		} catch (IOException e1) {
	    			// TODO Auto-generated catch block
	    			e1.printStackTrace();
	    		}
	            try {
					outwriterProxi.append(String.valueOf(event.values[0])+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            try {
					outwriterProxi.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
    		}
    	}
     
    }; 
    

    
    
	public void writeJSON(OutputStreamWriter myWriter, long timestamp, String tag, String info) {
		JSONObject object = new JSONObject();
		try {

			object.put("timestamp", timestamp);
			object.put(tag, info);
			String content = object.toString() + "\n";
			myWriter.append(content);
			Log.d("write json", content);
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendJSON(int Type, long timestamp, String tag, String info){
		JSONObject tmp_object = new JSONObject();
		Log.d("send json", "send json start");
		try {
			tmp_object.put("timestamp", timestamp);
			tmp_object.put(tag, info);
			if (Type==1)
			{
				callStart.put("raw" + String.valueOf(raw_counter), tmp_object);
			}
			if (Type==2)
			{
				callEnd.put("raw" + String.valueOf(raw_counter), tmp_object);
			}
			Log.d("send json", tmp_object.toString());
			raw_counter = raw_counter + 1;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	

	public void sendFinalJSON(int Type,String info, int useAudio) {
		try {
			Log.d("start to send inent", "start to send inent");
			if (Type == 1)
			{
				Log.d("start to send call start", "start to send call start");
				callStart.put("AveValue", info);
				callStart.put("callType", 1);
				callStart.put("Audioflag", Audio_flag);

        		
				// Yuru added
				Intent it = new Intent();
				it.setAction("call_started");
				String tmp_intent_mes = callStart.toString();
				it.putExtra("callStart", tmp_intent_mes);
				it.setClass(context, MyService.class);
				context.startService(it);
            	Log.d("send call start", "start intent");
            	Log.d("send call start", tmp_intent_mes);
            	callStart = null;
            	callStart = new JSONObject();

//								
//            	String tmp_intent_mes = callStart.toString();
//            	
//            	Intent i = new Intent("broadCastName");
//                // Data you need to pass to activity
//            	i.putExtra("message", tmp_intent_mes); 
//            	
//            	context.sendBroadcast(i);

				
			}
			
        	if (Type ==2)
        	{
        		
        		
        		callEnd.put("AveValue", info);
        		callEnd.put("callType", 2);
        		callEnd.put("Audioflag", Audio_flag);
        		JSONObject tmp_object = new JSONObject();
        		try {
					tmp_object.put("deviceID", device_ID);
					tmp_object.put("incomingNum", incoming_number);
        		} catch (JSONException e) {
        			e.printStackTrace();
        		}
        		
        		callEnd.put("device_info", tmp_object);
        		
        		tmp_object= null;
        		
				// Yuru added
				Intent it = new Intent();
				it.setAction("call_ended");
				String tmp_intent_mes = callEnd.toString();
				it.putExtra("callEnd", tmp_intent_mes);
				it.setClass(context, MyService.class);
				context.startService(it);
				Log.d("send call end", "end intent");
      			Log.d("send call end", tmp_intent_mes);
      			callEnd = null;
      			callEnd = new JSONObject();
    			raw_counter = 0;
				
				
//				Intent i1 = new Intent(context, MainActivity.class);
//				i1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				i1.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//				context.startActivity(i1);
//                    
//            	Intent i = new Intent("broadCastName");
//                // Data you need to pass to activity
//              	
//      			JSONObject object1 = new JSONObject();
//      			object1.put("deviceID", device_ID);
//      			object1.put("incomingNum", incoming_number);
//      			callEnd.put("device_info", object1);
//
//            	
//            	String tmp_intent_mes = callEnd.toString();
//            	i.putExtra("message", tmp_intent_mes); 
//            	Log.d(TAG, "successfully intent");
//            	context.sendBroadcast(i);
      			
      			

            	
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
