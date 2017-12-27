package com.example.vikas.tinyweather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.entity.mime.Header;

/**
 * Created by VIKAS on 12/28/2017.
 */

public class WeatherController extends AppCompatActivity {


    final int REQUEST_CODE = 123; // Request Code for permission request callback
    final int NEW_CITY_CODE = 456; // Request code for starting new activity for result callback
        final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";


    final String APP_ID = "e72ca729af228beabd5d20e3b7749713";


    final long MIN_TIME = 5000;


    final float MIN_DISTANCE = 1000;


    final String LOGCAT_TAG = "Clima";



    final String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;
    boolean mUseLocation = true;
    TextView mCityLabel;
    ImageView mWeatherImage;
    TextView mTemperatureLabel;


    LocationManager mLocationManager;
    LocationListener mLocationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_controller_layout);

        mCityLabel = findViewById(R.id.locationTV);
        mWeatherImage = findViewById(R.id.weatherSymbolIV);
        mTemperatureLabel = findViewById(R.id.tempTV);
        ImageButton changeCityButton = findViewById(R.id.changeCityButton);


        changeCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(WeatherController.this, ChangeCityController.class);

                                startActivityForResult(myIntent, NEW_CITY_CODE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOGCAT_TAG, "onResume() called");
        if(mUseLocation) getWeatherForCurrentLocation();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(LOGCAT_TAG, "onActivityResult() called");

        if (requestCode == NEW_CITY_CODE) {
            if (resultCode == RESULT_OK) {
                String city = data.getStringExtra("City");
                Log.d(LOGCAT_TAG, "New city is " + city);

                mUseLocation = false;
                getWeatherForNewCity(city);
            }
        }
    }


    private void getWeatherForNewCity(String city) {
        Log.d(LOGCAT_TAG, "Getting weather for new city");
        RequestParams params = new RequestParams();
        params.put("q", city);
        params.put("appid", APP_ID);

        letsDoSomeNetworking(params);
    }



    private void getWeatherForCurrentLocation() {

        Log.d(LOGCAT_TAG, "Getting weather for current location");
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                Log.d(LOGCAT_TAG, "onLocationChanged() callback received");
                String longitude = String.valueOf(location.getLongitude());
                String latitude = String.valueOf(location.getLatitude());

                Log.d(LOGCAT_TAG, "longitude is: " + longitude);
                Log.d(LOGCAT_TAG, "latitude is: " + latitude);


                RequestParams params = new RequestParams();
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("appid", APP_ID);
                letsDoSomeNetworking(params);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Log statements to help you debug your app.
                Log.d(LOGCAT_TAG, "onStatusChanged() callback received. Status: " + status);
                Log.d(LOGCAT_TAG, "2 means AVAILABLE, 1: TEMPORARILY_UNAVAILABLE, 0: OUT_OF_SERVICE");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(LOGCAT_TAG, "onProviderEnabled() callback received. Provider: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(LOGCAT_TAG, "onProviderDisabled() callback received. Provider: " + provider);
            }
        };

        // This is the permission check to access (fine) location.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }


        Log.d(LOGCAT_TAG, "Location Provider used: "
                + mLocationManager.getProvider(LOCATION_PROVIDER).getName());
        Log.d(LOGCAT_TAG, "Location Provider is enabled: "
                + mLocationManager.isProviderEnabled(LOCATION_PROVIDER));
        Log.d(LOGCAT_TAG, "Last known location (if any): "
                + mLocationManager.getLastKnownLocation(LOCATION_PROVIDER));
        Log.d(LOGCAT_TAG, "Requesting location updates");


        mLocationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, mLocationListener);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (requestCode == REQUEST_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOGCAT_TAG, "onRequestPermissionsResult(): Permission granted!");


                getWeatherForCurrentLocation();
            } else {
                Log.d(LOGCAT_TAG, "Permission denied =( ");
            }
        }

    }




   private void letsDoSomeNetworking (RequestParams params){

       AsyncHttpClient client = new AsyncHttpClient();
       client.get(WEATHER_URL,params,new JsonHttpResponseHandler(){

                   @Override
                   public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                       Log.d(LOGCAT_TAG, "Success! JSON: " + response.toString());
                       WeatherDataModel weatherData = WeatherDataModel.fromJson(response);
                       updateUI(weatherData);
                   }

                   @Override
                   public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable e, JSONObject errorResponse) {
                       Log.e(LOGCAT_TAG, "Fail " + e.toString());
                       Toast.makeText(WeatherController.this, "Request Failed", Toast.LENGTH_SHORT).show();

                       Log.d(LOGCAT_TAG, "Status code " + statusCode);
                       Log.d(LOGCAT_TAG, "Here's what we got instead " + errorResponse.toString());
                   }
               }

       );


   }



    private void updateUI(WeatherDataModel weather) {
        mTemperatureLabel.setText(weather.getTemperature());
        mCityLabel.setText(weather.getCity());


        int resourceID = getResources().getIdentifier(weather.getIconName(), "drawable", getPackageName());
        mWeatherImage.setImageResource(resourceID);
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mLocationManager != null) mLocationManager.removeUpdates(mLocationListener);
    }

}
