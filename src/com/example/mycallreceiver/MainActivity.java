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
	
	private static final String TAG = "MainActivity";
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

    	
//		Intent service_intent = getIntent();
//		if (service_intent.getStringExtra("ground_truth")!= null )
//		{
//			if (Ground_truth !=null)
//			{
//				Ground_truth=null;
//			}
//
//			try {
//				Ground_truth = new JSONObject(service_intent.getStringExtra("ground_truth"));
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			Log.d("get ground truth from service intent", Ground_truth.toString());
//            
//			popup(context);
//			try {
//				Location = Ground_truth.getJSONObject("Location");
//				location1 = Location.getString("Location1");
//				location2 = Location.getString("Location2");
//				textLocationStart_reading.setText("Location(call start): "+location1);
//				textLocationEnd_reading.setText("Location(call  end):  "+location2);
//				
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			try {
//				Result1 = Ground_truth.getJSONObject("result");
//				Result2 = Ground_truth.getJSONObject("result2");
//				result1_Str = Result1.getString("Result");
//				result2_Str = Result2.getString("Result");
//				calculate_mode1 = Result1.getInt("mode");
//				calculate_mode2 = Result2.getInt("mode");
//				printResults(result1_Str,1,calculate_mode1);
//				printResults(result2_Str,2,calculate_mode2);
//				
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} 
//		}
		
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

		final int[] mFiles = new int[] { R.raw.set1_model, R.raw.range_set1, R.raw.audio_model1, R.raw.audio_range1, R.raw.chirp14_file };
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
		Log.d("service intent in on create", service_intent.toString());

	
	
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		Intent service_intent = getIntent();
		Log.d("service intent in onresume", service_intent.toString());
		Bundle b = service_intent.getExtras();
		if (b!=null){
			
			String tmp_groundth =(String) b.get("ground_truth"); 
			if (tmp_groundth!=null)
			{
				Log.d("received ground truth in resume", tmp_groundth);
				try {
					Ground_truth = new JSONObject(tmp_groundth);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d("get ground truth from service intent", Ground_truth.toString());
				
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
							Log.d("add end ", Ground_truth.toString());
							Ground_truth.put("End_ground_truth", End_truth);
							Log.d("add end finish", Ground_truth.toString());
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
							Log.d("add start", Ground_truth.toString());
							Ground_truth.put("Start_ground_truth", Start_truth);
							Log.d("add start finish", Ground_truth.toString());
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
		Intent service_intent = getIntent();
		Log.d("service intent in onstart", service_intent.toString());
//		Bundle b = service_intent.getExtras();
//		if (b!=null){
//			
//			String tmp_groundth =(String) b.get("ground_truth"); 
//			Log.d("received ground truth on start", tmp_groundth);
//			String tmp_groundth = readFromFile("GroundTruthFile.txt");
//			Log.d("received ground truth in activity", tmp_groundth);

//			try {
//				Ground_truth = new JSONObject(tmp_groundth);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			Log.d("get ground truth from service intent", Ground_truth.toString());
//			WriteGroundth("");
//			final CharSequence[] items = { "Indoor", "Outdoor", "Unknown" };
//
//			
//			// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//			// WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setTitle("At the END of the call");
//			builder.setItems(items, new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int item) {
//					switch (item) {
//					case 0:
//						End_truth = -1;
//						break;
//					case 1:
//						End_truth = 1;
//						break;
//					case 2:
//						End_truth = 0;
//						break;
//					default:
//						End_truth = -10;
//					}
//					try {
//						Log.d("add end ", Ground_truth.toString());
//						Ground_truth.put("End_ground_truth", End_truth);
//						Log.d("add end finish", Ground_truth.toString());
//					} catch (JSONException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//
//					SendInformation();
//					Log.d(TAG, Integer.toString(End_truth));
//				}
//			});
//			AlertDialog alert = builder.create();
//			alert.show();
//
//			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
//			builder2.setTitle("At the START of the call ");
//			builder2.setItems(items, new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int item2) {
//					switch (item2) {
//					case 0:
//						Start_truth = -1;
//						break;
//					case 1:
//						Start_truth = 1;
//						break;
//					case 2:
//						Start_truth = 0;
//						break;
//					default:
//						Start_truth = -10;
//					}
//					try {
//						Log.d("add start", Ground_truth.toString());
//						Ground_truth.put("Start_ground_truth", Start_truth);
//						Log.d("add start finish", Ground_truth.toString());
//					} catch (JSONException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					// Do something with the selection
//					Log.d(TAG, Integer.toString(Start_truth));
//				}
//			});
//			AlertDialog alert2 = builder2.create();
//			alert2.show();
//
//			try {
//				location1 = Ground_truth.getString("Location1");
//				location2 = Ground_truth.getString("Location2");
//				textLocationStart_reading.setText("Location(call start): " + location1);
//				textLocationEnd_reading.setText("Location(call  end):  " + location2);
//
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			try {
//				Result1 = Ground_truth.getJSONObject("result");
//				Result2 = Ground_truth.getJSONObject("result2");
//				result1_Str = Result1.getString("Result");
//				result2_Str = Result2.getString("Result");
//				calculate_mode1 = Result1.getInt("mode");
//				calculate_mode2 = Result2.getInt("mode");
//				printResults(result1_Str, 1, calculate_mode1);
//				printResults(result2_Str, 2, calculate_mode2);
//
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	
	}
	
	@Override
	public void onRestart() {
	    super.onRestart();
	    Log.d("status", "onRestart");
		Intent service_intent = getIntent();
		Log.d("service intent in onRestart", service_intent.toString());
	    //registerReceiver(broadcastReceiver, new IntentFilter("broadCastName"));
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
	
	
	
//	BroadcastReceiver broadcastReceiver =  new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
// 
//			
//			StartTransmit = 0;
//			LocationFlag = 0;
//			
//			
//			if (waitForSending!= null)
//			{
//				if (!waitForSending.isInterrupted());
//				{
//					waitForSending.interrupt();
//					waitForSending = null;
//					Log.d("stop waitForSending at first", "stop waitForSending at first");
//					
//				}
//			}
//            Bundle b = intent.getExtras();
//            if (Ground_truth == null)
//            {
//            	Ground_truth = new JSONObject();
//            }
//
//			try {
//				tmp_call = new JSONObject(b.getString("message"));
//				callType = tmp_call.getInt("callType");	
//				Log.d("received", tmp_call.toString());
//				Log.d("call Type", String.valueOf(callType));
//			} catch (JSONException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//			}
//			
//			if (callType ==1){
//	            if (Ground_truth != null)
//	            {	Ground_truth = null;
//	            	Ground_truth = new JSONObject();
//	            }
//				try {
//					Ground_truth.put("callStart", tmp_call);
//					Log.d("Ground truth with call start", Ground_truth.toString());
//				} catch (JSONException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			
//			if (callType ==2) {
//				
//				try {
//					Ground_truth.put("callEnd", tmp_call);
//					
//					Log.d("Ground truth with call end", Ground_truth.toString());
//				} catch (JSONException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//				
//				
//				
//				if (GroundTruthFile.exists()) {
//
//					try {
//
//						foutGroundTruth = new FileOutputStream(GroundTruthFile, true);
//						outwriterGroundTruth = new OutputStreamWriter(foutGroundTruth);
//
//					} catch (Exception e) {
//
//					}
//				}
//				Log.d("start sending in calltype2", "start sending in calltype2");
//				waitSending();
//
//				final CharSequence[] items = { "Indoor", "Outdoor", "Unknown" };
//
//				// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//				// WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//				AlertDialog.Builder builder = new AlertDialog.Builder(context);
//				builder.setTitle("At the END of the call");
//				builder.setItems(items, new DialogInterface.OnClickListener() {
//					public void onClick(DialogInterface dialog, int item) {
//						switch (item) {
//						case 0:
//							End_truth = -1;
//							break;
//						case 1:
//							End_truth = 1;
//							break;
//						case 2:
//							End_truth = 0;
//							break;
//						default:
//							End_truth = -10;
//						}
//						long timeSta = System.currentTimeMillis();
//
//						try {
//							Log.d("add end ", Ground_truth.toString());
//							Ground_truth.put("End_ground_truth", End_truth);
//							Log.d("add end finish", Ground_truth.toString());
//						} catch (JSONException e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						}
//
//						//SendInformation();
//
//						try {
//							tmp_truth.put("End_ground_truth", End_truth);
//						} catch (JSONException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//
//						StartTransmit = StartTransmit + 1;
//
//						writeJSON(outwriterGroundTruth, timeSta, "Ground_truth", tmp_truth.toString());
//						tmp_truth = null;
//						tmp_truth = new JSONObject();
//
//						try {
//							outwriterGroundTruth.flush();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						try {
//							// outwriterProxi.flush();
//							outwriterGroundTruth.close();
//							outwriterGroundTruth.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						Log.d(TAG, Integer.toString(End_truth));
//					}
//				});
//				AlertDialog alert = builder.create();
//				alert.show();
//
//				AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
//				builder2.setTitle("At the START of the call ");
//				builder2.setItems(items, new DialogInterface.OnClickListener() {
//					public void onClick(DialogInterface dialog, int item2) {
//						switch (item2) {
//						case 0:
//							Start_truth = -1;
//							break;
//						case 1:
//							Start_truth = 1;
//							break;
//						case 2:
//							Start_truth = 0;
//							break;
//						default:
//							Start_truth = -10;
//						}
//						try {
//							Log.d("add start", Ground_truth.toString());
//							Ground_truth.put("Start_ground_truth", Start_truth);
//							Log.d("add start finish", Ground_truth.toString());
//						} catch (JSONException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						try {
//							tmp_truth.put("Start_groud_truth", Start_truth);
//						} catch (JSONException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						// Do something with the selection
//						Log.d(TAG, Integer.toString(Start_truth));
//					}
//				});
//				AlertDialog alert2 = builder2.create();
//				alert2.show();
//			}
//			getLocation(callType);
//			processInfo();
//        }
//    };
//    
        
//    public void getLocation(int CallType){
//    	
//    	if (Location_info == null){
//    		Location_info = new JSONObject();
//    	}
//    	
//    	String location_tmp;
//    	
//        if (mLastLocation != null)
//        {
//            location_tmp = Double.toString(mLastLocation.getLatitude()) + " "+Double.toString(mLastLocation.getLongitude());
//
//        }
//        else
//        {
//        	
//           
//            if (location_global.length()>2)
//            {
//            	location_tmp = location_global;
//            }
//            else
//            {
//            	location_tmp ="fail";
//            		
//            }    
//        }
//        
//        Log.d("Location current", location_tmp);
//        
//        if (CallType == 1)
//        {
//        	try {
//				Location_info.put("Location1", location_tmp);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        	textLocationStart_reading.setText("Location(call start): "+location_tmp);
//        }
//        if (CallType ==2){
//        	try {
//				Location_info.put("Location2", location_tmp);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        	textLocationEnd_reading.setText("Location(call  end):  "+location_tmp);
//        }
//        
//        if ((Location_info.has("Location2")) && (Ground_truth !=null)){
//        	try {
//				Ground_truth.put("Location", Location_info);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        }
//        
//        Log.d("Location info", Location_info.toString());
//    }
    
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
		Log.d("send start", Ground_truth.toString());
		if (waitForSending!= null)
		{
			if (!waitForSending.isInterrupted());
			{
				waitForSending.interrupt();
				waitForSending = null;
				Log.d("stop waitForSending", "stop waitForSending in SendInformation");
				
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
							Log.d("satify requirements", "triger the SendInformation ");
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
			Log.d("locationChanged", location_global);
		}

	}
	
	public void getLocation() {

		if (LocationThread != null) {
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				Log.d("stop LocationThread", "stop LocationThread");

			}
		}
		start_location_time = System.currentTimeMillis();
		stop_location_time = start_location_time + 3000;
	    Log.d("start_location_time start", String.valueOf(start_location_time));
		Log.d("stop_location_time start", String.valueOf(stop_location_time));

		if (LocationThreadstart == 0) {
			LocationThreadstart = 1;
			Log.d("start Locationthread", "start Location thread");
			LocationThread = new Thread() {

				public void run() {
					while (!Thread.interrupted()) {

						while (true) {

							if (start_location_time > stop_location_time) {
								Log.d("start_location_time", String.valueOf(start_location_time));
								Log.d("stop_location_time", String.valueOf(stop_location_time));
								location2 = "fail";
								break;
							}

							if ((mLastLocation != null) || (location_global.length() > 2)) {
								Log.d("mLastLocation", mLastLocation.toString());
								Log.d("location_global", location_global);
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
		
		Log.d("put location 2", location2);

		if (LocationThread != null) {
			if (!LocationThread.isInterrupted())
			{
				LocationThread.interrupt();
				LocationThread = null;
				Log.d("stop LocationThread", "stop LocationThread in writing location");

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
//	
//	
//    private String svmPredictResult(double Light_Sum, double R_Sum, double G_Sum, double B_Sum, double W_Sum){
//		double r_to_b = R_Sum/B_Sum;
//		double g_to_b = G_Sum/B_Sum;
//		double r_to_g = R_Sum/G_Sum;
////		double r_to_w = R_Sum/W_Sum;
////		double g_to_w = G_Sum/W_Sum;
////		double b_to_w = B_Sum/W_Sum;
////		double r_to_l = R_Sum/Light_Sum;
////		double g_to_l = G_Sum/Light_Sum;
////		double b_to_l = B_Sum/Light_Sum;
////		double w_to_l = W_Sum/Light_Sum;	
//	
//	
//		String Str_test_input = "0"+" "+"1:"+Double.toString(Light_Sum)+" "+
//				"2:"+Double.toString(r_to_b)+" "+"3:"+Double.toString(g_to_b)+" "+"4:"+Double.toString(r_to_g)+"\n";
////				"5:"+Double.toString(r_to_w)+" "+"6:"+Double.toString(g_to_w)+" "+"7:"+Double.toString(b_to_w)+" "+
////				"8:"+Double.toString(r_to_l)+" "+"9:"+Double.toString(g_to_l)+" "+"10:"+Double.toString(b_to_l)+" "+"11:"+Double.toString(w_to_l)+"\n";
//		if(TestInputFile.exists()){
//			  
//			  try{
//	    		
//				  foutTestInput = new FileOutputStream(TestInputFile,false);
//				  outwriterTestInput= new OutputStreamWriter(foutTestInput);
//			
//			  } catch(Exception e)
//			  {
//
//			  }
//		}
//		
//		try {
//			outwriterTestInput.append(Str_test_input);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		try {
//			outwriterTestInput.flush();
//			outwriterTestInput.close();
//			foutTestInput.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		String Str_dir = dir + "/";
//		String Str_model = Str_dir+"model_light";
//		String Str_range = Str_dir+"range_light";
//		String Str_test = Str_dir + "TestInput";
//		String Str_scale = Str_dir+"TestInput_scale";
//		String Str_result = Str_dir + "Detect_result";
//		
//		Cmd_svm_scale = "-r "+Str_range+" "+Str_test+" "+Str_scale;
//		Cmd_svm_predict = "-b 1 "+Str_scale+" "+Str_model+" "+Str_result;
//		jniSvmScale(Cmd_svm_scale);
//		jniSvmPredict(Cmd_svm_predict);
//		
//		File SvmResultFile = new File(Str_result);
//		BufferedReader bufferedReader_svm = null;
//		try {
//			bufferedReader_svm = new BufferedReader(new FileReader(SvmResultFile));
//		} catch (FileNotFoundException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//
//		StringBuilder finalString = new StringBuilder();
//
//		if (bufferedReader_svm != null) {
//			String line;
//			try {
//				int count = 0;
//				while ((line = bufferedReader_svm.readLine()) != null) {
//					if (count==0){
//						count = 1;
//						continue;
//					}
//					finalString.append(line);
//					Log.d(TAG, line);
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		try {
//			bufferedReader_svm.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String Str_Result = finalString.toString();
//		String [] splitStr = Str_Result.split("\\s+");
//		int Tmp_result = Integer.parseInt(splitStr[0]);
//		Double Tmp_con = 0.0;
//		if ( Tmp_result ==-1){
//			Tmp_con = Double.parseDouble(splitStr[2]);
//		}
//		else{
//			Tmp_con = Double.parseDouble(splitStr[1]);
//			
//		}
//		Str_Result = String.valueOf(Tmp_result) +" "+String.valueOf(Tmp_con);
//		Log.d("get light results", Str_Result);
//		calculate_mode = 1;
//		return Str_Result;  	   	
//    }
//    
//    
//    private String NightPredict(double Light_Sum, double Wifi_Sum){
//    	
//    	double Light_threshold = 30;
//    	double Wifi_threshold = -70;
//    	
//    	int Result = 0;
//    	double Result_con = 0.0;
//    	int Light_Result = 0;
//    	double Light_con = 0.0;
//    	int Wifi_Result = 0;
//    	double Wifi_con = 0.0;
//    	String Str_return_result;
//
//		if (Light_Sum > Light_threshold) {
//			Light_Result = -1;
//			Light_con = (Light_Sum - Light_threshold) / Light_Sum;
//		} else {
//			Light_Result = 1;
//			Light_con = ((Light_threshold - Light_Sum) / Light_threshold) * 0.9;
//
//		}
//
//		if (Wifi_Sum == (-127)) {
//			Wifi_Result = 0;
//		} else {
//			if (Wifi_Sum > (Wifi_threshold)) {
//				Wifi_Result = -1;
//				Wifi_con = (Wifi_Sum - Wifi_threshold) / Wifi_threshold + 0.6;
//			} else {
//				Wifi_Result = 1;
//				Wifi_con = (Wifi_threshold - Wifi_Sum) / Wifi_threshold + 0.6;
//			}
//		}
//
//		if (Wifi_Result == 0) {
//			Result = Light_Result;
//			Result_con = Light_con;
//		} else {
//			if (Light_Result == Wifi_Result) {
//				Result = Light_Result;
//				Result_con = 1 - (1 - Light_con) * (1 - Wifi_con);
//			} else {
//				if (Light_con > Wifi_con) {
//					Result = Light_Result;
//					Result_con = Light_con * (1 - Wifi_con);
//				} else {
//					Result = Wifi_Result;
//					Result_con = Wifi_con * (1 - Light_con);
//				}
//			}
//		}
//
//		Str_return_result = Integer.toString(Result) + " " + Double.toString(Result_con);
//		calculate_mode = 2;
//		if ((Wifi_Sum < (-105)) && (Light_Sum < 2)) {
//			Str_return_result = "0" +" "+"0.0";
//			calculate_mode = 5;
//		}
//		return Str_return_result;
//	   	
//    }
//    
//    
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
	 

	
}
