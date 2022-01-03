package co.mz.draft.android.android.challenge.util;

import co.mz.draft.android.android.challenge.R;

public class Utils {
    public static int getDrawableId(String description){
        int weatherDrawableId = R.drawable.ic_sun;

        switch (description){
            case "overcast clouds" : weatherDrawableId = R.drawable.ic_cloudy_day; break;
            case "broken clouds" : weatherDrawableId = R.drawable.ic_rain;break;
            case "few clouds" : weatherDrawableId = R.drawable.ic_cloudy_day;break;
            case "light intensity drizzle" : weatherDrawableId = R.drawable.ic_drizzle; break;
            case "mist" : weatherDrawableId = R.drawable.ic_misc;break;
            case "scattered clouds" : weatherDrawableId = R.drawable.ic_scattered;break;
        }

        return weatherDrawableId;
    }

    public static String convertToCelsius(double kelvin){
        return Math.round(kelvin - 273.15) +""+ (char) 0x00B0;
    }

}
