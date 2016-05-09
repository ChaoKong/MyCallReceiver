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
import android.os.Build;
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
	private static final String TAG = "MyCallReceiverBroadcastReceiver";
	public Logger logger = new Logger(true,TAG);
	
	public static JSONObject callStart = null;
	public static JSONObject callEnd = null;
	public static int raw_counter = 0;

	
	private boolean isRecording = false;
	int sampleRate = 48000;
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
    private SharedPreferences.Editor spEditor;
    private SharedPreferences sp1_log;
    private SharedPreferences sp1_audio;

    
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
    
    //  0- default; 1- RGB; 2 - night predict ; 3 - > 5000; 4 - audio test; 5 - no light and wifi; 6 - daytime predict
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
    public Thread lightWaitThread = null;
    public int register_light = 0;
    public int proxi_thread_start = 0;
    public int proxi_thread_start2 = 0;
    public int light_thread_start = 0;
    public int send_final_json = 0;
    
    public long start_light_time = 0;
    public long stop_light_time = 0;
    public long cur_light_time = 0;
    
    public int CallType = 0;
    
    public int RGBAvailabe = 0;
    
    public int isS6 = 0;
    public int isG4 = 0;
    
    StringBuilder finalString = null;
    
    private String clear_command = "logcat -c -b main -b radio -b events\n";
    private String start_log_command ="";
    private Process log_process = null;
    
    private String log_state = "TRUE";  //1: logging
    
    private String audio_state = "TRUE";  //1: audio in use
    
    private int audio_state_flag = 1;
    
    
	

    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	
    	this.context = context;
    	event = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
    	incoming_number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
    	previus_state = getCallState(context);
        current_state = "IDLE";
		long cur_time = System.currentTimeMillis();
		
		root = android.os.Environment.getExternalStorageDirectory();
		dir = new File (root.getAbsolutePath() + "/CallDetection");
		dir.mkdirs();
		
        start_log_command = "logcat   -v threadtime -b main -f " + dir + File.separator
                + "MyCallReceiver_BroadcastReceiver_logcat"
                + ".txt";

        
        
        switch (event) {
        case "RINGING":
        	
            logger.d( "State : Ringing, incoming_number : " + incoming_number);

            
            if((previus_state.equals("IDLE")) || (previus_state.equals("FIRST_CALL_RINGING"))){
                current_state ="FIRST_CALL_RINGING";
            }
            if((previus_state.equals("OFFHOOK"))||(previus_state.equals("SECOND_CALL_RINGING"))){
                current_state = "SECOND_CALL_RINGING";
            }
			cur_time = System.currentTimeMillis();
			logger.d( String.valueOf(cur_time));
			logger.d( String.valueOf(cur_time));
            break;
        case "OFFHOOK":
            logger.d( "State : offhook, incoming_number : " + incoming_number);
            if((previus_state.equals("IDLE")) ||(previus_state.equals("FIRST_CALL_RINGING")) || previus_state.equals("OFFHOOK")){
                current_state = "OFFHOOK";
            }
            if(previus_state.equals("SECOND_CALL_RINGING")){
                current_state ="OFFHOOK";
            }
			cur_time = System.currentTimeMillis();
			logger.d( String.valueOf(cur_time));
            
            break;
        case "IDLE":
            logger.d( "State : idle and  incoming_number : " + incoming_number);
            if((previus_state.equals("OFFHOOK")) || (previus_state.equals("SECOND_CALL_RINGING")) || (previus_state.equals("IDLE"))){
                current_state="IDLE"; 
            }    
            if(previus_state.equals("FIRST_CALL_RINGING")){
                current_state = "IDLE";
            }
			cur_time = System.currentTimeMillis();
			logger.d( String.valueOf(cur_time));
            updateIncomingNumber("no_number",context);
            logger.d("stored incoming number flushed");
            break;
        }
        
        if(!current_state.equals(previus_state)){
        	logger.d( "Updating  state from "+previus_state +" to "+current_state);
        	//Toast.makeText(context, "Updating  state from "+previus_state +" to "+current_state, Toast.LENGTH_LONG).show();
        	updateCallState(current_state,context);
        	if(((previus_state.equals("OFFHOOK")) && (current_state.equals("IDLE")))){
        		//Toast.makeText(context, "Updating  state from "+previus_state +" to "+current_state +"       register sensor", Toast.LENGTH_LONG).show();
        		init(context);
				callEnd = new JSONObject();
				EndingCallFlag = 1;
				registerProxiSensor();
				logger.d( "call end");
				cur_time = System.currentTimeMillis();
				logger.d( String.valueOf(cur_time));
				logger.d( String.valueOf(EndingCallFlag));
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
				cur_time = System.currentTimeMillis();
				logger.d( String.valueOf(cur_time));
				logger.d( String.valueOf(EndingCallFlag));
        		CallType = 1;
        		logger.d( "outgoing call");

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
				cur_time = System.currentTimeMillis();
				logger.d( String.valueOf(cur_time));
				logger.d( String.valueOf(EndingCallFlag));
        		CallType = 1;
        		logger.d( "incoming call");
        		
        	}	
        }   	   	
    }
    
    
 
    public void updateCallState(String state,Context context){
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = sp.edit();
        spEditor.putString("call_state", state);
        spEditor.commit();
        logger.d( "state updated");

    }
    public void updateIncomingNumber(String inc_num,Context context){
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = sp.edit();
        spEditor.putString("inc_num", inc_num);
        spEditor.commit();
        logger.d( "incoming number updated");
    }
    public String getCallState(Context context){
        sp1 = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1.getString("call_state", "IDLE");
        logger.d("get previous state as :"+st);
        return st;
    }
    public String getIncomingNumber(Context context){
        sp1 = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1.getString("inc_num", "no_num");
        //Log.i(TAG,"get incoming number as :"+st);
        return st;
    } 
    

    public String getLogState(Context context){
        sp1_log = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1_log.getString("log_state", "TRUE");
        logger.d("get log state in broadcastreceiver :"+st);
        return st;
    }
    
    
    public String getAudioState(Context context){
        sp1_audio = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1_audio.getString("audio_state", "TRUE");
        logger.d("get audio state as :"+st);
        return st;
    }
    

   
    
    private void init(Context context){
    	    	

        log_state  = getLogState(context);
        logger.d("getlogstate in broadcastreceiver:   "+log_state); 
        if (log_state.equals("TRUE"))
        {
        	try {
				Runtime.getRuntime().exec(clear_command);
				logger.d(clear_command);
				log_process = Runtime.getRuntime().exec(start_log_command);
				logger.d("log process:"+log_process.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
        }
        
        audio_state = getAudioState(context);
        
        if (audio_state.equals("TRUE"))
        {
        	audio_state_flag = 1;
        	logger.d("audio state flag true: "+String.valueOf(audio_state_flag));
        }
        if (audio_state.equals("FALSE"))
        {
        	audio_state_flag = 0;
        	logger.d("audio state flag false: "+String.valueOf(audio_state_flag));
        }
        
        
		
		
		am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		mediaPlayer = MediaPlayer.create(context, R.raw.chirp814);
		//mediaPlayer.setVolume(0.5f, 0.5f);
		InputRGBFile = new File("/sys/devices/virtual/sensors/light_sensor/raw_data");
		
		
		RGBAvailabe = 0;
		if (InputRGBFile.exists())
		{
			//RGBAvailabe = 1;
			try {
				bufferedReader = new BufferedReader(new FileReader(InputRGBFile));
				RGBAvailabe = 1;
			} catch (FileNotFoundException e2) {
				// TODO Auto-generated catch block
				RGBAvailabe = 0;
				logger.d("try to read RGB, unavailable");
				e2.printStackTrace();
				
			}
			logger.d( "RGB available");
			
		}
		
		
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
    	light_thread_start = 0;
    	lightWaitThread = null;
    	send_final_json = 0;
    	
    	callStart = null;
    	callEnd = null;
    	
       	String deviceModel = Build.MODEL;
       	logger.d(deviceModel);
    	String S6 = "g920t";
    	String G4 = "lg";
    	if (deviceModel.toLowerCase().contains(S6.toLowerCase())){
    		isS6 =1;
    	}
    	
    	if (deviceModel.toLowerCase().contains(G4.toLowerCase())){
    		isG4 =1;
    	}
    	
    	
		
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
	    logger.d( "create array "+String.valueOf(curTime));
    	
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
				logger.d( "stop proxi thread in register light");
				
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
    	am.setStreamVolume(AudioManager.STREAM_MUSIC,10,0);
    	mediaPlayer.setVolume(0.6f, 0.6f);
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
	            //Log.i(TAG,curTimeStr);
	            try {
	            	outwriterSound.append(curTimeStr);
	    		} catch (IOException e1) {
	    			// TODO Auto-generated catch block
	    			e1.printStackTrace();
	    		}
	        	String strI = ""+(sData.length);
	        	//Log.i(TAG, strI);
	        	
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
    			long timeSta = System.currentTimeMillis();
    			String curTimeStr = ""+timeSta+";   ";
    			logger.d( curTimeStr);
    			
                if (EndingCallFlag != 3){
                	
                	unregisterProxiSensor();
                	logger.d( "unregister proximity"+String.valueOf(EndingCallFlag));
                }
    			
    			if (lightValue.size()==0)
    			{
    				start_light_time = timeSta;
    				stop_light_time = timeSta + 800;
    			}
    			

    			if ((light_thread_start ==0) && (isG4==1))
    			{
    				light_thread_start = 1;
    				logger.d("first start light");
					logger.d( String.valueOf(start_light_time) + "  start light time in light thread");
					logger.d( String.valueOf(stop_light_time)+ " stop light timein light thread");
    				lightWaitThread = new Thread() {

    					public void run() {
    						while(!Thread.interrupted())
    					    {
	    						while (true )
	    						{
	    							if ((cur_light_time > stop_light_time) || (send_final_json==1))
	    							{
		    							logger.d( String.valueOf(cur_light_time) + " in light thread");
		    							logger.d( String.valueOf(stop_light_time)+ " in light thread");

	    								break;
	    							}
	    							cur_light_time = System.currentTimeMillis();	    						
	    						}

	    						if (send_final_json !=1)
	    						{
	    							unregisterLightSensor();
	    							logger.d( "unregister light"+String.valueOf(EndingCallFlag) +"   in light thread");
	    							logger.d( "light size		"+String.valueOf(lightValue.size()));

	    							if (lightValue.size()==0)
	    							{	
	    			    				String Light_RGB_Wifi = String.valueOf(0)+" "+String.valueOf(0)+" "+String.valueOf(0)+" "+String.valueOf(0)+" "+String.valueOf(0)+" "+String.valueOf(-127);
	    			    				   
	    			    				logger.d("get Ave info in light thread, all 0");
	    			    				logger.d( String.valueOf(CallType));
	    			             
	    			                    sendFinalJSON(CallType,Light_RGB_Wifi);
	    								
	    							}
	    							else{
	    			    				double Light_Sum = 0;
	    			    				double R_Sum = 0;
	    			    				double G_Sum = 0;
	    			    				double B_Sum = 0;
	    			    				double W_Sum = 0;
	    			    				double Wifi_Sum = 0;
	    								int tmp_size = lightValue.size();
	    								logger.d( String.valueOf(tmp_size)+"light value size");
	    			    				for ( int i = 0; i<tmp_size; i++){
	    			    					Light_Sum = Light_Sum + lightValue.get(i);
	    			    					R_Sum = R_Sum + RValue.get(i);
	    			    					G_Sum = G_Sum + GValue.get(i);
	    			    					B_Sum = B_Sum + BValue.get(i);
	    			    					W_Sum = W_Sum + WValue.get(i);
	    			    					Wifi_Sum = Wifi_Sum + (double)WifiValue.get(i);	
	    			    					
	    			    				}
	    			    				Light_Sum = Light_Sum/tmp_size;
	    			    				R_Sum = R_Sum/tmp_size;
	    			    				G_Sum = G_Sum/tmp_size;
	    			    				B_Sum = B_Sum/tmp_size;
	    			    				W_Sum = W_Sum/tmp_size;
	    			    				Wifi_Sum = Wifi_Sum/tmp_size;
	    			    				String Light_RGB_Wifi = String.valueOf(Light_Sum)+" "+String.valueOf(R_Sum)+" "+String.valueOf(G_Sum)+" "+String.valueOf(B_Sum)+" "+String.valueOf(W_Sum)+" "+String.valueOf(Wifi_Sum);
	    			    				   
	    			    				logger.d("get Ave info in light thread");
	    			    				logger.d( String.valueOf(CallType));
	    			    				
	    			    				
	    			                    sendFinalJSON(CallType,Light_RGB_Wifi);
	    							}
	    						}
	    						logger.d( "interrupte light thread");
	    						Thread.currentThread().interrupt();
	    						return;
    					    }

    				
    					}
    				};
    				lightWaitThread.start();
    			}				
				
    			
    			if (lightValue.size()==5){
   				
                    
                    unregisterLightSensor();
                    logger.d("unregister light"+String.valueOf(EndingCallFlag));
                    
    				logger.d( "light value size is 5");
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
    				
    				logger.d( String.valueOf(isS6));
    				
    	            if ( ((isS6 ==1)&& (audio_state_flag ==1)) &&  ((Audio_start==0) && (EndingCallFlag ==1)) )
    				//if (((Audio_start==0) && (EndingCallFlag ==1)) && (isS6 ==1) )
    	            {
    	            	Audio_start = 1;
    	        		startRecording();
    	        		try {
    	        		    Thread.sleep(80);
    	        		} catch (InterruptedException e) {
    	        		    // TODO Auto-generated catch block
    	        		    e.printStackTrace();
    	        		}
    	        		startEmitting();
    	        		try {
    	        		    Thread.sleep(750);
    	        		} catch (InterruptedException e) {
    	        		    // TODO Auto-generated catch block
    	        		    e.printStackTrace();
    	        		}
 
    	        		stopEmitting();
    	        		stopRecording();
    	                Audio_flag = 1;
    	            }
    				
    				String Light_RGB_Wifi = String.valueOf(Light_Sum)+" "+String.valueOf(R_Sum)+" "+String.valueOf(G_Sum)+" "+String.valueOf(B_Sum)+" "+String.valueOf(W_Sum)+" "+String.valueOf(Wifi_Sum);
    				   
    				logger.d("get Ave info");
    				logger.d( String.valueOf(CallType));
             
                    sendFinalJSON(CallType,Light_RGB_Wifi);
                   

 					
    			}

    			
	            
	            lightvalue = (double) (event.values[0]);
	            logger.d( String.valueOf(lightvalue));
	            //lightValue.add(lightvalue);
				
    			SupplicantState supState; 
    			
    			wifiInfo = wifiManager.getConnectionInfo();
    			
    			supState = wifiInfo.getSupplicantState();

	            
	            logger.d( String.valueOf(wifiInfo.getRssi()));
	            Wifi_RSSI = wifiInfo.getRssi();
	            finalString = new StringBuilder();
	            //WifiValue.add(Wifi_RSSI);
	            if (RGBAvailabe==1) {
					try {
						bufferedReader = new BufferedReader(new FileReader(InputRGBFile));
					} catch (FileNotFoundException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}

					

					if (bufferedReader != null) {
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

								
								lightValue.add(lightvalue);
								WifiValue.add(Wifi_RSSI);
								RValue.add(tmp_R);
								GValue.add(tmp_G);
								BValue.add(tmp_B);
								WValue.add(tmp_W);
								logger.d( line);

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
	            }
	            else{
					lightValue.add(lightvalue);
					WifiValue.add(Wifi_RSSI);
					RValue.add(0.0);
					GValue.add(0.0);
					BValue.add(0.0);
					WValue.add(0.0);
				
	            }
	            
	            logger.d( String.valueOf(lightValue.size()) + "ligth size");

				if   (RecordFlag==1){
					
					String Light_RGB_Wifi = String.valueOf(lightvalue) +" "+finalString.toString()+" "+String.valueOf(Wifi_RSSI);
					logger.d( Light_RGB_Wifi);
					writeJSON(outwriterAllInfo,timeSta,"rawData",Light_RGB_Wifi);
					
					logger.d("calladd");
					sendJSON(CallType,timeSta,"rawData",Light_RGB_Wifi);
					finalString = null;

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
    			stop_proxi_time_end = cur_proxi_time + 1000;
    			String curTimeStr = ""+proxi_time+";   ";
    			ReadProxi = event.values[0];
    			
//    			Log.d(TAG,String.valueOf(proxi_count));
//    			Log.d(TAG, String.valueOf(ReadProxi));
//    			Log.d(TAG,String.valueOf(proxi_time));
//    			Log.d(TAG,String.valueOf(cur_proxi_time));
//    			Log.d(TAG,String.valueOf(stop_proxi_time_end));
//    			Log.d(TAG,String.valueOf(stop_proxi_time));
//    			Log.d(TAG,String.valueOf(RecordFlag));
//    			Log.d(TAG, String.valueOf(EndingCallFlag));
//    			Log.d(TAG,"start to read proxi");
    			
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
            						logger.d("stop proxithread registed light sensor");     						
            					}
            				}
        					
        				}
						if (RecordFlag == 0) {
			    			if (proxi_thread_start ==0)
			    			{
			    				proxi_thread_start = 1;
			    				logger.d("first start proxithread");
			    				proxiThread = new Thread() {

			    					public void run() {
			    						while(!Thread.interrupted())
			    					    {
			    							logger.d( "start to do while");
				    						while (true )
				    						{
				    							if ((cur_proxi_time > stop_proxi_time_end) && (ReadProxi>1))
				    							{
//					    							Log.d(TAG, String.valueOf(cur_proxi_time));
//					    							Log.d(TAG, String.valueOf(stop_proxi_time_end));
//					    							Log.d(TAG,String.valueOf(ReadProxi));
//					    							Log.d(TAG,"cannot stop");
				    								break;
				    							}
				    							cur_proxi_time = System.currentTimeMillis();
//				    							Log.d(TAG, String.valueOf(cur_proxi_time)+"STOP");
//				    							Log.d(TAG, String.valueOf(stop_proxi_time)+"STOP");
				    						
				    						}

				    						RecordFlag = 1;
//				    						Log.d(TAG, "thread while break");
//				    						Log.d(TAG,String.valueOf(RecordFlag));
				    						if (register_light==0)
				    						{
				    							register_light = 1;
				    							logger.d("register light in first thread");
				    							registerLightSensor();

				    						}
				    						
				    						logger.d( "interrupte thread");
				    						Thread.currentThread().interrupt();
				    						return;
				    			

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
            						logger.d( "stop proxithread registed light sensor");     						
            					}
            				}
        					
        				}
						if (RecordFlag == 0) {
			    			if (proxi_thread_start ==0)
			    			{
			    				proxi_thread_start = 1;
			    				logger.d("first start proxithread");
			    				proxiThread = new Thread() {

			    					public void run() {
			    						while(!Thread.interrupted())
			    					    {
				    						while (true )
				    						{
				    							if ((cur_proxi_time > stop_proxi_time) && (ReadProxi>1))
				    							{
//					    							Log.d(TAG, String.valueOf(cur_proxi_time));
//					    							Log.d(TAG, String.valueOf(stop_proxi_time));
//					    							Log.d(TAG,String.valueOf(ReadProxi));
//					    							Log.d(TAG,"cannot stop");
				    								break;
				    							}
				    							cur_proxi_time = System.currentTimeMillis();
//				    							Log.d(TAG, String.valueOf(cur_proxi_time)+"STOP");
//				    							Log.d(TAG, String.valueOf(stop_proxi_time)+"STOP");
//				    							Log.d("cur proxi time in thread", String.valueOf(cur_proxi_time));
//				    							Log.d("stop_proxi_time in thread", String.valueOf(stop_proxi_time));
				    						
				    						}

				    						RecordFlag = 1;
				    						//Log.d("RecordFlag in thread",String.valueOf(RecordFlag));
				    						if (register_light==0)
				    						{
				    							register_light = 1;
				    							logger.d("register light in first thread");
				    							registerLightSensor();

				    						}
				    						
				    						logger.d( "interrupte thread");
				    						Thread.currentThread().interrupt();
				    						return;
				    			

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
			logger.d( content);
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendJSON(int Type, long timestamp, String tag, String info){
		JSONObject tmp_object = new JSONObject();
		logger.d( "write json start");
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
			logger.d( tmp_object.toString());
			raw_counter = raw_counter + 1;
			logger.d( String.valueOf(raw_counter));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	

	public void sendFinalJSON(int Type,String info) {
		try {
			//Log.d("start to send inent", "start to send inent");
			

			if (Type == 1)
			{
				logger.d( "start to send call start");
				callStart.put("AveValue", info);
				callStart.put("callType", 1);
				callStart.put("Audioflag", Audio_flag);
				//Log.d("RGBAvailable", String.valueOf(RGBAvailabe));
				callStart.put("RGBAvailable", RGBAvailabe);

        		
				// Yuru added
				Intent it = new Intent();
				it.setAction("call_started");
				String tmp_intent_mes = callStart.toString();
				it.putExtra("callStart", tmp_intent_mes);
				it.setClass(context, MyService.class);
				context.startService(it);
            	logger.d( "start intent");
            	logger.d(tmp_intent_mes);
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
        		logger.d( String.valueOf(RGBAvailabe));
        		callEnd.put("RGBAvailable", RGBAvailabe);
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
				logger.d( "send intent");
      			logger.d( tmp_intent_mes);
      			callEnd = null;
      			callEnd = new JSONObject();
    			raw_counter = 0;
    		    lightValue.clear();
    		    RValue.clear();
    		    GValue.clear();
    		    BValue.clear();
    		    WValue.clear();
    		    TValue.clear();
    		    WifiValue.clear();
				
            	
        	}
        	send_final_json = 1;
        	logger.d( String.valueOf(send_final_json) + "			sent final json");
			if (log_process != null)
			{
				logger.d("destroy log process in broadcastReceiver: "+log_process.toString());
				log_process.destroy();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
