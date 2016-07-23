package com.esri.arcgisruntime.closestfeature;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.CodedValueDomain;
import com.esri.core.map.Field;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.tasks.na.ClosestFacilityParameters;
import com.esri.core.tasks.na.ClosestFacilityResult;
import com.esri.core.tasks.na.ClosestFacilityTask;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.NATravelDirection;
import com.esri.core.tasks.na.Route;

import java.util.concurrent.ExecutionException;

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
    private GraphicsLayer graphicsLayer;

    // The current map extent, use to set the extent of the map after switching basemaps.
    private Polygon mCurrentMapExtent = null;
    //SpatialReference mapSR;

    String FEATURE_SERVICE_URL;
    public FeatureLayer featureLayer;
    public GeodatabaseFeatureServiceTable featureServiceTable;




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
        graphicsLayer = new GraphicsLayer();
        mMapView.addLayer(graphicsLayer);

        mFeatureServiceURL = this.getResources().getString(R.string.feature_service_url);
        // Add Feature layer to the MapView
        mFeatureLayer = new ArcGISFeatureLayer(mFeatureServiceURL, ArcGISFeatureLayer.MODE.ONDEMAND);
        mMapView.addLayer(mFeatureLayer);
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
        graphicsLayer = new GraphicsLayer();
        mMapView.addLayer(graphicsLayer);


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
                    Graphic[] facilityGraphics = {
                            new Graphic(GeometryEngine.project(-117.138368, 32.708657, mMapView.getSpatialReference()), availableParkingSymbol),
                            new Graphic(GeometryEngine.project(-117.163369, 32.724766, mMapView.getSpatialReference()), notAvailableParkingSymbol),
                            new Graphic(GeometryEngine.project(-117.159477, 32.735328, mMapView.getSpatialReference()), availableParkingSymbol),
                            new Graphic(GeometryEngine.project(-117.159918, 32.751387, mMapView.getSpatialReference()), notAvailableParkingSymbol),
                            new Graphic(GeometryEngine.project(-117.144708, 32.755919, mMapView.getSpatialReference()), availableParkingSymbol),
                            new Graphic(GeometryEngine.project(-117.201550, 32.752967, mMapView.getSpatialReference()), notAvailableParkingSymbol),
                            new Graphic(GeometryEngine.project(-117.221417, 32.748656, mMapView.getSpatialReference()), availableParkingSymbol)
                    };
                    // add them to the graphics layer for display and to our 'facilities' collection for the task
                    graphicsLayer.addGraphics(facilityGraphics);
                    facilities.addFeatures(facilityGraphics);


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
                graphicsLayer.addGraphic(graphic);
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
                    graphicsLayer.addGraphic(new Graphic(topRoute.getRouteGraphic().getGeometry(), routeSymbol));
                }
            } catch (InterruptedException e) {
                Log.e("Error",e.toString());
            } catch (ExecutionException e) {
                Log.e("Error",e.toString());
            }

        }
    }
}
