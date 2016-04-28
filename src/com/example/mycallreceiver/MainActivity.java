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
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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
	Context context = null;
	
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
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		setContentView(R.layout.activity_main);
		textCallStart_reading = (TextView) findViewById(R.id.CallStart_reading);
		textCallEnd_reading = (TextView) findViewById(R.id.CallEnd_reading);
		textLocationStart_reading = (TextView) findViewById(R.id.LocationStart_reading);
		textLocationEnd_reading = (TextView) findViewById(R.id.LocationEnd_reading);
		root = android.os.Environment.getExternalStorageDirectory();
		dir = new File(root.getAbsolutePath() + "/CallDetection");
		dir.mkdirs();
		Groundtruthfile_str = dir+"/GroundTruthFile.txt";
		

		
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
		Intent service_intent = getIntent();
		Log.d(TAG, service_intent.toString());

	
	
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		Intent service_intent = getIntent();
		Log.d(TAG, service_intent.toString());
		Bundle b = service_intent.getExtras();
		if (b!=null){
			
			String tmp_groundth =(String) b.get("ground_truth"); 
			if (tmp_groundth!=null)
			{
				Log.d(TAG, tmp_groundth);
				try {
					Ground_truth = new JSONObject(tmp_groundth);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d(TAG, Ground_truth.toString());
				
				mGoogleApiClient.connect();
				LocationFlag = 0;
				
				LocationThread = null;
				getLocation();
				
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
							Log.d(TAG, Ground_truth.toString());
							Ground_truth.put("End_ground_truth", End_truth);
							Log.d(TAG, Ground_truth.toString());
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						waitSending();
						Log.d(TAG, Integer.toString(End_truth));
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
							Log.d(TAG, Ground_truth.toString());
							Ground_truth.put("Start_ground_truth", Start_truth);
							Log.d(TAG, Ground_truth.toString());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// Do something with the selection
						Log.d(TAG, Integer.toString(Start_truth));
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
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    Log.d("status", "onDestroy");
	}
	
	@Override
	public void onPause() {
	    super.onPause();
	    Log.d("status", "onPause");
	    //unregisterReceiver(broadcastReceiver);
	}
	
	@Override
	public void onStart() {
	    super.onStart();
	    Log.d("status", "onstart");
	
	}
	
	@Override
	public void onRestart() {
	    super.onRestart();
	    Log.d("status", "onRestart");

	}
	
	@Override
	public void onStop() {
	    super.onStop();
	    Log.d("status", "onStop");
	    this.finish();
	    //registerReceiver(broadcastReceiver, new IntentFilter("broadCastName"));
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
				if ((result1_con < 0.9) && (calculate_mode==1))
				{
					result1_Str = "Result: " + "Unknown   ";
					
				}
				else{
				result1_Str = "Result:	" + "outdoor   " + "     confidence:  " + splitStr_result1[1];
				}
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
				if ((result2_con < 0.9) && (calculate_mode==1)){
					result2_Str = "Result:	" + "Unknown   ";					
				}
				else
				{
				result2_Str = "Result:	" + "outdoor   " + "     confidence:  " + splitStr_result2[1];
				}
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
						
						Log.d("Jack-Response", s);
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
		Log.d(TAG, Ground_truth.toString());
		if (waitForSending!= null)
		{
			if (!waitForSending.isInterrupted());
			{
				waitForSending.interrupt();
				waitForSending = null;
				Log.d(TAG, "stop waitForSending in SendInformation");
				
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
							Log.d(TAG, "triger the SendInformation ");
							break;
						}
					} else {
						break;
					}
				
			    }
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
			Log.d(TAG, location_global);
		}

	}
	
	public void getLocation() {

		if (LocationThread != null) {
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				Log.d(TAG, "stop LocationThread");

			}
		}
		start_location_time = System.currentTimeMillis();
		stop_location_time = start_location_time + 3000;
	    Log.d(TAG, String.valueOf(start_location_time));
		Log.d(TAG, String.valueOf(stop_location_time));

		if (LocationThreadstart == 0) {
			LocationThreadstart = 1;
			Log.d(TAG, "start Location thread");
			LocationThread = new Thread() {

				public void run() {
					while (!Thread.interrupted()) {

						while (true) {

							if (start_location_time > stop_location_time) {
								Log.d(TAG, String.valueOf(start_location_time));
								Log.d(TAG, String.valueOf(stop_location_time));
								location2 = "fail";
								break;
							}

							if ((mLastLocation != null) || (location_global.length() > 2)) {
								Log.d(TAG, mLastLocation.toString());
								Log.d(TAG, location_global);
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
					}
				}
			};
			LocationThread.start();
		}

	}

	private void putLocation() {
		
		Log.d(TAG, location2);

		if (LocationThread != null) {
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				Log.d(TAG, "stop LocationThread in writing location");

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
