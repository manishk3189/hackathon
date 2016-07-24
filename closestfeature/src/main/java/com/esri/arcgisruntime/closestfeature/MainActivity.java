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
    //SpatialReference mapSR;

    String FEATURE_SERVICE_URL;
    public FeatureLayer featureLayer;
    public GeodatabaseFeatureServiceTable featureServiceTable;

    static String IS_SPOT_AVAILABLE;
    static Point destinationPoint;
    Point mLocationLayerPoint;
    String mLocationLayerPointString;
    ArrayList<Point> points = new ArrayList<Point>();
    static boolean draw_activated = false;
    MultiPath polyline;
    MultiPath polygon;
    static GraphicsLayer redZone = new GraphicsLayer();
    LocationDisplayManager mLocDispMgr = null;
    GraphicsLayer currentLocation = new GraphicsLayer();
    boolean navigation = false;
    Point currentMapPt = null;
    private LocationManager locationManager;
    private String provider;
    List<String> providerList = null;
    int notifTimes = 0;
    GraphicsLayer graphicsLayer;

    private Locator mLocator;
    private SearchView mSearchView;
    private MenuItem searchMenuItem;
    private MatrixCursor mSuggestionCursor;
    MapViewHelper mMapViewHelper = null;


    private LocatorSuggestionParameters suggestParams;
    private LocatorFindParameters findParams;
    private static final String COLUMN_NAME_ADDRESS = "address";
    private static final String COLUMN_NAME_X = "x";
    private static final String COLUMN_NAME_Y = "y";
    private static final String LOCATION_TITLE = "Location";
    private static final String FIND_PLACE = "Find";
    private static final String SUGGEST_PLACE = "Suggest";
    private static final String SUGGESTION_ADDRESS_DELIMNATOR = ", ";
    private static ProgressDialog mProgressDialog;
    MyTouchListener myListener = null;
    FloatingActionButton navigationFAB;

    //driving directions
    final Handler mHandler = new Handler();
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateUI();
        }
    };

    // Progress dialog to show when route is being calculated
    //ProgressDialog dialog;
    // Spatial references used for projecting points
    final SpatialReference wm = SpatialReference.create(102100);
    final SpatialReference egs = SpatialReference.create(4326);
    // Index of the currently selected route segment (-1 = no selection)
    int selectedSegmentID = -1;
    GraphicsLayer directionrouteLayer, hiddenSegmentsLayer;
    Route curRoute = null;
    String routeSummary = null;
    public static Point mLocation = null;
    // Global results variable for calculating route on separate thread
    RouteTask mRouteTask = null;
    RouteResult mResults = null;
    // Variable to hold server exception to show to user
    Exception mException = null;
    SimpleLineSymbol segmentHider = new SimpleLineSymbol(Color.WHITE, 5);
    // Symbol used to highlight route segments
    SimpleLineSymbol segmentShower = new SimpleLineSymbol(Color.RED, 5);
    private static boolean suggestClickFlag = false;
    private static boolean searchClickFlag = false;

    private SpatialReference mapSpatialReference;
    private static ArrayList<LocatorSuggestionResult> suggestionsList;
    static UserCredentials credentials;





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
        mMapViewHelper = new MapViewHelper(mMapView);

        mProgressDialog = new ProgressDialog(this) {
            @Override
            public void onBackPressed() {
                // Back key pressed - just dismiss the dialog
                mProgressDialog.dismiss();
            }
        };
        credentials = getCred();

        // Add the hidden segments layer (for highlighting route segments)
        hiddenSegmentsLayer = new GraphicsLayer();
        mMapView.addLayer(hiddenSegmentsLayer);

        // Make the segmentHider symbol "invisible"
        segmentHider.setAlpha(1);
        MyOnSingleTapListener listener = new MyOnSingleTapListener(this);
        mMapView.setOnSingleTapListener(listener);

        mLocator = Locator.createOnlineLocator();
        // Initialize the RouteTask
        try {
            mRouteTask = RouteTask
                    .createOnlineRouteTask(
                            "http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Network/USA/NAServer/Route",
                            null);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        myListener = new MyTouchListener(MainActivity.this,
                mMapView);
        mMapView.setOnTouchListener(myListener);

        graphicsLayer = new GraphicsLayer();
        redZone = new GraphicsLayer();
        mMapView.addLayer(graphicsLayer);
        mMapView.addLayer(redZone);
        mSearchView = (SearchView)findViewById(R.id.searchView);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(!suggestClickFlag && !searchClickFlag) {
                    searchClickFlag = true;
                    onSearchButtonClicked(query);
                    mSearchView.clearFocus();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(mLocator == null)
                    return false;
                getSuggestions(newText);
                return true;
            }
        });

        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                // Obtain the content of the selected suggesting place via cursor
                MatrixCursor cursor = (MatrixCursor) mSearchView.getSuggestionsAdapter().getItem(position);
                int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
                int indexColumnX = cursor.getColumnIndex(COLUMN_NAME_X);
                int indexColumnY = cursor.getColumnIndex(COLUMN_NAME_Y);
                String address = cursor.getString(indexColumnSuggestion);
                suggestClickFlag = true;


                new FindLocationTask(address).execute(address);

                cursor.close();

                return true;
            }
        });



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
                    mLocDispMgr = mMapView.getLocationDisplayManager();
                    mLocDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    mLocDispMgr.setLocationListener(mLocationListener);

                }
            }
        });



