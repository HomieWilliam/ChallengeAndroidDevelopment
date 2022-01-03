package co.mz.draft.android.android.challenge;

import static co.mz.draft.android.android.challenge.util.Constant.REQUEST_GPS;
import static co.mz.draft.android.android.challenge.util.Utils.convertToCelsius;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import co.mz.draft.android.android.challenge.databinding.FragmentItemListBinding;
import co.mz.draft.android.android.challenge.databinding.ItemListContentBinding;
import co.mz.draft.android.android.challenge.placeholder.PlaceholderContent;
import co.mz.draft.android.android.challenge.response.city.name.WeatherResponse;
import co.mz.draft.android.android.challenge.util.Constant;
import co.mz.draft.android.android.challenge.util.Utils;

/**
 * A fragment representing a list of Items. This fragment
 * has different presentations for handset and larger screen devices. On
 * handsets, the fragment presents a list of items, which when touched,
 * lead to a {@link ItemDetailFragment} representing
 * item details. On larger screens, the Navigation controller presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ItemListFragment extends Fragment{

    /**
     * Method to intercept global key events in the
     * item list fragment to trigger keyboard shortcuts
     * Currently provides a toast when Ctrl + Z and Ctrl + F
     * are triggered
     */

    private View.OnContextClickListener onContextClickListener;
    private View.OnClickListener onClickListener;
    private RecyclerView recyclerView;
    private FragmentItemListBinding binding;
    private FusedLocationProviderClient locationProvider;
    private Location location;
    private Context context;
    private Activity activity;

    private int totalContries;

    private static boolean askedForLocationPermission = false;

    private final static List<PlaceholderContent.PlaceholderItem> mValues = new ArrayList<>();
    private final Map<String, String> queryParameters = new HashMap<>();

    private TextView weatherDescription;
    private ImageView weatherIc;
    private TextView currentCity;
    private ImageView locationPin;
    private TextView temp;
    private TextView wind;
    private TextView humidity;

    ViewCompat.OnUnhandledKeyEventListenerCompat unhandledKeyEventListenerCompat = (v, event) -> {
        if (event.getKeyCode() == KeyEvent.KEYCODE_Z && event.isCtrlPressed()) {
            Toast.makeText(
                    v.getContext(),
                    "Undo (Ctrl + Z) shortcut triggered",
                    Toast.LENGTH_LONG
            ).show();
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_F && event.isCtrlPressed()) {
            Toast.makeText(
                    v.getContext(),
                    "Find (Ctrl + F) shortcut triggered",
                    Toast.LENGTH_LONG
            ).show();
            return true;
        }
        return false;
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentItemListBinding.inflate(inflater, container, false);

        context = requireActivity().getApplicationContext();
        if(context!=null){
            AndroidNetworking.initialize(context);
            locationProvider = LocationServices.getFusedLocationProviderClient(context);
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProgressIndicator(true);

        mValues.clear();
        String[] list = getResources().getStringArray(R.array.cities);
        totalContries = list.length;
        for (String city:list){
            requestTemp(city);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.addOnUnhandledKeyEventListener(view, unhandledKeyEventListenerCompat);
        recyclerView = binding.itemList;

        weatherDescription = binding.weatherDescription;
        weatherIc = binding.weatherIc;
        currentCity = binding.currentCity;
        locationPin = binding.locationPin;
        temp = binding.temp;
        wind = binding.wind;
        humidity = binding.humidity;

        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        View itemDetailFragmentContainer = view.findViewById(R.id.item_detail_nav_container);

        /* Click Listener to trigger navigation based on if you have
         * a single pane layout or two pane layout
         */

         onClickListener = itemView -> {
            PlaceholderContent.PlaceholderItem item =
                    (PlaceholderContent.PlaceholderItem) itemView.getTag();
            Bundle arguments = new Bundle();
            arguments.putString(ItemDetailFragment.ARG_ITEM_ID, item.id);
            arguments.putString(ItemDetailFragment.ARG_ITEM_RESPONSE, new Gson().toJson(item.response));

            if (itemDetailFragmentContainer != null) {
                Navigation.findNavController(itemDetailFragmentContainer)
                        .navigate(R.id.fragment_item_detail, arguments);
            } else {
                Navigation.findNavController(itemView).navigate(R.id.show_item_detail, arguments);
            }
        };

        /*
         * Context click listener to handle Right click events
         * from mice and trackpad input to provide a more native
         * experience on larger screen devices
         */
         onContextClickListener = itemView -> {
            PlaceholderContent.PlaceholderItem item =
                    (PlaceholderContent.PlaceholderItem) itemView.getTag();
            Toast.makeText(
                    itemView.getContext(),
                    "Context click of item " + item.id,
                    Toast.LENGTH_LONG
            ).show();
            return true;
        };

    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {


        private final View.OnClickListener mOnClickListener;
        private final View.OnContextClickListener mOnContextClickListener;

        SimpleItemRecyclerViewAdapter(
                                      View.OnClickListener onClickListener,
                                      View.OnContextClickListener onContextClickListener) {
            mOnClickListener = onClickListener;
            mOnContextClickListener = onContextClickListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            ItemListContentBinding binding = ItemListContentBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            String description = mValues.get(position).response.getWeather().get(0).getDescription();
            int weatherDrawableId = Utils.getDrawableId(description);

            holder.mIdView.setText(mValues.get(position).id);
            holder.mContentView.setImageResource(weatherDrawableId);
            holder.mWeatherView.setText(
                    convertToCelsius(mValues.get(position).response.getMain().getTemp()));
            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
            holder.itemView.setOnContextClickListener(mOnContextClickListener);

            holder.itemView.setOnLongClickListener(v -> {
                // Setting the item id as the clip data so that the drop target is able to
                // identify the id of the content
                ClipData.Item clipItem = new ClipData.Item(mValues.get(position).id);
                ClipData dragData = new ClipData(
                        ((PlaceholderContent.PlaceholderItem) v.getTag()).content,
                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                        clipItem
                );

                v.startDragAndDrop(
                        dragData,
                        new View.DragShadowBuilder(v),
                        null,
                        0
                );
                return true;
            });

            if (mValues.size()==totalContries){
                requestLocation();
                loadProgressIndicator(false);
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final ImageView mContentView;
            final TextView mWeatherView;

            ViewHolder(ItemListContentBinding binding) {
                super(binding.getRoot());
                mIdView = binding.idCity;
                mContentView = binding.content;
                mWeatherView = binding.weather;
            }

        }
    }

    private void setupRecyclerView(
            RecyclerView recyclerView,
            View.OnClickListener onClickListener,
            View.OnContextClickListener onContextClickListener
    ) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(
                onClickListener,
                onContextClickListener
        ));
    }

    private void requestTemp(String city){
        queryParameters.clear();
        queryParameters.put("APPID", Constant.API_KEY);

        if(city!=null){
            queryParameters.put("q", city);
        }
        else {
            queryParameters.put("lat", String.valueOf(location.getLatitude()));
            queryParameters.put("lon", String.valueOf(location.getLongitude()));
        }
        AndroidNetworking.get(Constant.BASE_URL)
            .addQueryParameter(queryParameters)
            .setTag(this)
            .build()
            .getAsJSONObject(new JSONObjectRequestListener(){
                     @Override
                     public void onResponse(JSONObject resp) {
                         WeatherResponse response =
                                 new Gson().fromJson(resp.toString(), WeatherResponse.class);

                         PlaceholderContent.PlaceholderItem item = new PlaceholderContent.PlaceholderItem(
                                 response.getName(), String.valueOf(response.getMain().getTemp()), response.getBase(), response);

                         if(city!=null){
                             mValues.add(item);
                             if(totalContries==mValues.size()){
                                 setupRecyclerView(recyclerView, onClickListener, onContextClickListener);
                             }
                         }
                         else {
                             if(!response.getWeather().isEmpty()){
                                 String description = response.getWeather().get(0).getDescription();
                                 weatherDescription.setText(description);
                                 weatherIc.setImageResource(Utils.getDrawableId(description));
                             }
                             currentCity.setText(response.getName());
                             locationPin.setImageResource(R.drawable.ic_location_pin);
                             temp.setText(convertToCelsius(response.getMain().getTemp()));
                             wind.setText(String.valueOf(response.getWind().getSpeed()).concat("Km/h"));
                             humidity.setText(String.valueOf(response.getMain().getHumidity()).concat("%"));
                         }

                     }
                     @Override
                     public void onError(ANError anError) {

                     }
                 }
            );
    }

    @SuppressLint("MissingPermission")
    private Task<LocationSettingsResponse> initTaskSuccessListener(LocationSettingsRequest.Builder locationSettingBuilder){
        SettingsClient client = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(locationSettingBuilder.build());

        task.addOnSuccessListener(activity, locationSettingsResponse ->{
            locationProvider.getLastLocation().addOnCompleteListener(locationProviderTask->{
                if (locationProviderTask.getResult() != null){
                    location = locationProviderTask.getResult();
                    if(location!=null){
                        requestTemp(null);
                        askedForLocationPermission = true;
                    }
                }
            });

            if(location==null){
                startLocationUpdates();
            }
        });

        return task;
    }

    private void initTaskFailureListener(Task<LocationSettingsResponse> task){
        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    if(!askedForLocationPermission){
                        askedForLocationPermission = true;

                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(activity, REQUEST_GPS);
                    }

                } catch (IntentSender.SendIntentException sendIntentException) {
                    sendIntentException.printStackTrace();
                    Log.e("ItemDetailHostActivity", "onFailure: Pending Intent unable to execute Request");
                }
            }
        });
    }

    private void requestLocation() {
        activity = getActivity();

        Dexter.withContext(context)
            .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(new MultiplePermissionsListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                    if(multiplePermissionsReport.areAllPermissionsGranted()){
                        LocationSettingsRequest.Builder locationSettingBuilder = new LocationSettingsRequest.Builder()
                                .addLocationRequest(locationRequest).setAlwaysShow(true);

                        if(activity!=null){
                            Task<LocationSettingsResponse> task = initTaskSuccessListener(locationSettingBuilder);
                            if(location==null){
                                initTaskFailureListener(task);
                            }
                        }
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                }
            }).check();
    }

    private void startLocationUpdates(){
        Dexter.withContext(context)
            .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(new MultiplePermissionsListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        locationProvider.requestLocationUpdates(locationRequest, callback, Looper.myLooper());
                        if(location!=null){
                            requestTemp(null);
                        }
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                }
            }).check();
    }

    private LocationRequest locationRequest = LocationRequest.create()
            .setFastestInterval(Constant.FASTEST_INTERVAL)
            .setInterval(Constant.UPDATE_INTERVAL)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private LocationCallback callback = new LocationCallback(){
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if(!locationResult.getLocations().isEmpty()){
                location = locationResult.getLocations().get(0);
                if(location!=null){
                    requestTemp(null);
                }
            }
        }
    };

    private void loadProgressIndicator(boolean state){
        if(binding.progressIndicator!=null){
            binding.progressIndicator.setVisibility(View.VISIBLE);

            if(!state){
                binding.progressIndicator.setVisibility(View.GONE);
            }
        }
    }

}