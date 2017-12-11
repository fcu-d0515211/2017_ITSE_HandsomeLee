package com.handsomelee.gotroute.Controller;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.google.gson.Gson;
import com.handsomelee.gotroute.MainActivity;
import com.handsomelee.gotroute.Model.*;
import com.handsomelee.gotroute.R;
import com.handsomelee.gotroute.Services.DatabaseConnect;
import com.handsomelee.gotroute.Services.GoogleMapSystem;
import com.handsomelee.gotroute.Services.RequestHandler;
import com.handsomelee.gotroute.Services.RouteDetailAdapter;

import java.text.DateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class MapsActivity extends GoogleMapSystem implements PlaceSelectionListener
        , GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
  
  private EditText editText;
  private static PlaceAutocompleteFragment autocompleteFragment;
  public static PlaceAutocompleteFragment origin;
  public static PlaceAutocompleteFragment destination;
  private Button getLocationBtn;
  private Button navigationBtn;
  private static String destinationString = "";
  private static RequestHandler.ProgressType progressType = RequestHandler.ProgressType.Free;
  private static final Handler refreshHandler = new Handler();
  public static RadioGroup navigationRadioGroup;
  public static ListView listView;
  public static LinearLayout listLinearLayoutView;
  public static RouteDetailAdapter mAdapter;
  public static RouteInfo routeInfo;
  public static Button listViewBtn;
  public static Marker[] carParkingMarkers;
  public static Marker[] reportMarkers;
  private static long refreshSecond;
  int PLACE_PICKER_REQUEST = 1;
  
  GestureDetector gestureDetector;
  
  public MapsActivity(int mapViewId, int layoutActivityId, int googleMapType) {
    super(mapViewId, layoutActivityId, googleMapType);
    
  }
  
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    autoRefresh();
  }
  
  private void autoRefresh() {
    Log.v("Report","refresh");
    refreshHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        processReport();
        autoRefresh();
      }
    }, refreshSecond);
  }
  
  @Override
  public void addOn() {
    super.addOn();
    listViewBtn = rootView.findViewById(R.id.ListViewBtn);
    listLinearLayoutView = rootView.findViewById(R.id.ListViewParent);
    final Button reportBtn = (Button) rootView.findViewById(R.id.ReportBtn);
    navigationBtn = (Button) rootView.findViewById(R.id.navigationBtn);
    autocompleteFragment = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentById(R.id.placeSearch);
    origin = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentById(R.id.Origin);
    destination = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentById(R.id.Destination);
    navigationRadioGroup = (RadioGroup) rootView.findViewById(R.id.navigationRadioGroup);
    getLocationBtn = (Button) rootView.findViewById(R.id.getMyLocation);
    ProgressBar progressBar = new ProgressBar(getActivity());
    progressBar.setLayoutParams(new AbsListView.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    progressBar.setIndeterminate(true);
    listView = (ListView) rootView.findViewById(R.id.ListView);
    listView.setEmptyView(progressBar);
    autocompleteFragment.getView().setBackgroundColor(Color.argb(255 / 100 * 95, 255, 255, 255));
    configureRadioGroup();
    configureOriginAndDestination();
    afterScreenLoaded();
    autocompleteFragment.setOnPlaceSelectedListener(this);
    destination.setOnPlaceSelectedListener(this);
    processReport();
    autocompleteFragment.getView().setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        view.setVisibility(View.GONE);
      }
    });
    autocompleteFragment.getView().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        autocompleteFragment.setText("");
        removeMarker();
        hideNavigationBtn();
      }
    });
    
    getLocationBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(MainActivity.getLocationSystem().getLatLng())
                        .zoom(15)
                        .build()
        ));
      }
    });
    
    reportBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        reportProcessBtn();
      }
    });
  }
  
  @Override
  public void onMapReady(GoogleMap googleMap) {
    super.onMapReady(googleMap);
    mMap.setOnMarkerClickListener(this);
    mMap.setOnMapClickListener(this);
  }
  
  private void afterScreenLoaded() {
    rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        configureListView();
        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
      }
    });
  }
  
  public void showNavigationBtn() {
    navigationBtn.setClickable(true);
    navigationBtn.setVisibility(View.VISIBLE);
    navigationBtn.animate().setDuration(1000).y(MainActivity.getHeight() - navigationBtn.getHeight() - 30).alpha(1).start();
  }
  
  public void hideNavigationBtn() {
    navigationBtn.setClickable(false);
    navigationBtn.animate().setDuration(1000).y(MainActivity.getHeight()).alpha(0).withEndAction(new Runnable() {
      @Override
      public void run() {
        navigationBtn.setVisibility(View.INVISIBLE);
      }
    }).start();
  }
  
  public void configureListView() {
//    listView.setScrollY(ListView.SCROLL_AXIS_VERTICAL);
//    listView.animate().y(MainActivity.getHeight() + 100).start();
    listLinearLayoutView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MainActivity.calculateHeight(100.0 / 50)));
    listLinearLayoutView.animate().y(MainActivity.getHeight() + 100).start();
  }
  
  public static void closeListView() {
    listViewBtn.setText("^");
    listLinearLayoutView.animate().setDuration(600).y(MainActivity.getHeight()).start();
  }
  
  public static void showListView() {
    listViewBtn.setText("v");
//    listView.animate().setDuration(600).y(0).start();
    listLinearLayoutView.animate().setDuration(600).y(MainActivity.getHeight() - listLinearLayoutView.getHeight()).start();
    
  }
  
  public static void hideListView() {
    listViewBtn.setText("^");
//    listView.animate().setDuration(600).y(MainActivity.getHeight() + 100).start();
    listLinearLayoutView.animate().setDuration(600).y(MainActivity.getHeight() - 40).start();
    
  }
  
  public void configureRadioGroup() {
    navigationRadioGroup.setY(-570);
    navigationRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch (i) {
          case R.id.CyclingRadio:
            MainActivity.directionType = RequestHandler.DirectionType.Cycling;
            MainActivity.requestNavigation(new Button(getContext()));
            break;
          case R.id.DrivingRadio:
            MainActivity.directionType = RequestHandler.DirectionType.Driving;
            MainActivity.requestNavigation(new Button(getContext()));
            break;
          case R.id.TransitRadio:
            MainActivity.directionType = RequestHandler.DirectionType.Transit;
            MainActivity.requestNavigation(new Button(getContext()));
            break;
          case R.id.WalkingRadio:
            MainActivity.directionType = RequestHandler.DirectionType.Walking;
            MainActivity.requestNavigation(new Button(getContext()));
            break;
        }
        Log.v("radioButton", (i == R.id.CyclingRadio) + "");
      }
    });
  }
  
  public void configureAutoComplete(PlaceAutocompleteFragment autocompleteFragment) {
    autocompleteFragment.getView().setBackgroundColor(Color.rgb(255, 255, 255));
    autocompleteFragment.getView().findViewById(R.id.place_autocomplete_search_button).setVisibility(View.INVISIBLE);
    autocompleteFragment.getView().setAlpha(0);
    autocompleteFragment.getView().setClickable(false);
    autocompleteFragment.getView().setVisibility(View.INVISIBLE);
    
  }
  
  public void configureOriginAndDestination() {
    configureAutoComplete(origin);
    configureAutoComplete(destination);
    origin.getView().setY(-300);
    destination.getView().animate().y(-100).start();
  }
  
  public static void hideOriginAndDestination() {
    origin.getView().setClickable(false);
    destination.getView().setClickable(false);
    origin.getView().animate().setDuration(600).y(-300).alpha(0).start();
    navigationRadioGroup.animate().setDuration(600).y(-70).alpha(0).start();
    destination.getView().animate().setDuration(600).y(-100).alpha(0).withEndAction(new Runnable() {
      @Override
      public void run() {
        origin.getView().setVisibility(View.INVISIBLE);
        destination.getView().setVisibility(View.INVISIBLE);
        Log.v("Origin", origin.getView().getY() + "," + destination.getView().getY());
      }
    }).start();
    
    autocompleteFragment.getView().animate().setDuration(600).alpha(1).y(MainActivity.calculateHeight(85.33)).start();
  }
  
  public static void showOriginAndDestination() {
    origin.getView().setClickable(true);
    destination.getView().setClickable(true);
    origin.getView().animate().setDuration(600).alpha(1).y(MainActivity.calculateHeight(85.34)).start();
    destination.getView().animate().setDuration(600).alpha(1).y(MainActivity.calculateHeight(12.8) + MainActivity.calculateHeight(85.34) + 20).start();
    origin.getView().setVisibility(View.VISIBLE);
    destination.getView().setVisibility(View.VISIBLE);
    navigationRadioGroup.animate().y(2 * (MainActivity.calculateHeight(12.8)) + MainActivity.calculateHeight(85.34) + 40).alpha(1).start();
    autocompleteFragment.getView().animate().setDuration(600).alpha(0).y(-200).start();
  }
  
  public void placeMarker(Place place) {
    if (place == null) {
      return;
    }
    removeMarker();
    MarkerOptions markerOptions = new MarkerOptions();
    markerOptions.position(place.getLatLng()).icon(BitmapDescriptorFactory.defaultMarker()).title(place.getName().toString());
    marker = mMap.addMarker(markerOptions);
    CameraPosition cameraPosition = new CameraPosition.Builder()
            .target(place.getLatLng())      // Sets the center of the map to Mountain View
            .zoom(15)                   // Sets the zoom
            .bearing(0)                // Sets the orientation of the camera to east
            .build();                   // Creates a CameraPosition from the builder
    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    markerType = MARKERTYPE.Name;
  }
  
  public static void removeParkingMarker() {
    if (carParkingMarkers != null && carParkingMarkers.length > 0) {
      for (Marker i : carParkingMarkers) {
        i.remove();
      }
    }
  }
  
  // RefreshSecond Setter Method
  public static void updateRefreshSecond() {
    MapsActivity.refreshSecond = DeviceInfo.getInstance().getRefreshTime() * 1000;
  }
  
  public void reportProcessBtn() {
    if (marker != null) {
      final String[] reportType = new String[1];
      final String dateTime = DateFormat.getDateTimeInstance().format(new Date());
      final Report[] report = new Report[1];
      final AlertDialog alertDialog;
      AlertDialog.Builder mBuilder = new AlertDialog.Builder(rootView.getContext());
      final View mView = MainActivity.mActivity.getLayoutInflater().inflate(R.layout.report_window, null);
      String[] array_spinner = getResources().getStringArray(R.array.reports_array);
      ArrayAdapter<String> adapter = new ArrayAdapter<>(rootView.getContext(), R.layout.support_simple_spinner_dropdown_item, array_spinner);
      Spinner spinner = mView.findViewById(R.id.spinner_window);
      spinner.setAdapter(adapter);
      spinner.setLayoutParams(new LinearLayout.LayoutParams(MainActivity.calculateWidth(100 / 80), ViewGroup.LayoutParams.WRAP_CONTENT));
      spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
          reportType[0] = adapterView.getSelectedItem().toString();
        }
        
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
          
        }
      });
      Log.v("view", "Start");
      final EditText comment = mView.findViewById(R.id.comment_window);
      comment.setLayoutParams(new LinearLayout.LayoutParams(MainActivity.calculateWidth(100 / 80), MainActivity.calculateHeight(8)));
      Button send = mView.findViewById(R.id.send_window);
      Button cancel = mView.findViewById(R.id.cancel_window);
      mBuilder.setView(mView);
      alertDialog = mBuilder.create();
      cancel.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          alertDialog.dismiss();
        }
      });
      
      send.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if(!reportType[0].equals("Please Select One")) {
            report[0] = new Report(dateTime, reportType[0], comment.getText().toString(), new Report.Location(marker.getPosition().latitude, marker.getPosition().longitude));
                      DatabaseConnect.insertReportData(report[0]);
                      processReport();
                      alertDialog.dismiss();
          } else {
            Toast.makeText(MainActivity.mActivity, "Please select report type.", Toast.LENGTH_SHORT).show();
          }
          
        }
      });
      alertDialog.show();
    } else {
      Toast.makeText(MainActivity.mActivity, "Please put marker", Toast.LENGTH_LONG).show();
    }
    
  }
  
  public static void processReport() {
    final String[] fetchData = DatabaseConnect.fetchData("handsomelee", "reports");
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voids) {
        while (mMap == null);
        while (fetchData[0] == null){ System.out.print("buffer"); }
        MainActivity.mActivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Gson gson = new Gson();
            Report.fetchReport[] reports = gson.fromJson(fetchData[0], Report.fetchReport[].class);
            if(reportMarkers != null && reportMarkers.length > 0) {
              for(Marker marker : reportMarkers) {
                marker.remove();
                marker = null;
              }
            }
            reportMarkers = new Marker[reports.length];
            for (int i = 0; i < reports.length; i++) {
              MarkerOptions options = new MarkerOptions()
                      .title(reports[i].type)
                      .position(reports[i].getLatLng())
                      .snippet(reports[i].comment);
              switch (reports[i].type) {
                case "Temporary Inspection":
                  options.icon(BitmapDescriptorFactory.fromResource(R.drawable.temporary_inspection));
                  break;
                case "Road Block":
                  options.icon(BitmapDescriptorFactory.fromResource(R.drawable.road_block));
                  break;
                case "Mobile Speed Track":
                  options.icon(BitmapDescriptorFactory.fromResource(R.drawable.speed_track));
                  break;
              }
              reportMarkers[i] = mMap.addMarker(options);
            }
            fetchData[0] = null;
          }
        });
        return null;
      }
    }.execute();
  }
  
  public static void processParking() {
    removeParkingMarker();
    final PlaceSearch placeSearch = MainActivity.placeSearch;
    if (MainActivity.placeSearch != null && placeSearch.status.equals("OK")) {
      MainActivity.mActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (placeSearch.results != null) {
            carParkingMarkers = new Marker[placeSearch.results.length];
            
            for (int i = 0; i < placeSearch.results.length; i++) {
              int available = 0;
              String title = "";
              int open_now = 0;
              if (placeSearch.results[i].hasPhotoReference()) {
                RequestHandler.requestGooglePhoto(placeSearch.results[i].getPhotoReference(), i);
              }
              if (placeSearch.results[i].name != null) {
                title = placeSearch.results[i].name;
              }
              if (placeSearch.results[i].opening_hours != null && placeSearch.results[i].opening_hours.has("open_now")) {
                if (placeSearch.results[i].opening_hours.get("open_now").getAsBoolean()) {
                  open_now = 1;
                } else {
                  open_now = -1;
                }
                Log.v("open", "" + open_now);
              }
              
              MarkerOptions markerOptions = new MarkerOptions()
                      .position(placeSearch.results[i].getLatLng())
                      .title(title)
                      .snippet(open_now + "," + available)
                      .icon(BitmapDescriptorFactory.fromResource(R.drawable.parking));
//              ParkingWindow parkingWindow = new ParkingWindow(MainActivity.mActivity);
//              mMap.setInfoWindowAdapter(parkingWindow);
              final String finalTitle = title;
              final int finalI = i;
              mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                  Log.v("title", finalTitle);
                  Log.v("index", finalI + "");
                  ParkingWindow.updateButton(marker.getTitle(), marker.getSnippet().split(",")[1], marker);
                }
              });
              carParkingMarkers[i] = mMap.addMarker(markerOptions);
              carParkingMarkers[i].setTag(String.format("parking"));
            }
            new AsyncTask<Void, Void, Void>() {
              @Override
              protected Void doInBackground(Void... voids) {
                final String fetchData[] = DatabaseConnect.fetchData("handsomelee", "carParking");
                while (fetchData[0] == null) ;
                Log.v("FetchData", fetchData[0]);
                
                MainActivity.mActivity.runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    Gson gson = new Gson();
                    final CarParking[] carParkings = gson.fromJson(fetchData[0], CarParking[].class);
                    for (Marker marker : carParkingMarkers) {
                      for (CarParking carParking : carParkings) {
                        if (carParking.getName().equals(marker.getTitle())) {
                          String snippet = marker.getSnippet().split(",")[0];
                          marker.setSnippet(snippet + "," + carParking.getAvailable());
                        }
                      }
                    }
                  }
                });
                
                return null;
              }
            }.execute();
          } else {
            Toast.makeText(MainActivity.mActivity, "Parking Not Found.", Toast.LENGTH_LONG);
          }
        }
      });
    } else {
      Toast.makeText(MainActivity.mActivity, "Parking Request Failed.", Toast.LENGTH_LONG);
    }
  }
  
  @Override
  public boolean onMarkerClick(Marker marker) {
    Log.v("marker", "inside");
    if (marker.getTag() != null) {
      if (((String) marker.getTag()).equals("parking")) {
        Log.v("marker", "parking");
        mMap.setInfoWindowAdapter(new ParkingWindow(MainActivity.mActivity));
      }
    } else {
      mMap.setInfoWindowAdapter(null);
    }
    marker.showInfoWindow();
    return true;
  }
  
  @Override
  public void onMapClick(LatLng latLng) {
    if (progressType == RequestHandler.ProgressType.Free) {
      autocompleteFragment.setText("");
      removeMarker();
      hideNavigationBtn();
    }
  }
  
  @Override
  public void onMapLongClick(LatLng latLng) {
    super.onMapLongClick(latLng);
    hideOriginAndDestination();
    removePolyline();
    closeListView();
    navigationBtn.setText("navigation");
    MapsActivity.setProgressType(RequestHandler.ProgressType.Free);
    autocompleteFragment.setText(String.format("%.4f,\t%.4f", latLng.latitude, latLng.longitude));
    if (mMap.getCameraPosition().zoom >= 15f) {
      CameraPosition cameraPosition = new CameraPosition.Builder()
              .target(mMap.getCameraPosition().target)
              .zoom(15f)
              .build();
      MapsActivity.getmMap().animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
    showNavigationBtn();
  }
  
  public static String getDestinationString() {
    return destinationString;
  }
  
  // navigationBtn Getter Method
  public Button getNavigationBtn() {
    return navigationBtn;
  }
  
  // progressType Getter Method
  public static RequestHandler.ProgressType getProgressType() {
    return progressType;
  }
  
  // ProgressType Setter Method
  public static void setProgressType(RequestHandler.ProgressType progressType) {
    MapsActivity.progressType = progressType;
  }
  
  
  @Override
  public void onDestroyView() {
    super.onDestroyView();
    MainActivity.removeFragment(R.id.placeSearch);
    MainActivity.removeFragment(R.id.Origin);
    MainActivity.removeFragment(R.id.Destination);
  }
  
  @Override
  public void onPlaceSelected(Place place) {
    // TODO: Get info about the selected place.
    Log.i(TAG, "Place: " + place.getName());
    destinationString = place.getName().toString();
    Log.v("Place Address", place.getName().toString());
    placeMarker(place);
    showNavigationBtn();
  }
  
  public static void configureRouteDetail() {
    int viewId[] = {R.id.Distance, R.id.duration, R.id.html_instructions, R.id.travel_mode, R.id.maneuver};
    mAdapter = new RouteDetailAdapter(MainActivity.mActivity, routeInfo.routeDetails, R.layout.simple_list, viewId);
    listView.setAdapter(mAdapter);
  }
  
  @Override
  public void onError(Status status) {
    // TODO: Handle the error.
    Log.i(TAG, "An error occurred: " + status);
  }
  
}
