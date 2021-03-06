package tech.geeksquad.recyte;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PlacesNearMeActivity extends AppCompatActivity {

    private static final String TAG = "places_near_me";
    private LocationManager locationManager;

    private Location lastKnownLocation;
    private LocationListener locationListener;

    private ArrayList<Places> placesArrayList;
    private PlaceAdapter placeAdapter;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_near_me);

        ListView listView = (ListView) findViewById(R.id.places_list);
        listView.setEmptyView(findViewById(R.id.loading));

        placesArrayList = new ArrayList<>();
        placeAdapter = new PlaceAdapter(this, placesArrayList);
        listView.setAdapter(placeAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                AlertDialog.Builder builder = new AlertDialog.Builder(PlacesNearMeActivity.this)
                        .setItems(new CharSequence[]{"Book a Pickup", "Directions"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(PlacesNearMeActivity.this)
                                            .setTitle("Pickup Confirmed")
                                            .setMessage("You Pickup has been confirmed. Our representative will contact you shortly.")
                                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    PlacesNearMeActivity.this.finish();
                                                }
                                            });
                                    builder1.show();
                                } else {
                                    Places places = placesArrayList.get(position);
                                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + places.getLat() + "," + places.getLon());
                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                    mapIntent.setPackage("com.google.android.apps.maps");
                                    startActivity(mapIntent);
                                }
                            }
                        });
                builder.show();
            }
        });

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastKnownLocation = location;
                getPlaces();
                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == 1) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void getPlaces() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?key=AIzaSyCi5xluntOENb_3ll2QWVo0yOM0-KbcGY4&query=recycler&location=" +
                lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();

        Log.d(TAG, "getPlaces: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < min(30, results.length()); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");

                                String name = place.getString("name");
                                String address = place.getString("formatted_address");
                                Places places = new Places(location.getDouble("lat"),
                                        location.getDouble("lng"),
                                        name, address);
                                placesArrayList.add(places);
                                Log.d(TAG, "onResponse: " + places);
                                placeAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private int min(int i, int length) {
        if (i > length)
            return length;
        else
            return i;
    }


}
