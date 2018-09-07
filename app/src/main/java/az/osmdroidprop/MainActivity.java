package az.osmdroidprop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int MULTIPLE_PERMISSION_REQUEST_CODE = 4;
    private MapView mapView;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private Integer vehiclesNearby = 0;
    private Integer totalVehicles = 0;

    private Map<String, Vehicle> tramPositionsMap;
    private Map<String, Vehicle> busPositionsMap;

    private boolean currentlySearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        checkPermissionsState();

        tramPositionsMap = new LinkedHashMap<>();
        busPositionsMap = new LinkedHashMap<>();
    }

    private void checkPermissionsState() {
        int internetPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);

        int networkStatePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE);

        int writeExternalStoragePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        int coarseLocationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        int fineLocationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        int wifiStatePermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_WIFI_STATE);

        if (internetPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                networkStatePermissionCheck == PackageManager.PERMISSION_GRANTED &&
                writeExternalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                fineLocationPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                wifiStatePermissionCheck == PackageManager.PERMISSION_GRANTED) {

            setupMap();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE},
                    MULTIPLE_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean somePermissionWasDenied = false;
                    for (int result : grantResults) {
                        if (result == PackageManager.PERMISSION_DENIED) {
                            somePermissionWasDenied = true;
                        }
                    }
                    if (somePermissionWasDenied) {
                        Toast.makeText(this, "Cant load maps without all the permissions granted", Toast.LENGTH_SHORT).show();
                    } else {
                        setupMap();
                    }
                } else {
                    Toast.makeText(this, "Cant load maps without all the permissions granted", Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }

    private void setupMap() {

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mapView.getController().setZoom(15);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

        IMyLocationProvider myLocationProvider = new IMyLocationProvider() {
            @Override
            public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
                return false;
            }

            @Override
            public void stopLocationProvider() {

            }

            @Override
            public Location getLastKnownLocation() {
                return null;
            }

            @Override
            public void destroy() {

            }
        };

        MyLocationNewOverlay oMapLocationOverlay = new MyLocationNewOverlay(myLocationProvider, mapView);
        mapView.getOverlays().add(oMapLocationOverlay);
        oMapLocationOverlay.enableFollowLocation();
        oMapLocationOverlay.enableMyLocation();
        oMapLocationOverlay.enableFollowLocation();

        CompassOverlay compassOverlay = new CompassOverlay(this, mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        mapView.setMapListener(new DelayedMapListener(new MapListener() {
            public boolean onZoom(final ZoomEvent e) {
                UpdateCurrentLocationTextView("zoom");
                if (!currentlySearching)
                    filterVehiclePositionsByBoundingBox();
                return true;
            }

            public boolean onScroll(final ScrollEvent e) {
                UpdateCurrentLocationTextView("scroll");
                if (!currentlySearching)
                    filterVehiclePositionsByBoundingBox();
                return true;
            }
        }, 1000));
    }

    private void UpdateCurrentLocationTextView(String zoom) {
        MapView mapView = (MapView) findViewById(R.id.mapview);

        String latitudeStr = "" + mapView.getMapCenter().getLatitude();
        String longitudeStr = "" + mapView.getMapCenter().getLongitude();

        String latitudeFormattedStr = latitudeStr.substring(0, Math.min(latitudeStr.length(), 7));
        String longitudeFormattedStr = longitudeStr.substring(0, Math.min(longitudeStr.length(), 7));

        Log.i(zoom, "" + mapView.getMapCenter().getLatitude() + ", " + mapView.getMapCenter().getLongitude());
        TextView latLongTv = (TextView) findViewById(R.id.textView);
        latLongTv.setText("" + latitudeFormattedStr + ", " + longitudeFormattedStr);
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void setCenterInMyCurrentLocation() {
        if (mLastLocation != null) {
            mapView.getController().setCenter(new GeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

        } else {
            Toast.makeText(this, "Getting current location", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVehiclePositionsOverlay(List<IGeoPoint> points) {
        final SimpleFastPointOverlay sfpo = getSimpleFastPointOverlay(points);
        mapView.getOverlays().add(sfpo);
    }

    @NonNull
    private SimpleFastPointOverlay getSimpleFastPointOverlay(List<IGeoPoint> points) {
        SimplePointTheme pt = new SimplePointTheme(points, true);

        Paint textStyle = new Paint();
        textStyle.setStyle(Paint.Style.FILL);
        textStyle.setColor(Color.parseColor("#000000"));
        textStyle.setTextAlign(Paint.Align.CENTER);
        textStyle.setTextSize(24);

        Paint Style = new Paint();
        Style.setColor(Color.parseColor("#336699"));

        SimpleFastPointOverlayOptions opt = SimpleFastPointOverlayOptions.getDefaultStyle()
                .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                .setRadius(7).setIsClickable(false).setCellSize(15).setTextStyle(textStyle).setPointStyle(Style);

        final SimpleFastPointOverlay sfpo = new SimpleFastPointOverlay(pt, opt);

        sfpo.setOnClickListener(new SimpleFastPointOverlay.OnClickListener() {
            @Override
            public void onClick(SimpleFastPointOverlay.PointAdapter points, Integer point) {
                Toast.makeText(mapView.getContext()
                        , "You clicked " + ((LabelledGeoPoint) points.get(point)).getLabel()
                        , Toast.LENGTH_SHORT).show();
            }
        });
        return sfpo;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem mSearch = menu.findItem(R.id.action_search);

        SearchView mSearchView = (SearchView) mSearch.getActionView();
        mSearchView.setQueryHint("Tram or bus names separated by space");

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String[] names = query.split(" ");
                filterVehiclePositionsByNames(names);
                currentlySearching = true;
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                filterVehiclePositionsByBoundingBox();
                currentlySearching = false;
                return false;
            }
        });

        return true;
    }

    private void filterVehiclePositionsByNames(String[] names) {
        List<IGeoPoint> points = new LinkedList<>();
        clearVehiclePointsOverlay();

        for (String name: names) {
            for (Map.Entry<String, Vehicle> tram: tramPositionsMap.entrySet()) {
                if (tram.getKey().equals(name)) {
                    Vehicle value = tram.getValue();
                    points.add(new LabelledGeoPoint(value.lat, value.lon, value.line));
                }
            }
            for (Map.Entry<String, Vehicle> bus: busPositionsMap.entrySet()) {
                if (bus.getKey().equals(name)) {
                    Vehicle value = bus.getValue();
                    points.add(new LabelledGeoPoint(value.lat, value.lon, value.line));
                }
            }
        }

        updateVehiclePositionsOverlay(points);
        vehiclesNearby = points.size();

        Toast.makeText(this, "Showing buses " + vehiclesNearby + " of total " + totalVehicles + " (" + names + ")", Toast.LENGTH_SHORT).show();

    }

    private void filterVehiclePositionsByBoundingBox() {
        List<IGeoPoint> points = new LinkedList<>();
        clearVehiclePointsOverlay();
        BoundingBox boundingBox = mapView.getProjection().getBoundingBox();

        Double rangeInKm = boundingBox.getDiagonalLengthInMeters() / 1000;

        for (Map.Entry<String, Vehicle> tram: tramPositionsMap.entrySet()) {
            Vehicle vehicle = tram.getValue();
            if (boundingBox.contains(vehicle.lat, vehicle.lon))
                points.add(new LabelledGeoPoint(vehicle.lat, vehicle.lon, vehicle.line));
        }
        for (Map.Entry<String, Vehicle> bus: busPositionsMap.entrySet()) {
            Vehicle vehicle = bus.getValue();
            if (boundingBox.contains(vehicle.lat, vehicle.lon))
                points.add(new LabelledGeoPoint(vehicle.lat, vehicle.lon, vehicle.line));
        }

        updateVehiclePositionsOverlay(points);
        vehiclesNearby = points.size();

        Toast.makeText(this, "Showing buses " + vehiclesNearby + " of total " + totalVehicles + " (" + rangeInKm + " km range)", Toast.LENGTH_SHORT).show();
    }

    private void clearVehiclePointsOverlay() {
        List<Overlay> overlays = mapView.getOverlays();
        for (int i = 0; i < mapView.getOverlays().size(); i++) {
            if (overlays.get(i) instanceof SimpleFastPointOverlay) {
                overlays.remove(i);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_locate) {
            setCenterInMyCurrentLocation();
        } else if (id == R.id.action_refresh) {
            refreshVehiclePositions();
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshVehiclePositions() {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    vehiclesNearby = 0;
                    clearVehiclePointsOverlay();

                    UpdateVehiclePositions updateVehiclePositions = new UpdateVehiclePositions().invoke();
                    busPositionsMap = updateVehiclePositions.getBusPositionsMap();
                    tramPositionsMap = updateVehiclePositions.getTramPositionsMap();
                    filterVehiclePositionsByBoundingBox();

                    totalVehicles = busPositionsMap.size() + tramPositionsMap.size();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private class UpdateVehiclePositions {
        private Map<String, Vehicle> busPositionsMap;
        private Map<String, Vehicle> tramPositionsMap;

        public Map<String, Vehicle> getBusPositionsMap() {
            return busPositionsMap;
        }

        public Map<String, Vehicle> getTramPositionsMap() {
            return tramPositionsMap;
        }

        public UpdateVehiclePositions invoke() throws IOException, JSONException {
            VehiclePositions busPositions = new VehiclePositions("Bus");
            busPositionsMap = busPositions.fetchAll();

            VehiclePositions tramPositions = new VehiclePositions("Tram");
            tramPositionsMap = tramPositions.fetchAll();

            //filterVehiclePositionsByBoundingBox();
            totalVehicles = busPositionsMap.size() + tramPositionsMap.size();
            return this;
        }
    }
}
