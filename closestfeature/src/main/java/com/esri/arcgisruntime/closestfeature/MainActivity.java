package com.esri.arcgisruntime.closestfeature;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.toolkit.map.MapViewHelper;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geometry.AngularUnit;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Field;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.geocode.LocatorSuggestionParameters;
import com.esri.core.tasks.geocode.LocatorSuggestionResult;
import com.esri.core.tasks.na.ClosestFacilityParameters;
import com.esri.core.tasks.na.ClosestFacilityResult;
import com.esri.core.tasks.na.ClosestFacilityTask;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;
import com.esri.core.tasks.query.QueryParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Parker";
    // mapview definition
    MapView mMapView;
    // basemap layer
    ArcGISTiledMapServiceLayer basemapStreet;
    // feature layer
    ArcGISFeatureLayer fLayer;
    // closest facility task
    ClosestFacilityTask closestFacilityTask;
    // route definition
    Route route;
    // graphics layer to show route
    GraphicsLayer routeLayer;
    private Symbol availableParkingSymbol;
    private Symbol notAvailableParkingSymbol;
    private Symbol incidentSymbol;
    private Symbol routeSymbol;
    private NAFeaturesAsFeature facilities = new NAFeaturesAsFeature();
    private GraphicsLayer originalgraphicsLayer;

    // The current map extent, use to set the extent of the map after switching basemaps.
    private Polygon mCurrentMapExtent = null;

    String FEATURE_SERVICE_URL;
    public FeatureLayer featureLayer;
    public GeodatabaseFeatureServiceTable featureServiceTable;
    static String IS_SPOT_AVAILABLE;


    // create UI components
    static ProgressDialog dialog;
    private String mFeatureServiceURL;
    private ArcGISFeatureLayer mFeatureLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) findViewById(R.id.map);

        FEATURE_SERVICE_URL = getResources().getString(R.string.feature_service_url);
        // Set the Esri logo to be visible, and enable map to wrap around date line.
        mMapView.setEsriLogoVisible(true);
        mMapView.enableWrapAround(true);
        originalgraphicsLayer = new GraphicsLayer();
        mMapView.addLayer(originalgraphicsLayer);


        /*mFeatureServiceURL = this.getResources().getString(R.string.feature_service_url);
        // Add Feature layer to the MapView
        mFeatureLayer = new ArcGISFeatureLayer(mFeatureServiceURL, ArcGISFeatureLayer.MODE.ONDEMAND);
        mMapView.addLayer(mFeatureLayer);*/
        // create symbols
        availableParkingSymbol = createFacilitySymbol(1);
        notAvailableParkingSymbol = createFacilitySymbol(2);
        incidentSymbol = new SimpleMarkerSymbol(Color.BLACK, 18, SimpleMarkerSymbol.STYLE.CROSS);
        routeSymbol = new SimpleLineSymbol(Color.BLUE, 2.0f);
        //mapSR = mMapView.getSpatialReference();

        Point sanDiegoCenter = GeometryEngine.project(-117.1750, 32.727, mMapView.getSpatialReference());
        //mMapView.setExtent(mCurrentMapExtent);
        mMapView.setExtent(new Envelope(sanDiegoCenter,10000,10000));
        // create some facility graphics
        originalgraphicsLayer = new GraphicsLayer();
        mMapView.addLayer(originalgraphicsLayer);

        IS_SPOT_AVAILABLE = getResources().getString(R.string.parking_field_name);
        //setUpFeatureLayer();
        createFLfromURL();



        // Set a listener for map status changes; this will be called when switching basemaps.
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            private static final long serialVersionUID = 1L;

            public void onStatusChanged(Object source, STATUS status) {
                // Set the map extent once the map has been initialized, and the basemap is added
                // or changed; this will be indicated by the layer initialization of the basemap layer. As there is only
                // a single layer, there is no need to check the source object.

                mCurrentMapExtent = mMapView.getExtent();
                if (STATUS.LAYER_LOADED == status) {
                    availableParkingSymbol = createFacilitySymbol(1);
                    notAvailableParkingSymbol = createFacilitySymbol(2);
                    incidentSymbol = new SimpleMarkerSymbol(Color.BLACK, 18, SimpleMarkerSymbol.STYLE.CROSS);
                    routeSymbol = new SimpleLineSymbol(Color.BLUE, 2.0f);
                    Point sanDiegoCenter = GeometryEngine.project(-117.1750, 32.727, mMapView.getSpatialReference());
                    //mMapView.setExtent(mCurrentMapExtent);
                    mMapView.setExtent(new Envelope(sanDiegoCenter,10000,10000));
                    /*mLocDispMgr = mMapView.getLocationDisplayManager();
                    mLocDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    mLocDispMgr.setLocationListener(mLocationListener);*/

                }
            }
        });



        mMapView.setOnSingleTapListener(new OnSingleTapListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void onSingleTap(float x, float y) {
                final Point loc = mMapView.toMapPoint(x, y);
                // create a graphic for facility
                final SimpleMarkerSymbol sms = new SimpleMarkerSymbol(
                        Color.BLACK, 13, SimpleMarkerSymbol.STYLE.DIAMOND);
                // set start location
                Point point = mMapView.toMapPoint(x, y);
                // create graphic
                final Graphic graphic = new Graphic(point, sms);
                originalgraphicsLayer.addGraphic(graphic);
                // set parameters graphic and query url
                try {
                    getClosestFacility(graphic);
                } catch (Exception e) {
                    Log.e("Error",e.toString());
                }

            }
        });
    }

    private void setUpFeatureLayer() {
        try {
            featureServiceTable = new GeodatabaseFeatureServiceTable(FEATURE_SERVICE_URL, 0);
            Long l = Long.parseLong("28");
            Log.d(TAG, featureServiceTable.getFeature(l).getAttributes().size() + "");
            // initialize the GeodatabaseFeatureService and populate it with features from the service
            featureServiceTable.initialize(new CallbackListener<GeodatabaseFeatureServiceTable.Status>() {

                @Override
                public void onCallback(GeodatabaseFeatureServiceTable.Status status) {
                    Log.d(TAG, "callback");
                    // create a FeatureLayer from teh initialized GeodatabaseFeatureServiceTable
                    featureLayer = new FeatureLayer(featureServiceTable);
                    // emphasize the selected features by increasing the selection halo size and color
                    featureLayer.setSelectionColor(Color.GREEN);
                    featureLayer.setSelectionColorWidth(20);
                    // add feature layer to map
                    mMapView.addLayer(featureLayer);
                    Log.d(TAG, "after ading");
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    // set up spinners to contain values from the layer to query against


                }

                @Override
                public void onError(Throwable throwable) {
                    Toast.makeText(getApplicationContext(), "Error initializing FeatureServiceTable", Toast.LENGTH_SHORT);

                }
            });

        } catch (Exception e) {
            Log.e("Error", e.getLocalizedMessage());
        }

    }

    private Symbol createFacilitySymbol(int choice) {
        PictureMarkerSymbol symbol;



        try {

            if(choice == 1) {
                symbol =
                        new PictureMarkerSymbol(MainActivity.this, ContextCompat.getDrawable(MainActivity.this, R.drawable.circle_green));

            } else {
                symbol =
                        new PictureMarkerSymbol(MainActivity.this, ContextCompat.getDrawable(MainActivity.this, R.drawable.circle_red));

            }

        }
        catch (Exception e) {
            Log.e("Error",e.toString());
            return new SimpleMarkerSymbol(Color.RED, 15, SimpleMarkerSymbol.STYLE.DIAMOND);
        }
        return symbol;
    }

    private void getClosestFacility(final Graphic graphic) {
        final ClosestFacilityParameters[] parameters = new ClosestFacilityParameters[1];
        SpotFinder spotFinder = new SpotFinder(parameters[0],graphic);
        spotFinder.execute();

    }

    private class SpotFinder extends AsyncTask<Void, Void, ClosestFacilityResult> {

        private ClosestFacilityResult routeResult;
        ClosestFacilityParameters mParameters;
        Graphic mGraphic;
        SpatialReference mSpatialRef;

        public SpotFinder(ClosestFacilityParameters parameters, Graphic graphic) {
            mParameters = new ClosestFacilityParameters();
            mGraphic = graphic;
            mSpatialRef = mMapView.getSpatialReference();
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Displaying the Process Dialog
            dialog = ProgressDialog.show(MainActivity.this, "Finding nearest parking spot",
                    "Please wait ...");


        }

        @Override
        protected ClosestFacilityResult doInBackground(Void... params) {


            try {
                closestFacilityTask = new ClosestFacilityTask(
                        "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/ClosestFacility");

                mParameters = closestFacilityTask.getDefaultParameters();
            } catch (Exception e) {
                e.printStackTrace();
            }
            /**
             * cfp.setReturnFacilities(true);
             cfp.setOutSpatialReference(mMapView.getSpatialReference());
             // route direction to facility
             cfp.setTravelDirection(NATravelDirection.TO_FACILITY);
             // set incident to single tap location
             cfp.setIncidents(myLocationFeature);
             // set facilities to query url
             cfp.setFacilities(nafaf);
             cfp.setDefaultTargetFacilityCount(Integer.valueOf(1));
             */

            mParameters.setDefaultCutoff(Double.valueOf(30.0));

            NAFeaturesAsFeature incidents = new NAFeaturesAsFeature();
            incidents.addFeature(mGraphic);
            incidents.setSpatialReference(mSpatialRef);
            mParameters.setIncidents(incidents);
            //mParameters.setTravelDirection(NATravelDirection.TO_FACILITY);
            mParameters.setDefaultTargetFacilityCount(Integer.valueOf(1));

            facilities.setSpatialReference(mSpatialRef);
            mParameters.setFacilities(facilities);
            mParameters.setOutSpatialReference(mSpatialRef);
            ClosestFacilityResult result = null;

            try {
                result = closestFacilityTask.solve(mParameters);
            } catch (Exception e) {
                Log.e("Error",e.toString());
            }
            return result;
        }

        // The result of geocode task is passed as a parameter to map the
        // results

        @Override
        protected void onPostExecute(ClosestFacilityResult result) {
            super.onPostExecute(result);
            try {
                if(dialog.isShowing()) {
                    dialog.dismiss();
                }

                routeResult = get();

                if(routeResult.getRoutes().size() > 0) {
                    Route topRoute = routeResult.getRoutes().get(0);
                    originalgraphicsLayer.addGraphic(new Graphic(topRoute.getRouteGraphic().getGeometry(), routeSymbol));
                    createFLfromURL();
                }
            } catch (InterruptedException e) {
                Log.e("Error",e.toString());
            } catch (ExecutionException e) {
                Log.e("Error",e.toString());
            }

        }
    }

    private void createFLfromURL() {

        featureServiceTable = new GeodatabaseFeatureServiceTable(FEATURE_SERVICE_URL,0);
        // initialize the GeodatabaseFeatureService and populate it with features from the service
        featureServiceTable.initialize(new CallbackListener<GeodatabaseFeatureServiceTable.Status>() {

            @Override
            public void onCallback(GeodatabaseFeatureServiceTable.Status status) {
                Log.d(TAG,status.name());
                // create a FeatureLayer from teh initialized GeodatabaseFeatureServiceTable
                featureLayer = new FeatureLayer(featureServiceTable);

                // emphasize the selected features by increasing the selection halo size and color
                featureLayer.setSelectionColor(Color.GREEN);
                featureLayer.setSelectionColorWidth(20);
                // add feature layer to map
                mMapView.addLayer(featureLayer);
                // set up spinners to contain values from the layer to query against
                // Get the fields that will be used to query the layer.
                Field isAvailabe = featureServiceTable.getField(IS_SPOT_AVAILABLE);
                createONFields();

            }

            @Override
            public void onError(Throwable throwable) {
                Log.d(TAG,"error in callback");
                Log.d(TAG,throwable.getLocalizedMessage());
                Toast.makeText(getApplicationContext(),"Error initializing FeatureServiceTable",Toast.LENGTH_SHORT);


            }
        });
    }

    private void createONFields() {

        /*dialog = ProgressDialog.show(MainActivity.this, "Refreshing",
                "Please wait ...");*/
        if (featureLayer == null) {
            showToast("Feature layer is not set.");
            return;
        }
        featureLayer.clearSelection();
        //originalgraphicsLayer = new GraphicsLayer();
        // Build query predicates to construct a query where clause from selected values.
        String whereClause = "1=1";

        // Create query parameters, based on the constructed where clause.
        QueryParameters queryParams = new QueryParameters();
        queryParams.setWhere(whereClause);


        facilities = new NAFeaturesAsFeature();

        // Execute the query and create a callback for dealing with the results of the query.
        featureServiceTable.queryFeatures(queryParams, new CallbackListener<FeatureResult>() {

            @Override
            public void onError(Throwable ex) {
                // Highlight errors to the user.
                showToast("Error querying FeatureServiceTable");
            }

            @Override
            public void onCallback(FeatureResult objs) {

                // If there are no query results, inform user.
                if (objs.featureCount() < 1) {
                    showToast("No results");
                    return;
                }

                ArrayList<Feature> trueFeatures = new ArrayList<Feature>();

                // Report number of results to user.
                showToast("Found " + objs.featureCount() + " features.");

                // Iterate the results and select each feature.
                for (Object objFeature : objs) {
                    Feature feature = (Feature) objFeature;
                    String val = String.valueOf(feature.getAttributeValue("isAvailable"));
                    Log.d("val-",val);
                    if(val.equalsIgnoreCase("true")) {
                        featureLayer.selectFeature(feature.getId());
                        Log.d("geometry-",feature.getGeometry().getDimension()+"");
                        trueFeatures.add(feature);
                    }

                }
                Graphic[] facilityGraphics = new Graphic[trueFeatures.size()];
                int i = 0;
                for(Feature f : trueFeatures) {
                    facilityGraphics[i] = new Graphic(f.getGeometry(),availableParkingSymbol);
                    i++;
                }
                Log.d("count-",i+"");

                // add them to the graphics layer for display and to our 'facilities' collection for the task
                originalgraphicsLayer.addGraphics(facilityGraphics);
                facilities.addFeatures(facilityGraphics);
                if(dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });

    }


    public void showToast(final String message) {
        // Show toast message on the main thread only; this function can be
        // called from query callbacks that run on background threads.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }



}
