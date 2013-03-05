package com.krushton.locationexample;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class LocationActivity extends Activity implements LocationListener {

	LocationManager manager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.location, menu);
		return true;
	}
	
	public void checkLocation(View v) {
		
		//initialize location manager
		manager =  (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		//check if GPS is enabled
		//if not, notify user with a toast
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
	    	Toast.makeText(this, "GPS is disabled.", Toast.LENGTH_SHORT).show();
	    } else {
	    	
	    	//get a location provider from location manager
	    	//empty criteria searches through all providers and returns the best one
	    	String providerName = manager.getBestProvider(new Criteria(), true);
	    	Location location = manager.getLastKnownLocation(providerName);
	    	
	    	TextView tv = (TextView)findViewById(R.id.locationResults);
	    	if (location != null) {
	    		tv.setText(location.getLatitude() + " latitude, " + location.getLongitude() + " longitude");
	    	} else {
	    		tv.setText("Last known location not found. Waiting for updated location...");
	    	}
	    	//sign up to be notified of location updates every 15 seconds - for production code this should be at least a minute
	    	manager.requestLocationUpdates(providerName, 15000, 1, this);
	    }
	}
	
	@Override
	public void onLocationChanged(Location location) {
		TextView tv = (TextView)findViewById(R.id.locationResults);
    	if (location != null) {
    		tv.setText(location.getLatitude() + " latitude, " + location.getLongitude() + " longitude");
    	} else {
    		tv.setText("Problem getting location");
    	}
	}
	
	@Override
	public void onProviderDisabled(String arg0) {}

	@Override
	public void onProviderEnabled(String arg0) {}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
	
	
}
