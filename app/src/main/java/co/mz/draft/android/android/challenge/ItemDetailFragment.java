package co.mz.draft.android.android.challenge;

import static co.mz.draft.android.android.challenge.util.Utils.convertToCelsius;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Date;
import co.mz.draft.android.android.challenge.databinding.FragmentItemDetailBinding;
import co.mz.draft.android.android.challenge.placeholder.PlaceholderContent;
import co.mz.draft.android.android.challenge.response.city.name.WeatherResponse;
import co.mz.draft.android.android.challenge.util.Utils;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListFragment}
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
public class ItemDetailFragment extends Fragment {

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_RESPONSE = "ItemResponse";

    /**
     * The placeholder content this fragment is presenting.
     */
    private PlaceholderContent.PlaceholderItem mItem;

    private TextView mTextView;
    private TextView mCity;
    private TextView mCityWind;
    private TextView mCityHumidity;
    private TextView mDate;
    private TextView mWeatherDes;
    private TextView mTemp;
    private TextView mPressureValue;
    private TextView mMin;
    private TextView mMax;
    private ImageView mBack;

    private ImageView mWeatherImg;
    private ImageView mImgWeather;

    private final View.OnDragListener dragListener = (v, event) -> {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            ClipData.Item clipDataItem = event.getClipData().getItemAt(0);
            mItem = PlaceholderContent.ITEM_MAP.get(clipDataItem.getText().toString());
            updateContent(getArguments());
        }
        return true;
    };
    private FragmentItemDetailBinding binding;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the placeholder content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = PlaceholderContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentItemDetailBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        mTextView = binding.itemDetail;
        mCity = binding.city;
        mCityWind = binding.cityWind;
        mCityHumidity = binding.cityHumidity;
        mDate = binding.date;
        mWeatherImg = binding.weatherImg;
        mImgWeather = binding.imgWeather;
        mWeatherDes = binding.weatherDes;
        mTemp = binding.temp;
        mPressureValue = binding.pressureValue;
        mMin = binding.min;
        mMax = binding.max;
        mBack = binding.back;

        // Show the placeholder content as text in a TextView & in the toolbar if available.
        updateContent(getArguments());

        rootView.setOnDragListener(dragListener);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBack.setOnClickListener(v ->Navigation.findNavController(view).popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateContent(Bundle arguments) {
        if (mItem != null) {
            mTextView.setText(mItem.details);
        }

        if(arguments!=null){
            Gson gson = new Gson();
            WeatherResponse response = (gson.fromJson(arguments.getString("ItemResponse")
                    , WeatherResponse.class));

            Context context = getContext();

            if(response!=null && context!=null){
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy");
                Date date = new Date();

                String description = response.getWeather().get(0).getDescription();

                mCity.setText(response.getName());
                mCityWind.setText(String.valueOf(response.getWind().getSpeed()).concat("Km/h"));
                mCityHumidity.setText(String.valueOf(response.getMain().getHumidity()).concat("%"));
                mDate.setText(simpleDateFormat.format(date));
                mWeatherImg.setImageResource(Utils.getDrawableId(description));
                mImgWeather.setImageResource(Utils.getDrawableId(description));
                mWeatherDes.setText(description);
                mTemp.setText(convertToCelsius(response.getMain().getTemp()));
                mPressureValue.setText(String.valueOf(response.getMain().getPressure())
                        .concat(getString(R.string.mb)));
                mMin.setText(getString(R.string.min_temp)
                        .concat(convertToCelsius(response.getMain().getTemp_min())));
                mMax.setText(getString(R.string.max_temp)
                        .concat(convertToCelsius(response.getMain().getTemp_max())));

            }
        }
    }
}