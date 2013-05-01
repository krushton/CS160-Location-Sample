package com.krushton.locationexample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class LocationActivity extends Activity implements LocationListener {

	LocationManager manager;
	String providerName;
	Location lastKnownLocation;
	Date lastUpdate;
	
	//use dummy user (only 1 exists in the database for testing)
	private final String USER_ID = "1";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);
		checkLocation();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.location, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Start updates)
		if (providerName != null) {
			manager.requestLocationUpdates(providerName, 15000, 1, this);
		}
	}
	
	@Override
		protected void onPause() {
		super.onPause();
		// Stop updates while the app is paused and store location
		manager.removeUpdates(this);
	}

	
	public void checkLocation() {

		//initialize location manager
		manager =  (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		//check if GPS is enabled
		//if not, notify user with a toast
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
	    	Toast.makeText(this, "GPS is disabled.", Toast.LENGTH_SHORT).show();
	    } else {

	    	//get a location provider from location manager
	    	//empty criteria searches through all providers and returns the best one
	    	providerName = manager.getBestProvider(new Criteria(), true);
	    	lastKnownLocation = manager.getLastKnownLocation(providerName);

	    	TextView tv = (TextView)findViewById(R.id.myLocation);
	    	if (lastKnownLocation != null) {
	    		tv.setText(lastKnownLocation.getLatitude() + " latitude, " + lastKnownLocation.getLongitude() + " longitude");
	    	} else {
	    		tv.setText("Last known location not found. Waiting for updated location...");
	    	}
	    	//sign up to be notified of location updates every 15 seconds
	    	manager.requestLocationUpdates(providerName, 15000, 1, this);
	    }
	}
	
	@Override
	
	public void onLocationChanged(Location location) {
		
		Toast.makeText(this, "Updated location received", Toast.LENGTH_SHORT).show();
		TextView tv = (TextView)findViewById(R.id.myLocation);
		if (location == null) {
			tv.setText("Problem getting location");
			return;
		}
		
		//the first time the app runs, onLocationChanged will just update the lastKnownLocation and updateTime.
		//the next time onLocationChanged is called, it will compare the current time against the last update time. 
		//If the time is less than one hour, then it adds a new segment with D = distance between currentLocation and last known
		//and T = current time - last update time
		//otherwise it should just update the last known location to the current location and set the time to the current time

		if (lastUpdate != null && lastKnownLocation != null) {
			
			Date d = new Date();
    		long interval = (d.getTime() - lastUpdate.getTime()) / 1000; //time in seconds
    		float distance = location.distanceTo(lastKnownLocation);
    		
			if (interval > 0 && interval < 3600) {	
				addSegment(distance, interval);
			} 
		} 
		lastKnownLocation = location;
		lastUpdate = new Date();
		
		tv.setText(lastKnownLocation.getLatitude() + " latitude, " + lastKnownLocation.getLongitude() + " longitude");
		
		
	}
	
	public void refresh(View v) {
		GetStatsTask task = new GetStatsTask();
		task.execute();
	}
	
	public void addSegment(float distance, long interval) {

		PostTask pt = new PostTask();
		String mode = "";
		float speed = distance/interval; //meters per second
		
		if (speed > 8.9) {
			mode = "car";
		} else if (speed > 5 && speed <= 8.9) {
			mode = "bike";
		} else {
			mode = "walk";
		}
		
		TextView modText = (TextView)findViewById(R.id.mode);
		modText.setText("Traveling by: " + mode);
		pt.execute(String.valueOf(distance), String.valueOf(interval), mode);
	}
	

	public void getMetrics(int numberOfDays) {
		GetStatsTask task = new GetStatsTask();
		task.execute();
	}
	
	@Override
	public void onProviderDisabled(String arg0) {}

	@Override
	public void onProviderEnabled(String arg0) {}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
	
	

    private class PostTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			
			Log.d("Here we are", params.toString());
			String url = "http://locationtracker.pythonanywhere.com/segments";
			
			HttpResponse response;
			HttpClient httpclient = new DefaultHttpClient();
			

				 HttpPost post = new HttpPost(url);
			     List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			     DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			     postParameters.add(new BasicNameValuePair("time", sdf.format(new Date())));
			     postParameters.add(new BasicNameValuePair("distance", params[0]));
			     postParameters.add(new BasicNameValuePair("interval", params[1]));
			     postParameters.add(new BasicNameValuePair("mode", params[2]));
			     postParameters.add(new BasicNameValuePair("user_id", USER_ID));
			     Log.d("0", params[0]);
			     Log.d("1", params[1]);
			     Log.d("2", params[2]);
			     Log.d("user", USER_ID);

			    
		         String responseString = "";

			try {      
				 UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
		         post.setEntity(entity);
 	             response = httpclient.execute(post);
	 	            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
		                ByteArrayOutputStream out = new ByteArrayOutputStream();
		                response.getEntity().writeTo(out);
		                out.close();
		                responseString = out.toString();
	
		            } else{
		                //Closes the connection.
		                response.getEntity().getContent().close();
		                throw new IOException(response.getStatusLine().getReasonPhrase());
		            }
 	            
 	            return responseString;
 	            
 	        } catch (ClientProtocolException e) {
 	            //TODO Handle problems..
 	        } catch (IOException e) {
 	            //TODO Handle problems..
 	        }

			 return null;
		}

		@Override
		protected void onPostExecute(String arg0) {
			refresh(null);
		}
    	
    }
    
    	private class GetStatsTask extends AsyncTask<Void, Void, JSONObject> {
    	
    	protected JSONObject doInBackground(Void...params) {			
			String url = "http://locationtracker.pythonanywhere.com/users/" + USER_ID + "/track";

			HttpResponse response;
			HttpClient httpclient = new DefaultHttpClient();
			String responseString = "";

			try {
	            response = httpclient.execute(new HttpGet(url));
	            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                response.getEntity().writeTo(out);
	                out.close();
	                responseString = out.toString();

	            } else{
	                //Closes the connection.
	                response.getEntity().getContent().close();
	                throw new IOException(response.getStatusLine().getReasonPhrase());
	            }
	        } catch (ClientProtocolException e) {
	            //TODO Handle problems..
	        } catch (Exception e) {
	            //TODO Handle problems..
	        }
			
			try {
				Log.d("hahahahaha", responseString);
				JSONObject userData = new JSONObject(responseString);
				return userData;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return null;
    	}
    

		protected void onPostExecute(JSONObject userData) {
			TextView carDistance = (TextView)findViewById(R.id.carDist);
			TextView carTime = (TextView)findViewById(R.id.carTime);
			
			TextView bikeDistance = (TextView)findViewById(R.id.bikeDist);
			TextView bikeTime = (TextView)findViewById(R.id.bikeTime);
			
			TextView walkDistance = (TextView)findViewById(R.id.walkDist);
			TextView walkTime = (TextView)findViewById(R.id.walkTime);
			
			try {
				JSONObject carData = userData.getJSONObject("car");
				JSONObject bikeData = userData.getJSONObject("bike");
				JSONObject walkData = userData.getJSONObject("walk");
				
				//just show month interval for now 
				carDistance.setText(carData.getJSONArray("month").getString(0));
				carTime.setText(carData.getJSONArray("month").getString(1));
				
				bikeDistance.setText(bikeData.getJSONArray("month").getString(0));
				bikeTime.setText(bikeData.getJSONArray("month").getString(1));
				
				walkDistance.setText(walkData.getJSONArray("month").getString(0));
				walkTime.setText(walkData.getJSONArray("month").getString(1));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}

    }
    
    


}
