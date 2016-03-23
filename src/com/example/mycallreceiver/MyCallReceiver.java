package com.example.mycallreceiver;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class MyCallReceiver extends BroadcastReceiver {
	
	static{
		System.loadLibrary("jnilibsvm");
	}
	
	public native void jniSvmTrain(String cmd);
	public native void jniSvmPredict(String cmd);
	public native void jniSvmScale(String cmd);
	private static final String TAG = "Debug";

	
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
	
	
	File SoundFile = null;
	FileOutputStream foutSound = null;
	OutputStreamWriter outwriterSound = null;
	
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
	
	File TestInputFile = null;
	FileOutputStream foutTestInput = null;
	OutputStreamWriter outwriterTestInput = null;
	
	
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
    private int EndingCallFlag =0;
    
    public String Cmd_svm_scale;
    public String Cmd_svm_predict;
    
    
    
    
	

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
        		EndingCallFlag = 1;
        		registerProxiSensor(); 
    			long curTime = System.currentTimeMillis();
    			String curTimeStr = ""+curTime+";   ";
    			Log.d(TAG, curTimeStr);
        		
        	}
        	if (((previus_state.equals("IDLE")) && (current_state.equals("OFFHOOK"))))	
        	{
        		init(context);
        		EndingCallFlag = 0;
        		registerLightSensor2();
    			long curTime = System.currentTimeMillis();
    			String curTimeStr = ""+curTime+";   ";
    			Log.d(TAG, curTimeStr);
        	}
        	
        	if ((previus_state.equals("IDLE")) && (current_state.equals("FIRST_CALL_RINGING")))
        	{
        		init(context);
        		EndingCallFlag = 0;
        		registerLightSensor2();
    			long curTime = System.currentTimeMillis();
    			String curTimeStr = ""+curTime+";   ";
    			Log.d(TAG, curTimeStr);
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
		
		
//		am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//		mediaPlayer = MediaPlayer.create(context, R.raw.chirp11);
//		mediaPlayer.setVolume(0.5f, 0.5f);
		InputRGBFile = new File("/sys/devices/virtual/sensors/light_sensor/raw_data");
		

		
		
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        
        mySensorManager = (SensorManager)context.getSystemService(context.SENSOR_SERVICE);
        
        LightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    	
    	
		SoundFile = new File(dir, "SoundRecord.txt");
    	if (!SoundFile.exists()){
    		try {
    			SoundFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	LightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

//		if (LightSensor != null) {
//			// textLIGHT_available.setText("Sensor LIGHT Available");
//			// mySensorManager.registerListener(LightSensorListener,
//			// LightSensor, SensorManager.SENSOR_DELAY_NORMAL);
//			LightFile = new File(dir, "LightRecord.txt");
//			RGBFile = new File(dir, "RGB.txt");
//			if (!LightFile.exists()) {
//				try {
//					LightFile.createNewFile();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			if (!RGBFile.exists()) {
//				try {
//					RGBFile.createNewFile();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		} else {
//		}

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
		
//        WifiFile = new File(dir,"WifiRecord.txt");
//    	if (!WifiFile.exists()){
//    		try {
//    			WifiFile.createNewFile();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
    	
    	
    	AllInfoFile = new File(dir,"AllInfoRecord.txt");
    	if (!AllInfoFile.exists()){
    		try {
    			AllInfoFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	
    	TestInputFile = new File(dir,"TestInput");
    	if (!TestInputFile.exists()){
    		try {
    			TestInputFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
//    	ResultFile = new File(dir,"ResultRecord.txt");
//    	if (!ResultFile.exists()){
//    		try {
//    			ResultFile.createNewFile();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
    	
    	Log.i(TAG, "init successfully");

    	
    }
    
    private void registerLightSensor() {
    	
		if(LightSensor != null){
			
//			if(LightFile.exists()){
//				try{
//					foutLight = new FileOutputStream(LightFile,true);
//					outwriterLight = new OutputStreamWriter(foutLight);
//				} catch(Exception e)
//				{
//				}
//			}	
//			if(WifiFile.exists()){
//				  
//				  try{
//		    		
//					  foutWifi = new FileOutputStream(WifiFile,true);
//					  outwriterWifi= new OutputStreamWriter(foutWifi);
//				
//				  } catch(Exception e)
//				  {
//
//				  }
//			}
//			if(RGBFile.exists()){
//				  
//				  try{
//		    		
//					  foutRGB = new FileOutputStream(RGBFile,true);
//					  outwriterRGB= new OutputStreamWriter(foutRGB);
//				
//				  } catch(Exception e)
//				  {
//
//				  }
//			}
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
	    lightValue = new ArrayList<Double>();
	    RValue = new ArrayList<Double>();
	    GValue = new ArrayList<Double>();
	    BValue = new ArrayList<Double>();
	    WValue = new ArrayList<Double>();
	    TValue = new ArrayList<Integer>();
	    WifiValue = new ArrayList<Integer>();	
	    	    
	    
    }
    
    
    private void registerLightSensor2() {
    	
		if(LightSensor != null){
			
//			if(LightFile.exists()){
//				try{
//					foutLight = new FileOutputStream(LightFile,true);
//					outwriterLight = new OutputStreamWriter(foutLight);
//				} catch(Exception e)
//				{
//				}
//			}	
//			if(WifiFile.exists()){
//				  
//				  try{
//		    		
//					  foutWifi = new FileOutputStream(WifiFile,true);
//					  outwriterWifi= new OutputStreamWriter(foutWifi);
//				
//				  } catch(Exception e)
//				  {
//
//				  }
//			}
//			if(RGBFile.exists()){
//				  
//				  try{
//		    		
//					  foutRGB = new FileOutputStream(RGBFile,true);
//					  outwriterRGB= new OutputStreamWriter(foutRGB);
//				
//				  } catch(Exception e)
//				  {
//
//				  }
//			}
			
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
	    lightValue = new ArrayList<Double>();
	    RValue = new ArrayList<Double>();
	    GValue = new ArrayList<Double>();
	    BValue = new ArrayList<Double>();
	    WValue = new ArrayList<Double>();
	    TValue = new ArrayList<Integer>();
	    WifiValue = new ArrayList<Integer>();	
    }
    
    
    
    private void unregisterLightSensor()
    {
    	if(LightSensor != null){
			mySensorManager.unregisterListener(LightSensorListener,LightSensor);
//			try {
//				outwriterLight.flush();
//				outwriterLight.close();
//				foutLight.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			try {
//				outwriterWifi.flush();
//				outwriterWifi.close();
//				foutWifi.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			try {
//				outwriterRGB.flush();
//				outwriterRGB.close();
//				foutRGB.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
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
			mySensorManager.registerListener(ProxiSensorListener, ProxiSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}		
    }
    
    

    
    
   
    
//    private void startRecording() {
//
//		int min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
//		record = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
//				AudioFormat.ENCODING_PCM_16BIT, min);
//        record.startRecording();
//        isRecording = true;
//        recordingThread = new Thread(new Runnable() {
//            public void run() {
//                writeAudioDataToFile();
//            }
//        }, "AudioRecorder Thread");
//        recordingThread.start();
//    }

        //convert short to byte
//    private byte[] short2byte(short[] sData) {
//        int shortArrsize = sData.length;
//        byte[] bytes = new byte[shortArrsize * 2];
//        for (int i = 0; i < shortArrsize; i++) {
//            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
//            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
//            sData[i] = 0;
//        }
//        return bytes;
//
//    }
//
//    private void writeAudioDataToFile() {
//        // Write the output audio in byte
//        int num = 0;
//        short sData[] = new short[BufferElements2Rec];
//
//        
//        try {
//        	foutSound = new FileOutputStream(SoundFile,true);
//        	outwriterSound = new OutputStreamWriter(foutSound);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        while (isRecording) {
//            // gets the voice output from microphone to byte format
//
//            num=record.read(sData, 0, BufferElements2Rec);
//            System.out.println("Short wirting to file" + sData.toString());
//            if (num == BufferElements2Rec){
//	            long curTime = System.currentTimeMillis();
//	            String curTimeStr = ""+curTime+";   ";
//	            Log.d(TAG,curTimeStr);
//	            try {
//	            	outwriterSound.append(curTimeStr);
//	    		} catch (IOException e1) {
//	    			// TODO Auto-generated catch block
//	    			e1.printStackTrace();
//	    		}
//	        	String strI = ""+(sData.length);
//	        	Log.d(TAG, strI);
//	        	
//	        	for ( int i=0; i<sData.length;i++){
//	        		try {
//	        			String tempS = Short.toString(sData[i])+"    ";
//	        			outwriterSound.append(tempS);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//	        	}
//        		try {
//        			String tempS = "\n";
//        			outwriterSound.append(tempS);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//            }
//            
//        	try {
//        		outwriterSound.flush();
//    		} catch (IOException e) {
//    			// TODO Auto-generated catch block
//    			e.printStackTrace();
//    		}
//            
//            
//        }
//       
//    }
//
//    private void stopRecording() {
//        // stops the recording activity
//        if (null != record) {
//            isRecording = false;
//            record.stop();
//            record.release();
//            record = null;
//
//            recordingThread = null;
//        }
//    }
    
    private String svmPredictResult(double Light_Sum, double R_Sum, double G_Sum, double B_Sum, double W_Sum){
		double r_to_b = R_Sum/B_Sum;
		double g_to_b = G_Sum/B_Sum;
		double r_to_g = R_Sum/G_Sum;
		double r_to_w = R_Sum/W_Sum;
		double g_to_w = G_Sum/W_Sum;
		double b_to_w = B_Sum/W_Sum;
		double r_to_l = R_Sum/Light_Sum;
		double g_to_l = G_Sum/Light_Sum;
		double b_to_l = B_Sum/Light_Sum;
		double w_to_l = W_Sum/Light_Sum;	
	
	
		String Str_test_input = "0"+" "+"1:"+Double.toString(Light_Sum)+" "+
				"2:"+Double.toString(r_to_b)+" "+"3:"+Double.toString(g_to_b)+" "+"4:"+Double.toString(r_to_g)+" "+
				"5:"+Double.toString(r_to_w)+" "+"6:"+Double.toString(g_to_w)+" "+"7:"+Double.toString(b_to_w)+" "+
				"8:"+Double.toString(r_to_l)+" "+"9:"+Double.toString(g_to_l)+" "+"10:"+Double.toString(b_to_l)+" "+"11:"+Double.toString(w_to_l)+"\n";
		if(TestInputFile.exists()){
			  
			  try{
	    		
				  foutTestInput = new FileOutputStream(TestInputFile,false);
				  outwriterTestInput= new OutputStreamWriter(foutTestInput);
			
			  } catch(Exception e)
			  {

			  }
		}
		
		try {
			outwriterTestInput.append(Str_test_input);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			outwriterTestInput.flush();
			outwriterTestInput.close();
			foutTestInput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String Str_dir = dir + "/";
		String Str_model = Str_dir+"model";
		String Str_range = Str_dir+"range";
		String Str_test = Str_dir + "TestInput";
		String Str_scale = Str_dir+"TestInput_scale";
		String Str_result = Str_dir + "Detect_result";
		
		Cmd_svm_scale = "-r "+Str_range+" "+Str_test+" "+Str_scale;
		Cmd_svm_predict = "-b 1 "+Str_scale+" "+Str_model+" "+Str_result;
		jniSvmScale(Cmd_svm_scale);
		jniSvmPredict(Cmd_svm_predict);
		
		File SvmResultFile = new File(Str_result);
		BufferedReader bufferedReader_svm = null;
		try {
			bufferedReader_svm = new BufferedReader(new FileReader(SvmResultFile));
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		StringBuilder finalString = new StringBuilder();

		if (bufferedReader_svm != null) {
			String line;
			try {
				int count = 0;
				while ((line = bufferedReader_svm.readLine()) != null) {
					if (count==0){
						count = 1;
						continue;
					}
					finalString.append(line);
					Log.d(TAG, line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			bufferedReader_svm.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String Str_Result = finalString.toString();
		return Str_Result;  	   	
    }
    
//    private void WriteResult(String str_detect_result){
//    	
//		if(ResultFile.exists()){
//			  
//			  try{
//	    		
//				  foutResult = new FileOutputStream(ResultFile,true);
//				  outwriterResult= new OutputStreamWriter(foutResult);
//			
//			  } catch(Exception e)
//			  {
//
//			  }
//		}
//		
//		try {
//			outwriterResult.append((str_detect_result+"\n"));
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		try {
//			outwriterResult.flush();
//			outwriterResult.close();
//			foutResult.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    	    	
//    }
    
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
    			Log.d(TAG, curTimeStr);
    			
    			
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
    				
    				if ((B_Sum <3) || (G_Sum < 3)){
    					Detect_result = 2;
    					Detect_confidence = 0.0;
    				}
    				else{
    					Str_Detect_result = svmPredictResult(Light_Sum,R_Sum,G_Sum,B_Sum,W_Sum);
    					Log.d(TAG, Str_Detect_result);
    					writeJSON(outwriterAllInfo,timeSta,"Result",Str_Detect_result);			
    				}		
    				
    				if (EndingCallFlag == 1)
    				{
    					unregisterLightSensor();
    					unregisterProxiSensor();
    					
    				}
    				if (EndingCallFlag ==0)
    				{
    					unregisterLightSensor();	
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
							
							double flag = tmp_R/lightvalue;
							if (flag < 1)
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
				if (UsefulFlag==1){
					String Light_RGB_Wifi = String.valueOf(lightvalue) +" "+finalString.toString()+" "+String.valueOf(Wifi_RSSI);
					writeJSON(outwriterAllInfo,timeSta,"rawData",Light_RGB_Wifi);	
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
    			long timeSta = System.currentTimeMillis();
    			String curTimeStr = ""+timeSta+";   ";
    			ReadProxi = event.values[0];
    			Log.d(TAG, String.valueOf(ReadProxi));
        		if (ReadProxi>1)
        		{	
        			if (RecordFlag == 0)
        			{
        				registerLightSensor();
        				RecordFlag = 1;
        			}	
        		}
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
			Log.i(TAG, content);

		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}  
}
