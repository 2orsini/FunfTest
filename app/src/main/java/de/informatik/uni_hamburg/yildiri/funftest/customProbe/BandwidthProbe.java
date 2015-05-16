package de.informatik.uni_hamburg.yildiri.funftest.customProbe;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import de.informatik.uni_hamburg.yildiri.funftest.SettingsActivity;
import de.informatik.uni_hamburg.yildiri.funftest.utils.AsyncResponse;
import de.informatik.uni_hamburg.yildiri.funftest.tools.BandwidthMeasureTool;
import de.informatik.uni_hamburg.yildiri.funftest.utils.BandwidthResultRecord;
import de.informatik.uni_hamburg.yildiri.funftest.tools.HttpURLConnectionMeasureTool;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.Base;

/**
 * This is a funf probe to measure the bandwidth of the device. The probe won't force to establish a connection if the device is not already connected by the time the probe starts running.
 * Further technical details of the measurement (like whether the measurement is active or passive) depend on the implementing class of the BandwidthMeasureTool being used.
 */
@Probe.DisplayName("Bandwidth measuring probe")
@Probe.RequiredFeatures("android.hardware.wifi")
@Probe.RequiredPermissions(android.Manifest.permission.INTERNET)
@Schedule.DefaultSchedule(interval = 120)
public class BandwidthProbe extends Base {

    /**
     * The BandwidthMeasureTool that is actively running the measurement. Concrete technical details of the measurement (like whether the measurement is active or passive) depend on the implementing class being used (by default HttpURLConnectionMeasureTool is used)
     */
    BandwidthMeasureTool measureTool;
    /**
     * Boolean whether the device is connected to the internet or not (the type of connection doesn't matter here)
     */
    boolean isConnected;
    /**
     * If the device is connected to the internet, this field holds the information what type of connection is currently established (WiFi, GSM, ...).
     * The connection types match the ones given by <code>ConnectivityManager.TYPE_*</code> - except for when no connection is up, then we use our own constant {@link de.informatik.uni_hamburg.yildiri.funftest.customProbe.BandwidthProbe#NO_CONNECTION}
     */
    int connectionType = NO_CONNECTION;

    /**
     * Connection type that indicates that the device has no connection to the internet
     */
    public static final int NO_CONNECTION = -1;

    @Override
    protected void onStart() {
        super.onStart();
        isConnected = isConnected();
        connectionType = getConnectionType();

        // Initialize and run the measurement tool
        // Also since BandwidthMeasureTool is an AsyncTask, this is going to run in an other new thread. Thus we also need to define an inner-anonymous class implementing the async response to process the measurement finish
        String currentFileURL = getFileURLFromAppPreferences();
        measureTool = new HttpURLConnectionMeasureTool(new AsyncResponse() {
            @Override
            public void processFinish(BandwidthResultRecord bandwidthResultRecord) {
                if(bandwidthResultRecord.hasMeasurementSucceeded())
                {
                    // Process the measurement finish by packing all the bandwidth results in a bundle and sending that data to all listeners
                    Log.d(getClass().getSimpleName(), "Going to get and pack the data Bundle");
                    Bundle data = packDataBundle(bandwidthResultRecord);
                    sendData(getGson().toJsonTree(data).getAsJsonObject());
                    Log.d(getClass().getSimpleName(), "Got and sent all the data bundles");
                }
                else {
                    // Failed measurement, report this incident
                    Exception measurementException = bandwidthResultRecord.getMeasurementException();
                    if(measurementException != null) {
                        displayMeasurementErrorToast(measurementException);
                    }
                }
                // Measurement and all post-processing has completed, so we can stop the probe now
                stop();
            }
        }, getContext(), currentFileURL, connectionType);
        measureTool.execute(currentFileURL);
    }

