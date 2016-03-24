package com.example.mycallreceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

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

public class MainActivity extends Activity {
	
	private static final String TAG = "Diag";
	int Start_truth = -10;
	int End_truth = -10;
    NotificationManager manager;
    Notification myNotication;
	public static JSONObject result = new JSONObject();
	public static int raw_counter = -100;
	
	File GroundTruthFile = null;
	FileOutputStream foutGroundTruth = null;
	OutputStreamWriter outwriterGroundTruth = null;
	File root = null;
	File dir = null;
	String Ground_truth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		registerReceiver(broadcastReceiver, new IntentFilter("broadCastName")); 
		root = android.os.Environment.getExternalStorageDirectory();
		dir = new File (root.getAbsolutePath() + "/CallDetection");
		dir.mkdirs();
		GroundTruthFile = new File(dir, "GroundTruth.txt");
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
	public void onResume() {
		super.onResume();
		registerReceiver(broadcastReceiver, new IntentFilter("broadCastName"));
	}
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    unregisterReceiver(broadcastReceiver);
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
	
	
	BroadcastReceiver broadcastReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
 
            Bundle b = intent.getExtras();
 
            String message = b.getString("message");
            Ground_truth = message;
            Log.d("newmesage", "" + Ground_truth);
            
			if(GroundTruthFile.exists()){
				  
				  try{
		    		
					  foutGroundTruth = new FileOutputStream(GroundTruthFile,true);
					  outwriterGroundTruth= new OutputStreamWriter(foutGroundTruth);
				
				  } catch(Exception e)
				  {

				  }
			}
			
			
//			NotificationCompat.Builder mBuilder =
//			        new NotificationCompat.Builder(context)
//			        .setSmallIcon(R.drawable.ic_launcher)
//			        .setContentTitle("My notification")
//			        .setContentText("Hello World!");
//			// Creates an explicit intent for an Activity in your app
//			Intent resultIntent = new Intent(context, MainActivity.class);
//
//			// The stack builder object will contain an artificial back stack for the
//			// started Activity.
//			// This ensures that navigating backward from the Activity leads out of
//			// your application to the Home screen.
//			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//			// Adds the back stack for the Intent (but not the Intent itself)
//			stackBuilder.addParentStack(MainActivity.class);
//			// Adds the Intent that starts the Activity to the top of the stack
//			stackBuilder.addNextIntent(resultIntent);
//			PendingIntent resultPendingIntent =
//			        stackBuilder.getPendingIntent(
//			            0,
//			            PendingIntent.FLAG_UPDATE_CURRENT
//			        );
//			mBuilder.setContentIntent(resultPendingIntent);
//			NotificationManager mNotificationManager =
//			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//			// mId allows you to update the notification later on.
//			mNotificationManager.notify(123, mBuilder.build());

            final CharSequence[] items = {
                    "Indoor", "Outdoor", "Unknown"
            };

//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("At the END of the call");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                	switch (item){
            		case 0: End_truth = -1;
            				break;
            		case 1: End_truth = 1;
            				break;
            		case 2: End_truth = 0;
        					break;
            		default:
            				End_truth = -10;
            				}
                	long timeSta = System.currentTimeMillis();
                	Ground_truth = Ground_truth + " End: "+Integer.toString(End_truth);
                    writeJSON(outwriterGroundTruth,timeSta,"ground_truth",Ground_truth);
                    writeFinalJSON(outwriterGroundTruth,timeSta,"ground_truth",Ground_truth);
    	            try {
    	            	outwriterGroundTruth.flush();
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    				try {
    					//outwriterProxi.flush();
    					outwriterGroundTruth.close();
    					outwriterGroundTruth.close();
    				} catch (IOException e) {
    						// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
                	Log.d(TAG, Integer.toString(End_truth));
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            
            
            AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
            builder2.setTitle("At the START of the call ");
            builder2.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item2) {
                	switch (item2){
            		case 0: Start_truth = -1;
            				break;
            		case 1: Start_truth = 1;
            				break;
            		case 2: Start_truth = 0;
        					break;
            		default:
            				Start_truth = -10;
            				}
                	Ground_truth = Ground_truth + " Start: "+Integer.toString(Start_truth);
                    // Do something with the selection
                	Log.d(TAG, Integer.toString(Start_truth));
                }
            });
            AlertDialog alert2 = builder2.create();
            alert2.show();
            
            
            Log.d("newmesage", "" + Ground_truth);
        }
    };
    
    
    
    
    public void writeJSON(OutputStreamWriter myWriter, long timestamp, String tag, String info) {
 	   JSONObject object = new JSONObject();
 	   try {

 	      object.put("timestamp", timestamp);
 	      object.put(tag, info);
 	           result.put("raw"+String.valueOf(raw_counter),object);
 	          
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

	public void writeFinalJSON(OutputStreamWriter myWriter, long timestamp, String tag, String info) {
		try {
			JSONObject object = new JSONObject();
			object.put("timestamp", timestamp);
			object.put(tag, info);
			Log.d("JACK", result.toString());
			if (result.has("result")) {
				result.put("result2", object);
			} else {
				result.put("result", object);
			}
			Thread t = new Thread() {

				public void run() {
					Looper.prepare(); // For Preparing Message Pool for the
										// child Thread
					HttpClient client = new DefaultHttpClient();
					HttpResponse response;
					JSONObject json = new JSONObject();

					try {
						HttpPost post = new HttpPost("http://psychic-rush-755.appspot.com/upload");
						StringEntity se = new StringEntity(result.toString());
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
						result = null;
						result = new JSONObject();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			if (result.has("result2")) {
				t.start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
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
}
