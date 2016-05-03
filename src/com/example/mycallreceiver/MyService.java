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
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

public class MyService extends IntentService implements
ConnectionCallbacks, OnConnectionFailedListener,LocationListener {
	
	
	static{
		System.loadLibrary("jnilibsvm");
	}
	
	public native void jniSvmPredict(String cmd);
	public native void jniSvmScale(String cmd);
	public native void processAudio(String cmd);
	
	
	private static final String TAG = "MyCallReceiverService";
	public Logger logger = new Logger(true,TAG);
	
	File root = null;
	File dir = null;
	JSONObject Ground_truth = null;
	JSONObject tmp_truth = null;
	JSONObject tmp_call = null;
	
    public int calculate_mode1 = 0;
    public int calculate_mode2 = 0;
    public int calculate_mode = 0;
    
    //  0- default; 1- RGB; 2 - night predict ; 3 - > 5000; 4 - audio test; 5 - no light and wifi; 6 - no RGB, daytime with ligth value and wifi value;
    
    public String Cmd_svm_scale;
    public String Cmd_svm_predict;
    public String Cmd_get_feature;
    
	File TestInputFile = null;
	FileOutputStream foutTestInput = null;
	OutputStreamWriter outwriterTestInput = null;
	
	File GroundTruthFile = null;
	FileOutputStream foutGroundtruth = null;
	OutputStreamWriter outwriterGroundtruth = null;
	
	
	File ResultFile = null;
	FileOutputStream foutResult = null;
	OutputStreamWriter outwriterResult = null;
	
	File tmpCallStartFile = null;
	
	File CallStartLocationFile = null;
	
	String CallStartLocationFile_Str = "";

	
	String tmpCallStartFile_str="";


	
	File SoundFile = null;
	FileOutputStream foutSound = null;
	OutputStreamWriter outwriterSound = null;
	
	
	File SoundDataFile = null;
	FileOutputStream foutSoundData = null;
	OutputStreamWriter outwriterSoundData = null;
	
	
	String result1_Str = "call start result";
	int result1 = 0;
	double result1_con = 0.0;
	String result2_Str = "call end result";
	int result2 = 0;
	double result2_con = 0.0;
	
	public int audio_in_use = 0;
	public double Light_Sum = 0;
	public double R_Sum = 0;
	public double G_Sum = 0;
	public double B_Sum = 0;
	public double W_Sum = 0;
	public double Wifi_Sum = 0;
	
	public int callType = 0;
	
    private Context context;
    
	GoogleApiClient mGoogleApiClient;
	LocationRequest mLocationRequest;
	Location mLocation;
	Location mLastLocation;
	int LocationFlag = 0;
	int StartTransmit = 0;
	String location_global ="";
	JSONObject Location_info = null;

	public Thread LocationThread = null;
	public int LocationThreadstart = 0;
	
    public long start_location_time = 0;
    public long stop_location_time = 0;
    public String location_tmp;
    
    JSONObject location1 = null;
    public int RGBAvailable = 0;
    
    private SharedPreferences sp1_log;
    
    
    private String clear_command = "logcat -c -b main -b radio -b events\n";
    private String start_log_command ="";
    private Process log_process = null;
    
    private String log_state = "TRUE";  //1: logging
    
    private SharedPreferences sp;
    private SharedPreferences.Editor spEditor;

	public MyService() {
		super("MyService");
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public void onCreate() {
	    super.onCreate();
	    
	    context = this.getApplicationContext();
	    root = android.os.Environment.getExternalStorageDirectory();
		dir = new File (root.getAbsolutePath() + "/CallDetection");
		if (dir == null){
			logger.d( dir.toString());
			
		}
		dir.mkdirs();
		
        start_log_command = "logcat   -v threadtime -b main -f " + dir + File.separator
                + "MycallReceiver_Service_logcat"
                + ".txt";
        

		
    	// Create an instance of GoogleAPIClient.
    	if (mGoogleApiClient == null) {
    	    mGoogleApiClient = new GoogleApiClient.Builder(this)
    	        .addConnectionCallbacks((ConnectionCallbacks) this)
    	        .addOnConnectionFailedListener((OnConnectionFailedListener) this)
    	        .addApi(LocationServices.API)
    	        .build();
    	}
    	mGoogleApiClient.connect();
  	
    	TestInputFile = new File(dir,"TestInput");
    	if (!TestInputFile.exists()){
    		try {
    			TestInputFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	ResultFile = new File(dir,"ResultRecord.txt");
    	if (!ResultFile.exists()){
    		try {
    			ResultFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	tmpCallStartFile = new File(dir,"tmpCallStartFile.txt");
    	if (!tmpCallStartFile.exists()){
    		try {
    			tmpCallStartFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	tmpCallStartFile_str = "tmpCallStartFile.txt";
    	
    	
    	CallStartLocationFile = new File(dir,"CallStartLocationFile.txt");
    	if (!CallStartLocationFile.exists()){
    		try {
    			CallStartLocationFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	CallStartLocationFile_Str = "CallStartLocationFile.txt";
    	
    	GroundTruthFile = new File(dir,"GroundTruthFile.txt");
    	if (!GroundTruthFile.exists()){
    		try {
    			GroundTruthFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
		
	

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		
		
        log_state  = getLogState(context);
        logger.d("getlogstate in service:   "+log_state); 
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
		
		String action = intent.getAction();
		if (action.equals("call_started")) {
			
			writeToFile("clear",tmpCallStartFile_str);
			writeToFile("clear",CallStartLocationFile_Str);
			mGoogleApiClient.connect();
			calculate_mode = 0;
			Bundle b = intent.getExtras();
            if (Ground_truth == null)
            {
            	Ground_truth = new JSONObject();
            }

			try {
				tmp_call = new JSONObject(b.getString("callStart"));
				Ground_truth.put("callStart", tmp_call);
				callType = tmp_call.getInt("callType");	
				logger.d(tmp_call.toString());
				logger.d( String.valueOf(callType));
			} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
			}
			processInfo();	
			getLocation(callType);
			logger.d(Ground_truth.toString());

			writeToFile(Ground_truth.toString(),tmpCallStartFile_str);
			
			
			
			
		} else if (action.equals("call_ended")) {
			mGoogleApiClient.connect();
			calculate_mode = 0;
			Bundle b = intent.getExtras();
			try {
				tmp_call = new JSONObject(b.getString("callEnd"));
				callType = tmp_call.getInt("callType");	
				logger.d(tmp_call.toString());
				logger.d( String.valueOf(callType));
			} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
			}
			if (Ground_truth !=null){
				logger.d( Ground_truth.toString());
			}
			else
			{
				String tmp_groundth = readFromFile(tmpCallStartFile_str);
				try {
					Ground_truth = new JSONObject(tmp_groundth);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String tmp_location1 = readFromFile(CallStartLocationFile_Str);
				try {
					location1 = new JSONObject(tmp_location1);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (Ground_truth!=null){
					String tmp_start_location = "fail";
					if (location1 != null) {
						try {
							tmp_start_location = location1.getString("startLocation");
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					try {
						Ground_truth.put("Location1",tmp_start_location );
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				
				try {
					Ground_truth.put("callEnd", tmp_call);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				logger.d(Ground_truth.toString());
			}
			
//	    	Log.d("Process audio", "Process audio");
//	    	ProcessAudio();
			processInfo();	
			//getLocation(callType);
			logger.d(Ground_truth.toString());
			
			
			updateSentState("FALSE",context);

			
			Intent i1 = new Intent(context, MainActivity.class);
			i1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i1.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i1.putExtra("ground_truth", Ground_truth.toString());
			context.startActivity(i1);
			logger.d("send result to activity     "+i1.toString());
			Ground_truth = null;
		
			
		}
		
		if (log_process != null)
		{
			logger.d("destroy log process in service: "+log_process.toString());
			log_process.destroy();
		}
		
		
	}
	
	
    public String getLogState(Context context){
        sp1_log = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1_log.getString("log_state", "TRUE");
        logger.d("get log state in service as :"+st);
        return st;
    }
    
    public void updateSentState(String state,Context context){
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = sp.edit();
        spEditor.putString("send_state", state);
        spEditor.commit();
        logger.d( "send state is false in service for every service");
    }
	
	
    public void processInfo()
    {
    	if (tmp_call == null)
    	{
    		return;
    	}

    	try {
			audio_in_use = tmp_call.getInt("Audioflag");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	if (audio_in_use == 0)
    	{
    		logger.d("process light");
    		ProcessLight();
    	}
    	if (audio_in_use == 1)
    	{	logger.d("process audio");
    		ProcessAudio();
    	}    	
    }
    
    public void ProcessLight(){
    	
    	String AveValue;
		try {
			AveValue = tmp_call.getString("AveValue");
			logger.d( AveValue);
			RGBAvailable = tmp_call.getInt("RGBAvailable");
			logger.d( String.valueOf(RGBAvailable));
	    	String[] splitStr_AveValue = AveValue.split("\\s+");
	    	Light_Sum = Double.parseDouble(splitStr_AveValue[0]);
	    	R_Sum = Double.parseDouble(splitStr_AveValue[1]);
	    	G_Sum = Double.parseDouble(splitStr_AveValue[2]);
	    	B_Sum = Double.parseDouble(splitStr_AveValue[3]);
	    	W_Sum = Double.parseDouble(splitStr_AveValue[4]);
	    	Wifi_Sum = Double.parseDouble(splitStr_AveValue[5]);
			int DaytimeFlag = 0;
			Calendar rightNow = Calendar.getInstance();
			int Hours = rightNow.get(Calendar.HOUR_OF_DAY);
			int Minutes = rightNow.get(Calendar.MINUTE);
			Double CurrentTime = (double) Hours + (double) (Minutes/60);
			
			if (RGBAvailable == 1){
				if ((CurrentTime > 7.5) && (CurrentTime < 20.5)) {
					DaytimeFlag = 1;
				}
			}
			else{
				if ((CurrentTime > 7.5) && (CurrentTime < 20)) {
					DaytimeFlag = 1;
				}
				
			}
			String result_str="";
	    	if ((DaytimeFlag==1) && (Light_Sum>1) && (R_Sum>1) && (G_Sum>1) && (B_Sum>1) && (RGBAvailable==1))
	    	{
	    		if (Light_Sum > 5000)
	    		{
	    			result_str = "1"+" "+"1.0";	
	    			calculate_mode = 3;
	    		}
	    		else{
	    			result_str = svmPredictResult(Light_Sum,R_Sum,G_Sum,B_Sum,W_Sum);
	    		}
	    		
	    	}
	    	else if ((DaytimeFlag==1) && (RGBAvailable==0))
	    	{
	    		result_str = DaytimePredict(Light_Sum,Wifi_Sum);
	    	}
	    	else{
	    		result_str = NightPredict(Light_Sum,Wifi_Sum);
	    	}
	    	logger.d( result_str);
	    	addResults(result_str,callType,calculate_mode);
	    	WriteResult(result_str);
	    	
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void addResults(String result_str,int callType, int calculateMode){
		long curTime = System.currentTimeMillis();
		JSONObject tmp_object = new JSONObject();
		try {
			tmp_object.put("timestamp", curTime);
			tmp_object.put("Result", result_str);
			tmp_object.put("mode", calculateMode);
			if (callType==1){
				Ground_truth.put("result", tmp_object);}
			if (callType==2){
				Ground_truth.put("result2", tmp_object);}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}   	
    }
    
    public void ProcessAudio(){
    	
    	String Str_return_result = "";
		String Str_dir = dir + "/";
		String Str_rawdata = Str_dir + "SoundData.txt";
		String Str_chirpfile = Str_dir + "chirp_file";
		String Str_model = Str_dir + "model_audio";
		String Str_range = Str_dir + "range_audio";
		String Str_test = Str_dir + "Feature_SoundData.txt";
		String Str_scale = Str_dir + "AudioTestInput_scale";
		String Str_result = Str_dir + "AudioDetect_result";
		Cmd_get_feature = Str_rawdata + " " + Str_chirpfile + " " + Str_test;
		Cmd_svm_scale = "-r " + Str_range + " " + Str_test + " " + Str_scale;
		Cmd_svm_predict = "-b 1 " + Str_scale + " " + Str_model + " " + Str_result;
//		Log.d("get feature of files", "get feature of files");
//		processAudio(Cmd_get_feature);
	
		try {
			processAudio(Cmd_get_feature);
			logger.d( "get feature of files");
		} catch (Exception e1)
		{
			e1.printStackTrace();
		}
		jniSvmScale(Cmd_svm_scale);
		jniSvmPredict(Cmd_svm_predict);
		logger.d( "finish testing");
		
		File AudioResultFile = new File(Str_result);
		BufferedReader bufferedReader_svm = null;
		try {
			bufferedReader_svm = new BufferedReader(new FileReader(AudioResultFile));
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
					if (count == 0) {
						count = 1;
						continue;
					}
					finalString.append(line);
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

		Str_return_result = finalString.toString();
		if (Str_return_result.length() > 6) {
			String[] splitStr = Str_return_result.split("\\s+");
			int Tmp_result = Integer.parseInt(splitStr[0]);
			Double Tmp_con = 0.0;
			if (Tmp_result == -1) {
				Tmp_con = Double.parseDouble(splitStr[2]);
			} else {
				Tmp_con = Double.parseDouble(splitStr[1]);

			}
			Str_return_result = String.valueOf(Tmp_result) + " " + String.valueOf(Tmp_con);
			logger.d( Str_return_result);
		} else {
			Str_return_result = "0" +" "+ "0.0";
		}
		calculate_mode = 4;
		addResults(Str_return_result,callType,calculate_mode);	
		WriteResult(Str_return_result);
    }
	
    private String svmPredictResult(double Light_Sum, double R_Sum, double G_Sum, double B_Sum, double W_Sum){
		double r_to_b = R_Sum/B_Sum;
		double g_to_b = G_Sum/B_Sum;
		double r_to_g = R_Sum/G_Sum;
//		double r_to_w = R_Sum/W_Sum;
//		double g_to_w = G_Sum/W_Sum;
//		double b_to_w = B_Sum/W_Sum;
//		double r_to_l = R_Sum/Light_Sum;
//		double g_to_l = G_Sum/Light_Sum;
//		double b_to_l = B_Sum/Light_Sum;
//		double w_to_l = W_Sum/Light_Sum;	
	
	
		String Str_test_input = "0"+" "+"1:"+Double.toString(Light_Sum)+" "+
				"2:"+Double.toString(r_to_b)+" "+"3:"+Double.toString(g_to_b)+" "+"4:"+Double.toString(r_to_g)+"\n";
//				"5:"+Double.toString(r_to_w)+" "+"6:"+Double.toString(g_to_w)+" "+"7:"+Double.toString(b_to_w)+" "+
//				"8:"+Double.toString(r_to_l)+" "+"9:"+Double.toString(g_to_l)+" "+"10:"+Double.toString(b_to_l)+" "+"11:"+Double.toString(w_to_l)+"\n";
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
		String Str_model = Str_dir+"model_light";
		String Str_range = Str_dir+"range_light";
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
		String [] splitStr = Str_Result.split("\\s+");
		int Tmp_result = Integer.parseInt(splitStr[0]);
		Double Tmp_con = 0.0;
		if ( Tmp_result ==-1){
			Tmp_con = Double.parseDouble(splitStr[2]);
		}
		else{
			Tmp_con = Double.parseDouble(splitStr[1]);
			
		}
		Str_Result = String.valueOf(Tmp_result) +" "+String.valueOf(Tmp_con);
		logger.d( Str_Result);
		calculate_mode = 1;
		return Str_Result;  	   	
    }
    
    
    private String NightPredict(double Light_Sum, double Wifi_Sum){
    	
    	double Light_threshold = 30;
    	double Wifi_threshold = -70;
    	
    	int Result = 0;
    	double Result_con = 0.0;
    	int Light_Result = 0;
    	double Light_con = 0.0;
    	int Wifi_Result = 0;
    	double Wifi_con = 0.0;
    	String Str_return_result;

		if (Light_Sum > Light_threshold) {
			Light_Result = -1;
			Light_con = (Light_Sum - Light_threshold) / Light_Sum;
		} else {
			Light_Result = 1;
			Light_con = ((Light_threshold - Light_Sum) / Light_threshold) * 0.9;

		}

		if (Wifi_Sum <(-100)) {
			Wifi_Result = 0;
		} else {
			if (Wifi_Sum > (Wifi_threshold)) {
				Wifi_Result = -1;
				Wifi_con = (Wifi_Sum - Wifi_threshold) / Math.abs(Wifi_threshold) + 0.6;
			} else {
				Wifi_Result = 1;
				Wifi_con = (Wifi_threshold - Wifi_Sum) / Math.abs(Wifi_threshold) + 0.6;
			}
		}

		if (Wifi_Result == 0) {
			Result = Light_Result;
			Result_con = Light_con;
		} else {
			if (Light_Result == Wifi_Result) {
				Result = Light_Result;
				Result_con = 1 - (1 - Light_con) * (1 - Wifi_con);
			} else {
				if (Light_con > Wifi_con) {
					Result = Light_Result;
					Result_con = Light_con * (1 - Wifi_con);
				} else {
					Result = Wifi_Result;
					Result_con = Wifi_con * (1 - Light_con);
				}
			}
		}

		Str_return_result = Integer.toString(Result) + " " + Double.toString(Result_con);
		calculate_mode = 2;
		if ((Wifi_Sum < (-105)) && (Light_Sum < 2)) {
			Str_return_result = "0" +" "+"0.0";
			calculate_mode = 5;
		}
		return Str_return_result;
	   	
    }
    
    
    private String DaytimePredict(double Light_Sum, double Wifi_Sum){
    	
    	double Light_threshold1 = 4000;
    	double Light_threshold2 = 2000;
    	double Wifi_threshold = -70;
    	
    	int Result = 0;
    	double Result_con = 0.0;
    	int Light_Result = 0;
    	double Light_con = 0.0;
    	int Wifi_Result = 0;
    	double Wifi_con = 0.0;
    	String Str_return_result;

		if (Light_Sum > Light_threshold1) {
			Light_Result = 1;
			Light_con = 1.0;
		} else {
			if (Light_Sum > Light_threshold2)
			{
				Light_Result = 1;
				Light_con = (( Light_Sum - Light_threshold2) / Light_threshold2);
			}
			else
			{
				Light_Result = -1;
				Light_con = (( Light_threshold2 - Light_Sum) / Light_threshold2);
			}

		}

		if (Wifi_Sum <(-100)) {
			Wifi_Result = 0;
		} else {
			if (Wifi_Sum > (Wifi_threshold)) {
				Wifi_Result = -1;
				Wifi_con = (Wifi_Sum - Wifi_threshold) / Math.abs(Wifi_threshold) + 0.6;
			} else {
				Wifi_Result = 1;
				Wifi_con = (Wifi_threshold - Wifi_Sum) / Math.abs(Wifi_threshold) + 0.6;
			}
		}

		if (Wifi_Result == 0) {
			Result = Light_Result;
			Result_con = Light_con;
		} else {
			if (Light_Result == Wifi_Result) {
				Result = Light_Result;
				Result_con = 1 - (1 - Light_con) * (1 - Wifi_con);
			} else {
				if (Light_con > Wifi_con) {
					Result = Light_Result;
					Result_con = Light_con * (1 - Wifi_con);
				} else {
					Result = Wifi_Result;
					Result_con = Wifi_con * (1 - Light_con);
				}
			}
		}

		Str_return_result = Integer.toString(Result) + " " + Double.toString(Result_con);
		calculate_mode = 6;
		if ((Wifi_Sum < (-105)) && (Light_Sum < 2)) {
			Str_return_result = "0" +" "+"0.0";
			calculate_mode = 5;
		}
		return Str_return_result;
	   	
    }
    
    
    private void WriteResult(String str_detect_result){
    	
		if(ResultFile.exists()){
			  
			  try{
	    		
				  foutResult = new FileOutputStream(ResultFile,true);
				  outwriterResult= new OutputStreamWriter(foutResult);
			
			  } catch(Exception e)
			  {

			  }
		}
		
		try {
			outwriterResult.append((str_detect_result+"\n"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			outwriterResult.flush();
			outwriterResult.close();
			foutResult.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	    	
    }
	
    public void writeJSON(OutputStreamWriter myWriter, long timestamp, String tag, String info) {
  	   JSONObject object = new JSONObject();
  	   try {

  	      object.put("timestamp", timestamp);
  	      object.put(tag, info); 	          
  	      String content = object.toString() + "\n";
  	      myWriter.append(content);
  	           /*if(status.getStatusCode() == HttpStatus.SC_OK){
  	               ByteArrayOutputStream out = new ByteArrayOutputStream();
  	               response.getEntity().writeTo(out);
  	               out.close();
  	               Log.d("DEBUG",out.toString());
  	           }*/
  	           //Log.i(TAG, content);

  	   } catch (JSONException | IOException e) {
  	      e.printStackTrace();
  	   }
  	}
    
	@Override
	public void onConnected(Bundle connectionHint) {
		// Provides a simple way of getting a device's location and is well
		// suited for
		// applications that do not require a fine-grained location and that do
		// not need location
		// updates. Gets the best and most recent location currently available,
		// which may be null
		// in rare cases when a location is not available.
		mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100); // Update location every second

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, (LocationListener) this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
	
	}
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Refer to the javadoc for ConnectionResult to see what error codes
		// might be returned in
		// onConnectionFailed.
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// The connection to Google Play services was lost for some reason. We
		// call connect() to
		// attempt to re-establish the connection.
		Log.i(TAG, "Connection suspended");
		mGoogleApiClient.connect();
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		if (LocationFlag == 0){
			location_global = Double.toString(location.getLatitude()) + " "+Double.toString(location.getLongitude());	
			LocationFlag = 1;	
			logger.d( location_global);
		}

	}
	
    public void getLocation(final int CallType){
    	    
		if (LocationThread!= null)
		{
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				logger.d("stop LocationThread");
				
			}
		}
	    start_location_time = System.currentTimeMillis();
	    stop_location_time = start_location_time + 3000;
	    logger.d( String.valueOf(start_location_time));
		logger.d( String.valueOf(stop_location_time));
    	
    	if (LocationThreadstart ==0)
		{
    		LocationThreadstart = 1;
			logger.d("start Location thread");
			LocationThread = new Thread() {

				public void run() {
					while(!Thread.interrupted())
				    {
						
						while (true){
							
							if (start_location_time > stop_location_time)
							{
    							logger.d( String.valueOf(start_location_time));
    							logger.d( String.valueOf(stop_location_time));
    							location_tmp = "fail";
    							break;
							}
							
							if ((mLastLocation != null) || (location_global.length() > 2)) {
								logger.d( mLastLocation.toString());
								logger.d( location_global);
								if (mLastLocation != null) {
									location_tmp = Double.toString(mLastLocation.getLatitude()) + " "
											+ Double.toString(mLastLocation.getLongitude());

								} else {

									location_tmp = location_global;
								}
								break;
							}
							start_location_time = System.currentTimeMillis();
						}
						writeLocation();
						logger.d( "interrupte location thread");
						Thread.currentThread().interrupt();
						return;
				   }
				}
			};
			LocationThread.start();
		}
	
    }
    
    private void writeLocation(){
    	
		if (LocationThread!= null)
		{
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				logger.d( "stop LocationThread in writing location");
				
			}
		}
		JSONObject tmp_object = new JSONObject();
		try {
			tmp_object.put("startLocation", location_tmp);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		writeToFile(tmp_object.toString(),CallStartLocationFile_Str);
		logger.d( tmp_object.toString());
    }
    
    private void writeToFile(String data, String file) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(file, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            logger.d( "write file successfully");
        }
        catch (IOException e) {
            //Log.e(TAG, "File write failed: " + e.toString());
        } 
    }


    private String readFromFile(String file) {

        String ret = "";

        try {
            InputStream inputStream = openFileInput(file);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            //Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            //Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }
	
}