    /**
     * Get the NetworkInfo of the device
     *
     * @param context the context of this app
     * @return the NetworkInfo containing the information of the currently active network
     */
    private NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connMgr.getActiveNetworkInfo();
    }

    /**
     * Get whether the device is currently connected to the internet
     *
     * @return whether the device is currently connected to the internet
     */
    protected boolean isConnected() {
        NetworkInfo netInfo = getNetworkInfo(getContext());
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Get the current type of connection of the device
     *
     * @return current type of connection of the device
     */
    protected int getConnectionType() {
        if (isConnected()) {
            return getNetworkInfo(getContext()).getType();
        } else return NO_CONNECTION;
    }

    /**
     * Read the currently set file URL from the app preferences
     * @return string of the file URL
     */
    private String getFileURLFromAppPreferences() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String fileURL = sharedPref.getString("pref_key_testfileURL", "");
        Log.d(getClass().getSimpleName(), "Gotten file URL: " + fileURL + "  from the app preferences");
        return fileURL;
    }

    /**
     * Display a toast on screen that contains the error message of a failed bandwidth measurement
     * @param measurementException exception that has been thrown while measuring
     */
    private void displayMeasurementErrorToast(final Exception measurementException) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                final Exception currentMeasurementException = measurementException;
                Toast toast = Toast.makeText(getContext(), "BandwidthProbe: Error while trying to measure (" + currentMeasurementException.getMessage() + ")", Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    /**
     * Return the data bundle, that results by packing it with all the bandwidth measurement results
     *
     * @param bandwidthResultRecord the bandwidth measurement results in form of a BandwidthResultRecord object
     * @return data bundle containing all the bandwidth measurement results
     */
    private Bundle packDataBundle(BandwidthResultRecord bandwidthResultRecord) {
        Bundle data = new Bundle();
        Log.d(getClass().getSimpleName(), "Bandwidth measure overall total speed: " + bandwidthResultRecord.getOverallTotalBandwidthMeasure() + " kbit/s");

        // Put all the data from the BandwidthResultRecord in the bundle
        Log.d(getClass().getSimpleName(), "Packing the data bundle");
        data.putString(BandwidthProbeKeys.URL, bandwidthResultRecord.getFileURL().toString());
        data.putLong(BandwidthProbeKeys.FILE_SIZE, bandwidthResultRecord.getFileSize());
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_100, bandwidthResultRecord.getBandwidthMeasure(0));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_200, bandwidthResultRecord.getBandwidthMeasure(1));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_300, bandwidthResultRecord.getBandwidthMeasure(2));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_400, bandwidthResultRecord.getBandwidthMeasure(3));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_500, bandwidthResultRecord.getBandwidthMeasure(4));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_600, bandwidthResultRecord.getBandwidthMeasure(5));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_700, bandwidthResultRecord.getBandwidthMeasure(6));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_800, bandwidthResultRecord.getBandwidthMeasure(7));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_900, bandwidthResultRecord.getBandwidthMeasure(8));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1000, bandwidthResultRecord.getBandwidthMeasure(9));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1100, bandwidthResultRecord.getBandwidthMeasure(10));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1200, bandwidthResultRecord.getBandwidthMeasure(11));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1300, bandwidthResultRecord.getBandwidthMeasure(12));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1400, bandwidthResultRecord.getBandwidthMeasure(13));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1500, bandwidthResultRecord.getBandwidthMeasure(14));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1600, bandwidthResultRecord.getBandwidthMeasure(15));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1700, bandwidthResultRecord.getBandwidthMeasure(16));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1800, bandwidthResultRecord.getBandwidthMeasure(17));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_1900, bandwidthResultRecord.getBandwidthMeasure(18));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_2000, bandwidthResultRecord.getBandwidthMeasure(19));
        data.putDouble(BandwidthProbeKeys.BANDWIDTH_TOTAL, bandwidthResultRecord.getBandwidthMeasure(bandwidthResultRecord.TOTAL_BANDWIDTH_INDEX));

        return data;
    }

}