/*        mMapView.setOnSingleTapListener(new OnSingleTapListener() {
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
        });*/



        navigationFAB = (FloatingActionButton) findViewById(R.id.restartNavImageButton) ;
        navigationFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocDispMgr != null) {
                    // Re-enable the navigation mode.
                    notifTimes = 0;

                    Toast.makeText(MainActivity.this,"I gotcha", Toast.LENGTH_SHORT).show();
                    mLocDispMgr.start();
                    mLocDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    Graphic a = redZone.getGraphic(0);
                    Point point = mLocDispMgr.getPoint();
                    mMapView.zoomTo(point,20);
                    Polygon buffer = GeometryEngine.buffer(point,SpatialReference.create(4326),0.0033,Unit.create(AngularUnit.Code.DEGREE));
                    if (redZone.getNumberOfGraphics () != 0)
                    {
                        if (GeometryEngine.intersects(buffer,redZone.getGraphic(redZone.getGraphicIDs()[0]).getGeometry(),SpatialReference.create(4326)))
                            if (notifTimes == 0) {

                                notifTimes++;
                                sendNotification("You are in Red Zone. Be Safe!!");
                            }
                    }

                }
            }
        });
    }

    public void onSearchButtonClicked(String address) {
        hideKeyboard();
        mMapViewHelper.removeAllGraphics();
        executeLocatorTaskSearch(address);
    }

    private void executeLocatorTaskSearch(String address) {

        //Create Locator parameters from single line address string
        locatorParams(FIND_PLACE, address);

        //Execute async task to find the address
        LocatorAsyncTaskSearch locatorTask = new LocatorAsyncTaskSearch();
        locatorTask.execute(findParams);

    }

    private class LocatorAsyncTaskSearch extends
            AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {

        private Exception mException;

        public LocatorAsyncTaskSearch() {
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.setMessage(getString(R.string.address_search));
            mProgressDialog.show();
        }

        @Override
        protected List<LocatorGeocodeResult> doInBackground(
                LocatorFindParameters... params) {
            // Perform routing request on background thread
            mException = null;
            List<LocatorGeocodeResult> results = null;

            // Create locator using default online geocoding service and tell it
            // to
            // find the given address
            Locator locator = Locator.createOnlineLocator();
            try {
                results = locator.find(params[0]);
            } catch (Exception e) {
                mException = e;
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<LocatorGeocodeResult> result) {
            // Display results on UI thread
            mProgressDialog.dismiss();
            if (mException != null) {
                Log.w(TAG, "LocatorSyncTask failed with:");
                mException.printStackTrace();
                Toast.makeText(MainActivity.this,
                        getString(R.string.addressSearchFailed),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (result.size() == 0) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.noResultsFound), Toast.LENGTH_LONG)
                        .show();
            } else {
                // Use first result in the list
                LocatorGeocodeResult geocodeResult = result.get(0);

                // get return geometry from geocode result
                Point resultPoint = geocodeResult.getLocation();

                double x = resultPoint.getX();
                double y = resultPoint.getY();

                // Get the address
                String address = geocodeResult.getAddress();

                // Display the result on the map
                QueryDirections(currentMapPt,resultPoint);
                displaySearchResult(x,y,address);
                hideKeyboard();

            }
        }

    }

    private void initSuggestionCursor() {
        String[] cols = new String[]{BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
        mSuggestionCursor = new MatrixCursor(cols);
    }

    // Set the suggestion cursor to an Adapter then set it to the search view
    private void applySuggestionCursor() {
        String[] cols = new String[]{COLUMN_NAME_ADDRESS};
        int[] to = new int[]{R.id.suggestion_item_address};

        SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(mMapView.getContext(), R.layout.suggestion, mSuggestionCursor, cols, to, 0);
        mSearchView.setSuggestionsAdapter(mSuggestionAdapter);
        mSuggestionAdapter.notifyDataSetChanged();
    }

    // Find the address
    private class FindPlaceTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
        private final Locator mLocator;

        public FindPlaceTask(Locator locator) {
            mLocator = locator;
        }

        @Override
        protected List<LocatorGeocodeResult> doInBackground(LocatorFindParameters... params) {

            // Execute the task
            List<LocatorGeocodeResult> results = null;
            try {
                results = mLocator.find(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return results;

        }

        @Override
        protected void onPreExecute() {
            // Display progress dialog on UI thread
            mProgressDialog.setMessage(getString(R.string.address_search));
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(List<LocatorGeocodeResult> results) {
            // Dismiss progress dialog
            mProgressDialog.dismiss();
            if ((results == null) || (results.size() == 0))
                return;

            // Add the first result to the map and zoom to it
            LocatorGeocodeResult result = results.get(0);
            destinationPoint = result.getLocation();
            double x = result.getLocation().getX();

            double y = result.getLocation().getY();
            String address = result.getAddress();
            Log.d(TAG + "currentPt",currentMapPt.toString());
            Log.d(TAG + "resultPt", destinationPoint.toString());

            QueryDirections(currentMapPt, destinationPoint);

            displaySearchResult(x, y, address);
            hideKeyboard();
        }
    }



    /**
     * Display the search location on the map
     * @param x Longitude of the place
     * @param y Latitude of the place
     * @param address The address of the location
     */
    protected void displaySearchResult(double x, double y, String address) {
        // Add a marker at the found place. When tapping on the marker, a Callout with the address
        // will be displayed
        mMapViewHelper.addMarkerGraphic(y, x, LOCATION_TITLE, address, android.R.drawable.ic_menu_myplaces, null, false, 1);
        mMapView.centerAndZoom(y, x, 14);
        mSearchView.setQuery(address, true);
        searchClickFlag = false;
        suggestClickFlag = false;


    }

    protected void getSuggestions(String suggestText) {
        final CallbackListener<List<LocatorSuggestionResult>> suggestCallback = new CallbackListener<List<LocatorSuggestionResult>>() {
            @Override
            public void onCallback(List<LocatorSuggestionResult> locatorSuggestionResults) {
                final List<LocatorSuggestionResult> locSuggestionResults = locatorSuggestionResults;
                if (locatorSuggestionResults == null)
                    return;
                suggestionsList = new ArrayList<>();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int key = 0;
                        if(locSuggestionResults.size() > 0) {
                            // Add suggestion list to a cursor
                            initSuggestionCursor();
                            for (final LocatorSuggestionResult result : locSuggestionResults) {
                                suggestionsList.add(result);

                                // Add the suggestion results to the cursor
                                mSuggestionCursor.addRow(new Object[]{key++, result.getText(), "0", "0"});
                            }

                            applySuggestionCursor();
                        }
                    }

                });

            }


            @Override
            public void onError(Throwable throwable) {
                //Log the error
                Log.e(MainActivity.class.getSimpleName(), "No Results found!!");
                Log.e(MainActivity.class.getSimpleName(), throwable.getMessage());
            }
        };

        try {
            // Initialize the LocatorSuggestion parameters
            locatorParams(SUGGEST_PLACE,suggestText);

            mLocator.suggest(suggestParams, suggestCallback);

        } catch (Exception e) {
            Log.e(MainActivity.class.getSimpleName(),"No Results found");
            Log.e(MainActivity.class.getSimpleName(),e.getMessage());
        }
    }

    /**
     * Initialize the LocatorSuggestionParameters or LocatorFindParameters
     *
     * @param query The string for which the locator parameters are to be initialized
     */
    protected void locatorParams(String TYPE, String query) {

        if(TYPE.contentEquals(SUGGEST_PLACE)) {
            suggestParams = new LocatorSuggestionParameters(query);
            // Use the centre of the current map extent as the suggest location point
            suggestParams.setLocation(mMapView.getCenter(), mMapView.getSpatialReference());
            // Set the radial search distance in meters
            suggestParams.setDistance(500.0);
        }
        else if(TYPE.contentEquals(FIND_PLACE)) {
            findParams = new LocatorFindParameters(query);
            //Use the center of the current map extent as the find point
            findParams.setLocation(mMapView.getCenter(), mMapView.getSpatialReference());
            // Set the radial search distance in meters
            findParams.setDistance(500.0);
        }


    }

    protected void hideKeyboard() {

        // Hide soft keyboard
        mSearchView.clearFocus();
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
    }

    private void QueryDirections(final Point mLocation, final Point p) {

        // Show that the route is calculating
        dialog = ProgressDialog.show(MainActivity.this, "Routing Sample",
                "Calculating route...", true);
        // Spawn the request off in a new thread to keep UI responsive
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    // Start building up routing parameters
// Start building up routing parameters
                    RouteParameters rp = mRouteTask
                            .retrieveDefaultRouteTaskParameters();
                    NAFeaturesAsFeature rfaf = new NAFeaturesAsFeature();
                    // Convert point to EGS (decimal degrees)
                    // Create the stop points (start at our location, go
                    // to pressed location)
                    StopGraphic point1 = new StopGraphic(mLocation);
                    StopGraphic point2 = new StopGraphic(p);
                    rfaf.setFeatures(new Graphic[] { point1, point2 });
                    rfaf.setCompressedRequest(true);
                    rp.setStops(rfaf);
                    // Set the routing service output SR to our map
                    // service's SR
                    rp.setOutSpatialReference(wm);

                    // Solve the route and use the results to update UI
                    // when received
                    mResults = mRouteTask.solve(rp);
                    mHandler.post(mUpdateResults);
                } catch (Exception e) {
                    mException = e;
                    mHandler.post(mUpdateResults);
                }
            }
        };
        // Start the operation
        t.start();

    }

    void updateUI() {
        dialog.dismiss();

        if (mResults == null) {
            Toast.makeText(MainActivity.this, mException.toString(),
                    Toast.LENGTH_LONG).show();
            Log.d(TAG,mException.toString());
            return;
        }


        Log.d(TAG + "RouteResults", mResults.getRoutes().size() + "");
        curRoute = mResults.getRoutes().get(0);
        // Symbols for the route and the destination (blue line, checker flag)
        SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.BLUE, 3);
        PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(
                mMapView.getContext(), getResources().getDrawable(
                R.drawable.ic_action_place));

        // Add all the route segments with their relevant information to the
        // hiddenSegmentsLayer, and add the direction information to the list
        // of directions
        for (RouteDirection rd : curRoute.getRoutingDirections()) {
            Log.d(TAG,rd.getText());
            HashMap<String, Object> attribs = new HashMap<String, Object>();
            attribs.put("text", rd.getText());
            attribs.put("time", Double.valueOf(rd.getMinutes()));
            attribs.put("length", Double.valueOf(rd.getLength()));
            Graphic routeGraphic = new Graphic(rd.getGeometry(), segmentHider,
                    attribs);
            hiddenSegmentsLayer.addGraphic(routeGraphic);
        }
        // Reset the selected segment
        selectedSegmentID = -1;

        // Add the full route graphics, start and destination graphic to the
        // routeLayer
        Graphic routeGraphic = new Graphic(curRoute.getRouteGraphic()
                .getGeometry(), routeSymbol);
        Graphic endGraphic = new Graphic(
                ((Polyline) routeGraphic.getGeometry()).getPoint(((Polyline) routeGraphic
                        .getGeometry()).getPointCount() - 1), destinationSymbol);
        routeLayer.addGraphics(new Graphic[]{routeGraphic, endGraphic});
        // Get the full route summary and set it as our current label
        routeSummary = String.format("%s%n%.1f minutes (%.1f miles)",
                curRoute.getRouteName(), curRoute.getTotalMinutes(),
                curRoute.getTotalMiles());

    }



    //Fetch the Location from the Map and display it
    private class FindLocationTask extends AsyncTask<String,Void,Point> {
        private Point resultPoint = null;
        private String resultAddress;
        private Point temp = null;

        public FindLocationTask(String address) {
            resultAddress = address;
        }

        @Override
        protected Point doInBackground(String... params) {

            // get the Location for the suggestion from the map
            for(LocatorSuggestionResult result: suggestionsList) {
                if (resultAddress.matches(result.getText())) {
                    try {
                        temp = ((mLocator.find(result, 2, null, mapSpatialReference)).get(0)).getLocation();
                    } catch (Exception e) {
                        Log.e(TAG,"Exception in FIND");
                        Log.e(TAG,e.getMessage());
                    }
                }
            }

            resultPoint = (Point) GeometryEngine.project(temp, mapSpatialReference, SpatialReference.create(4326));

            return resultPoint;
        }

        @Override
        protected void onPreExecute() {
            // Display progress dialog on UI thread
            mProgressDialog.setMessage(getString(R.string.address_search));
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Point resultPoint) {
            // Dismiss progress dialog
            mProgressDialog.dismiss();
            if (resultPoint == null)
                return;

            QueryDirections(currentMapPt,resultPoint);
            // Display the result
            displaySearchResult(resultPoint.getX(), resultPoint.getY(), resultAddress);
            hideKeyboard();
        }

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
                    String val = String.valueOf(feature.getAttributeValue("IsAvailable"));
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

    final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentLocation.removeAll();
            Point currentPt = new Point(location.getLongitude(), location.getLatitude());
            currentMapPt = (Point) GeometryEngine.project(currentPt,
                    SpatialReference.create(4326), mMapView.getSpatialReference());
            Polygon buffer = GeometryEngine.buffer(currentPt,SpatialReference.create(4326),0.0033, Unit.create(AngularUnit.Code.DEGREE));
            SimpleFillSymbol simpleFillSymbol = new SimpleFillSymbol(
                    Color.RED);
            simpleFillSymbol.setAlpha(50);
            Graphic polygon = new Graphic(buffer, (simpleFillSymbol));
            currentLocation.addGraphic(polygon);
            mMapView.addLayer(currentLocation);
            if (redZone.getNumberOfGraphics () != 0)
            {
                if (GeometryEngine.intersects(buffer,redZone.getGraphic(redZone.getGraphicIDs()[0]).getGeometry(),SpatialReference.create(4326)))
                    if(notifTimes < 2)
                    {
                        sendNotification("You are in Red Zone. Be Safe!!");
                        notifTimes++;
                    }
            }
            mLocDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
        }


        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this,"Turn the GPS on to get accurate location", Toast.LENGTH_SHORT).show();}
    };

    public void sendNotification(String title) {
        try {
            NotificationManager notificationManager = null;
            Context ctx = getApplicationContext();
            if (ctx != null)
            {
                notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
            }
            else {
                notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            }

            // Build a notification.
            Notification.Builder notificationBuilder = new Notification.Builder(this);
            notificationBuilder.setContentTitle(title);
            //notificationBuilder.setContentText(LocalGeofence.getSubtitle());
            notificationBuilder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilder.setSound(alarmSound);
            notificationBuilder.setSmallIcon(R.drawable.abc_btn_rating_star_on_mtrl_alpha);
            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            notificationBuilder.setLargeIcon(largeIcon);
            if (Build.VERSION.SDK_INT >= 21) {
                // API 21 and over -
                notificationBuilder.setColor(getResources().getColor(R.color.material_blue_grey_800));
            }

            // Notification API was introduced at v11, but there were some additional changes from v16.
            // We only need to deal with changes since v14.
            Random r = new Random();
            int id = r.nextInt();
            if (Build.VERSION.SDK_INT < 16) {
                notificationManager.notify(id, notificationBuilder.getNotification());
            } else {
                notificationManager.notify(id, notificationBuilder.build());
            }
        }
        catch (Exception ex) {
            Log.i("", ex.getMessage());
        }
    }

    class MyTouchListener extends MapOnTouchListener {
        Point startPoint = null;
        public MyTouchListener(Context context, MapView view) {
            super(context, view);
        }

        @Override
        public boolean onSingleTap(MotionEvent e) {

            if (draw_activated) {
                redZone.removeAll();
                startPoint = mMapView.toMapPoint(new Point(e.getX(), e.getY()));
                points.add(startPoint);
                if (points.size() == 1)
                {
                    Graphic graphic_point = new Graphic(mMapView.toMapPoint(new Point(e.getX(), e
                            .getY())),new SimpleMarkerSymbol(Color.RED,1, SimpleMarkerSymbol.STYLE.CIRCLE));
                    redZone.addGraphic(graphic_point);

                }
                if (points.size() == 2)
                {
                    redZone.removeAll();
                    polyline = new Polyline();
                    polyline.startPath(points.get(0));
                    for (int i = 1; i < points.size(); i++) {
                        polyline.lineTo(points.get(i));

                    }
                    Graphic graphic_line = new Graphic(polyline, new SimpleLineSymbol(Color.BLACK, 1));
                    redZone.addGraphic(graphic_line);
                }

                if (points.size() > 2) {
                    redZone.removeAll();
                    polygon = new Polygon();
                    polygon.startPath(points.get(0));
                    for (int i = 1; i < points.size(); i++) {
                        polygon.lineTo(points.get(i));
                    }
                    SimpleFillSymbol simpleFillSymbol = new SimpleFillSymbol(
                            Color.RED);
                    simpleFillSymbol.setAlpha(50);
                    Graphic graphic_polygon = new Graphic(polygon, (simpleFillSymbol));
                    redZone.addGraphic(graphic_polygon);
                    Graphic a = redZone.getGraphic(redZone.getGraphicIDs()[0]);
                    int b =9;
                }return true;
            }
            return false;

        }



    }

    public void clear() {
        if (graphicsLayer != null) {
            graphicsLayer.removeAll();
        }

        if (redZone != null) {
            redZone.removeAll();
        }

        points.clear();
    }

    public void save()
    {draw_activated=false;}

    private class LocatorAsyncTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
        private Exception mException;

        @Override
        protected List<LocatorGeocodeResult> doInBackground(LocatorFindParameters... params) {
            mException = null;
            List<LocatorGeocodeResult> results = null;
            Locator locator = Locator.createOnlineLocator();
            try {
                results = locator.find(params[0]);
            } catch (Exception e) {
                mException = e;
            }
            return results;
        }

        protected void onPostExecute(List<LocatorGeocodeResult> result) {
            if (mException != null) {
                Log.w("PlaceSearch", "LocatorSyncTask failed with:");
                mException.printStackTrace();
                Toast.makeText(MainActivity.this, "Address Search Failed", Toast.LENGTH_SHORT).show();
                return;
            }

            if (result.size() == 0) {
                Toast.makeText(MainActivity.this, "No Results Found", Toast.LENGTH_SHORT).show();
            } else {
                // Use first result in the list
                LocatorGeocodeResult geocodeResult = result.get(0);

                // get return geometry from geocode result
                Point resultPoint = geocodeResult.getLocation();
                // create marker symbol to represent location
                SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(Color.RED, 16, SimpleMarkerSymbol.STYLE.CROSS);
                // create graphic object for resulting location
                Graphic resultLocGraphic = new Graphic(resultPoint, resultSymbol);
                // add graphic to location layer
                graphicsLayer.addGraphic(resultLocGraphic);

                // create text symbol for return address
                String address = geocodeResult.getAddress();
                TextSymbol resultAddress = new TextSymbol(20, address, Color.BLACK);
                // create offset for text
                resultAddress.setOffsetX(-4 * address.length());
                resultAddress.setOffsetY(10);
                // create a graphic object for address text
                Graphic resultText = new Graphic(resultPoint, resultAddress);
                // add address text graphic to location graphics layer
                // graphicsLayer.addGraphic(resultText);

                mLocationLayerPoint = resultPoint;

                // Zoom map to geocode result location
                //mMapView.zoomToResolution(geocodeResult.getLocation(), 2);
            }
        }

    }


    private static UserCredentials getCred() {


        credentials = new UserCredentials();

        credentials.setUserAccount("mani3189","Leonidas123#");


        return credentials;
    }

    private class MyOnSingleTapListener implements OnSingleTapListener {

        // Here, we use a single tap to popup the attributes for a report...
        Context _ctx;
        private static final long serialVersionUID = 1L;

        public MyOnSingleTapListener(Context ctx) {
            _ctx = ctx;
        }

        @Override
        public void onSingleTap(float x, float y) {
            Callout mapCallout = mMapView.getCallout();
            mapCallout.hide();
            mapCallout.getStyle().setTitleTextColor(Color.GREEN);
            Point pnt = mMapView.toMapPoint(x, y);

            int[] grs = graphicsLayer.getGraphicIDs(x, y, 20);
            Log.d("Test", "Graphics number is " + grs.length);

            if (grs.length > 0) {
                Graphic g = graphicsLayer.getGraphic(grs[0]);
                Map<String, Object> atts = g.getAttributes();
                String text = "";
                for (int i = 0; i < atts.size(); i++) {
                    text = text + atts.keySet().toArray()[i] + ": " + atts.values().toArray()[i] + "\n";
                }

                TextView tv = new TextView(_ctx);
                tv.setText(text);
                tv.setTextColor(Color.GREEN);

                // Here, we populate the Callout with the attribute information
                // from the report.
                mapCallout.setOffset(0, -3);
                mapCallout.setCoordinates(pnt);
                mapCallout.setMaxHeight(350);
                mapCallout.setMaxWidth(900);
                mapCallout.setStyle(R.xml.mycalloutprefs);
                mapCallout.setContent(tv);

                mapCallout.show();
            }

        }
    }



}
