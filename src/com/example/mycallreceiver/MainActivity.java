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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity implements
ConnectionCallbacks, OnConnectionFailedListener,LocationListener {
	
	
//	static{
//		System.loadLibrary("jnilibsvm");
//	}
//	
//	public native void jniSvmPredict(String cmd);
//	public native void jniSvmScale(String cmd);
//	public native void processAudio(String cmd);
	
	private static final String TAG = "MyCallReceiverMainActivity";
	public Logger logger = new Logger(true,TAG);
	int Start_truth = -10;
	int End_truth = -10;
    NotificationManager manager;
    Notification myNotication;
	public static int raw_counter = 0;
	

	File root = null;
	File dir = null;
	JSONObject Ground_truth = null;
	JSONObject tmp_truth = null;
	JSONObject tmp_call = null;
	JSONObject Location = null;
	JSONObject Result1 = null;
	JSONObject Result2 = null;
	
	public String GroundTruthFile_str = "GroundTruthFile.txt";
	int callType = 0;
	
//	GoogleApiClient mGoogleApiClient;
//	LocationRequest mLocationRequest;
//	Location mLocation;
//	Location mLastLocation;
//	int LocationFlag = 0;
//	int StartTransmit = 0;
	
	
	TextView textCallEnd_reading;
	TextView textCallStart_reading;
	TextView textLocationStart_reading;
	TextView textLocationEnd_reading;
	Button logBtn;
	Button AudioBtn;
	
	File ResultFile = null;
	FileOutputStream foutResult = null;
	OutputStreamWriter outwriterResult = null;
	
	public int calculate_mode1 = 0;
	
	public int calculate_mode2 = 0;
	
	public String location1="";
	public String location2="";
	
	String result1_Str = "call start result";
	int result1 = 0;
	double result1_con = 0.0;
	String result2_Str = "call end result";
	int result2 = 0;
	double result2_con = 0.0;

	public Thread waitForSending = null;
	private Context context = null;
	
	File GroundTruthFile = null;
	FileOutputStream foutGroundtruth = null;
	OutputStreamWriter outwriterGroundtruth = null;
	public String Groundtruthfile_str= "";
	
	GoogleApiClient mGoogleApiClient;
	LocationRequest mLocationRequest;
	Location mLocation;
	Location mLastLocation;
	int LocationFlag = 0;
	int StartTransmit = 0;
	String location_global ="";
	public Thread LocationThread = null;
	public int LocationThreadstart = 0;
    public long start_location_time = 0;
    public long stop_location_time = 0;
    public Intent service_intent = null;

    
    private SharedPreferences sp,sp1;
    private SharedPreferences.Editor spEditor;
    
    private SharedPreferences sp_log,sp1_log;
    private SharedPreferences.Editor spEditor_log;
    
    private SharedPreferences sp_audio,sp1_audio;
    private SharedPreferences.Editor spEditor_audio;
    
    private String sent_state;
    
    private String clear_command = "logcat -c -b main -b radio -b events\n";
    private String start_log_command ="";
    private Process log_process = null;
    
    private String log_state = "TRUE";  //1: logging
    
    private String audio_state = "TRUE";
    
    private int isS6 = 0;
    private int isG4 = 0;
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;
		setContentView(R.layout.activity_main);
		textCallStart_reading = (TextView) findViewById(R.id.CallStart_reading);
		textCallEnd_reading = (TextView) findViewById(R.id.CallEnd_reading);
		textLocationStart_reading = (TextView) findViewById(R.id.LocationStart_reading);
		textLocationEnd_reading = (TextView) findViewById(R.id.LocationEnd_reading);
		logBtn=(Button) findViewById(R.id.log_switch);
		AudioBtn = (Button) findViewById(R.id.Audio_switch);
		
		root = android.os.Environment.getExternalStorageDirectory();
		dir = new File(root.getAbsolutePath() + "/CallDetection");
		dir.mkdirs();
		Groundtruthfile_str = dir+"/GroundTruthFile.txt";
		if (!dir.exists()) {
		}
		start_log_command = "logcat   -v threadtime -b main -f " + dir + File.separator
		        + "MyCallReceiver_MainActivity_logcat"
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

		final int[] mFiles = new int[] { R.raw.light_model6, R.raw.light_range_set6, R.raw.audio_model4, R.raw.audio_range4, R.raw.chirp14_file };
		final CharSequence[] filenames = { "model_light", "range_light", "model_audio", "range_audio", "chirp_file" };
		for (int i = 0; i < mFiles.length; i++) {
			try {
				if (dir.mkdirs() || dir.isDirectory()) {
					String str_song_name = dir + "/" + filenames[i];
					CopyRAWtoSDCard(mFiles[i], str_song_name);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
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
		
		

		logger.d("test for logger");
	
	}
	
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		
		if (log_process != null)
		{
			logger.d("log_process in onResume:  "+log_process.toString());
		}
        log_state  = getLogState(context);
        logger.d("getlogstate in main activity:   "+log_state); 
        if (log_state.equals("FALSE"))
        {
        	logBtn.setText("logging stopped (push to start logging)");
        	logger.d("set text on logBtn logging stopped");
        }
        else
        {
        	logBtn.setText("logging (push to stop logging)");
        	logger.d("set text on logBtn logging started");
        }

        if (isS6==1)
        {
            
            audio_state = getAudioState(context);
            logger.d("get audio state in main activity:   "+audio_state);
        	
			if (audio_state.equals("FALSE")) {
				
				AudioBtn.setText("Audio stopped (push to use audio)");
				logger.d("set text on AudioBtn audio stopped");
				

			} else {
				AudioBtn.setText("Audio in using (push to stop audio)");
				logger.d("set text on AudioBtn audio started");
			}
        }
        else
        {
        	AudioBtn.setText("Audio not supported");
        }
        
        
        if ((log_state.equals("TRUE")) && (log_process ==null ))
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
				
		service_intent = getIntent();
		sent_state = getSentState(context);
		logger.d("get previous sent state:"+sent_state);
		boolean launchedFromHistory = service_intent != null ? (service_intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0 : false;
		logger.d("lacunch from history:	"+launchedFromHistory);
		
		boolean enable_pop = true;
		if (launchedFromHistory)
		{
			if (sent_state.equals("TRUE"))
			{
				enable_pop = false;
			}
		}
		
		logger.d("value of enable pop:"+enable_pop);
		

		logger.d("onResume");
		logger.d(service_intent.toString());
		Bundle b = service_intent.getExtras();
		if ((b!=null) && (enable_pop)){
			
			String tmp_groundth =(String) b.get("ground_truth"); 
			if (tmp_groundth!=null)
			{
				
				try {
					Ground_truth = new JSONObject(tmp_groundth);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				logger.d( Ground_truth.toString());
				
				mGoogleApiClient.connect();
				LocationFlag = 0;
			
				
				LocationThread = null;
				getLocation();
				
				
//				getIntent().replaceExtras(new Bundle());
//				getIntent().setAction("");
//				getIntent().setData(null);
//				getIntent().setFlags(0); 
//				Log.d(TAG,"clear intet");
//				Intent tmp_intent = getIntent();
//				Log.d(TAG, tmp_intent.toString());
				
				final CharSequence[] items = { "Indoor", "Outdoor", "Unknown" };

				// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				// WindowManager.LayoutParams.FLAG_FULLSCREEN);

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("At the END of the call");
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch (item) {
						case 0:
							End_truth = -1;
							break;
						case 1:
							End_truth = 1;
							break;
						case 2:
							End_truth = 0;
							break;
						default:
							End_truth = -10;
						}
						try {
							logger.d(Ground_truth.toString());
							Ground_truth.put("End_ground_truth", End_truth);
							logger.d(Ground_truth.toString());
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						waitSending();
						logger.d(Integer.toString(End_truth));
					}
				});
				AlertDialog alert = builder.create();
				alert.show();

				AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
				builder2.setTitle("At the START of the call ");
				builder2.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item2) {
						switch (item2) {
						case 0:
							Start_truth = -1;
							break;
						case 1:
							Start_truth = 1;
							break;
						case 2:
							Start_truth = 0;
							break;
						default:
							Start_truth = -10;
						}
						try {
							logger.d(Ground_truth.toString());
							Ground_truth.put("Start_ground_truth", Start_truth);
							logger.d(Ground_truth.toString());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// Do something with the selection
						logger.d(Integer.toString(Start_truth));
					}
				});
				AlertDialog alert2 = builder2.create();
				alert2.show();

				try {
					location1 = Ground_truth.getString("Location1");
					textLocationStart_reading.setText("Location(call start): " + location1);	

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					Result1 = Ground_truth.getJSONObject("result");
					Result2 = Ground_truth.getJSONObject("result2");
					result1_Str = Result1.getString("Result");
					result2_Str = Result2.getString("Result");
					calculate_mode1 = Result1.getInt("mode");
					calculate_mode2 = Result2.getInt("mode");
					printResults(result1_Str, 1, calculate_mode1);
					printResults(result2_Str, 2, calculate_mode2);
					

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
			}

		}
		
	}
	
	
	public void logSwitch(View view) throws IOException {
		
		
		log_state = getLogState(context);
		logger.d("get log state from getlogstate:	"+log_state);
		if (log_state.equals("FALSE")){
			log_state ="TRUE";
			updateLogState(log_state,context);
			logBtn.setText("logging (push to stop logging)");
			
			if (log_process==null) {
				
				logger.d("log process:   "+"null");
			}

		}
		else
		{	log_state = "FALSE";
			
			updateLogState(log_state,context);
			if (log_process!=null)
			{
				logger.d("destroy log procee:"+log_process.toString());
				log_process.destroy();
				
			}
			logBtn.setText("logging stopped (push to start logging)");
		}
	}
	
	
	
	public void AudioSwitch(View view) throws IOException {
		
		if (isS6==1) {
			audio_state = getAudioState(context);
			logger.d("get audio state from getAudiostate:	" + audio_state);
			if (audio_state.equals("FALSE")) {
				audio_state = "TRUE";
				updateAudioState(audio_state, context);
				AudioBtn.setText("Audio in using (push to stop audio)");
				logger.d("set audio in using");

			} else {
				audio_state = "FALSE";

				updateAudioState(audio_state, context);
				AudioBtn.setText("Audio stopped (push to use audio)");
				logger.d("set audio to stop");
			}
		}
		else
		{
        	AudioBtn.setText("Audio not supported");
			
		}
	}
	
    public void updateSentState(String state,Context context){
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor = sp.edit();
        spEditor.putString("send_state", state);
        spEditor.commit();
        logger.d( "finish updated data");
    }
    
    public String getSentState(Context context){
        sp1 = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1.getString("send_state", "");
        logger.d("get send state as :"+st);
        return st;
    }
    
    
    public void updateLogState(String state,Context context){
        sp_log = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor_log = sp_log.edit();
        spEditor_log.putString("log_state", state);
        spEditor_log.commit();
        logger.d( "finish update log state"+state);
    }
    
    public String getLogState(Context context){
        sp1_log = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1_log.getString("log_state", "TRUE");
        logger.d("get log state as :"+st);
        return st;
    }
    
    
    public void updateAudioState(String state,Context context){
        sp_audio = PreferenceManager.getDefaultSharedPreferences(context);
        spEditor_audio = sp_audio.edit();
        spEditor_audio.putString("audio_state", state);
        spEditor_audio.commit();
        logger.d( "finish update audio state"+state);
    }
    
    public String getAudioState(Context context){
        sp1_audio = PreferenceManager.getDefaultSharedPreferences(context);
        String st =sp1_audio.getString("audio_state", "TRUE");
        logger.d("get audio state as :"+st);
        return st;
    }
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    logger.d("status onDestroy");
		if (log_process != null)
		{
			logger.d("log process:(destroy) "+log_process.toString());
			log_process.destroy();

			
		}
	}
	
	@Override
	public void onPause() {
	    super.onPause();
	    logger.d("status onPause");
	   
	}
	
	@Override
	public void onStart() {
	    super.onStart();
	    logger.d("status onstart");
	
	}
	
	@Override
	public void onRestart() {
	    super.onRestart();
	    logger.d("status onRestart");

	}
	
	@Override
	public void onStop() {
	    super.onStop();
	    logger.d("status onStop");
	    this.finish();
	  
	}
	
		

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
    
    public void printResults(String result_str,int CallType, int calculate_mode){
    	
    	String tmp_result_str = result_str;
    	
    	if (CallType==1){
			
    		String[] splitStr_result1 = tmp_result_str.split("\\s+");
    		result1 = Integer.parseInt(splitStr_result1[0]);
    		result1_con = Double.parseDouble(splitStr_result1[1]);
			if (result1 == -1) {
				result1_Str = "Result: " + "indoor   " + "     confidence:  " + splitStr_result1[1];
			}
			else if (result1 == 1) {
				
				result1_Str = "Result:	" + "outdoor   " + "     confidence:  " + splitStr_result1[1];
				
			}
			else{
				result1_Str = "Result: " + "Unknown   ";
			}
			textCallStart_reading.setText("Call Start:     " + result1_Str);
		}
    	
    	if (CallType==2){

			String[] splitStr_result2 = tmp_result_str.split("\\s+");
			result2 = Integer.parseInt(splitStr_result2[0]);
			result2_con = Double.parseDouble(splitStr_result2[1]);
			if (result2 == -1) {
				result2_Str = "Result: " + "indoor   " + "     confidence:  " + splitStr_result2[1];
			}
			else if(result2 == 1) {
			
				result2_Str = "Result:	" + "outdoor   " + "     confidence:  " + splitStr_result2[1];
				
			}
			else{
				result2_Str = "Result:	" + "Unknown   ";	
			}
			textCallEnd_reading.setText("Call end  :     " + result2_Str);
		}   	
    }
    

	public void SendInformation() {
		Thread t = new Thread() {

			public void run() {
				Looper.prepare(); // For Preparing Message Pool for the
									// child Thread
				HttpClient client = new DefaultHttpClient();
				HttpResponse response;
				JSONObject json = new JSONObject();

				try {
					HttpPost post = new HttpPost("http://psychic-rush-755.appspot.com/upload");
					StringEntity se = new StringEntity(Ground_truth.toString());
					se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
					post.setEntity(se);
					response = client.execute(post);

					/* Checking response */
					if (response != null) {
						InputStream in = response.getEntity().getContent(); // Get
																			// the
																			// data
																			// in
																			// the
																			// entity
						String s = getStringFromInputStream(in);
						updateSentState("TRUE",context);
						logger.d("update sent state:TRUE");
						
						logger.d( "Jack-Response   " +s);

					}
					Ground_truth = null;
					Ground_truth = new JSONObject();
					raw_counter = 0;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		t.start();	
		logger.d(Ground_truth.toString());
		if (waitForSending!= null)
		{
			if (!waitForSending.isInterrupted());
			{
				waitForSending.interrupt();
				waitForSending = null;
				logger.d("stop waitForSending in SendInformation");
				
			}
		}
		
	}
	
	
	private void CopyRAWtoSDCard(int id, String path) throws IOException {
	    InputStream in = getResources().openRawResource(id);
	    FileOutputStream out = new FileOutputStream(path);
	    byte[] buff = new byte[1024];
	    int read = 0;
	    try {
	        while ((read = in.read(buff)) > 0) {
	            out.write(buff, 0, read);
	        }
	    } finally {
	        in.close();
	        out.close();
	    }
	}
	
	public void waitSending() {

		waitForSending = new Thread() {

			public void run() {
				while(!Thread.interrupted())
			    {
					if (Ground_truth != null) {
						if ((Ground_truth.has("Start_ground_truth") && Ground_truth.has("Location2")) && Ground_truth.has("End_ground_truth")) {
							SendInformation();
							logger.d("triger the SendInformation ");
							break;
						}
					} else {
						break;
					}
				
			    }
				logger.d("return waitsending thread");
				Thread.currentThread().interrupt();
				return;
				
			}
		};
		waitForSending.start();
	}

	private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();

	}
	
//	private void CopyRAWtoSDCard(int id, String path) throws IOException {
//	    InputStream in = getResources().openRawResource(id);
//	    FileOutputStream out = new FileOutputStream(path);
//	    byte[] buff = new byte[1024];
//	    int read = 0;
//	    try {
//	        while ((read = in.read(buff)) > 0) {
//	            out.write(buff, 0, read);
//	        }
//	    } finally {
//	        in.close();
//	        out.close();
//	    }
//	}
//	
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
        mLocationRequest.setInterval(1000); // Update location every second

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
		//Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// The connection to Google Play services was lost for some reason. We
		// call connect() to
		// attempt to re-establish the connection.
		//Log.i(TAG, "Connection suspended");
		mGoogleApiClient.connect();
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		if (LocationFlag == 0){
			location_global = Double.toString(location.getLatitude()) + " "+Double.toString(location.getLongitude());	
			LocationFlag = 1;	
			logger.d(location_global);
		}

	}
	
	public void getLocation() {

		if (LocationThread != null) {
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				logger.d( "stop LocationThread");

			}
		}
		start_location_time = System.currentTimeMillis();
		stop_location_time = start_location_time + 3000;
	    logger.d(String.valueOf(start_location_time));
		logger.d(String.valueOf(stop_location_time));

		if (LocationThreadstart == 0) {
			LocationThreadstart = 1;
			logger.d("start Location thread");
			LocationThread = new Thread() {

				public void run() {
					while (!Thread.interrupted()) {

						while (true) {

							if (start_location_time > stop_location_time) {
								logger.d(String.valueOf(start_location_time));
								logger.d(String.valueOf(stop_location_time));
								location2 = "fail";
								break;
							}

							if ((mLastLocation != null) || (location_global.length() > 2)) {
								logger.d(mLastLocation.toString());
								logger.d(location_global);
								if (mLastLocation != null) {
									location2 = Double.toString(mLastLocation.getLatitude()) + " "
											+ Double.toString(mLastLocation.getLongitude());

								} else {

									location2 = location_global;
								}
								break;
							}
							start_location_time = System.currentTimeMillis();
						}
						putLocation();
						
						logger.d("interrupte location thread");
						Thread.currentThread().interrupt();
						return;
					}
				}
			};
			LocationThread.start();
		}

	}

	private void putLocation() {
		
		logger.d(location2);

		if (LocationThread != null) {
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				logger.d("stop LocationThread in writing location");

			}
		}
		if (Ground_truth!=null)
		{
			try {
				Ground_truth.put("Location2",location2);
				textLocationEnd_reading.post(new Runnable() {
				    public void run() {
				    	textLocationEnd_reading.setText("Location(call  end):  " + location2);
				    } 
				});
				
				
				
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	 

	
}
