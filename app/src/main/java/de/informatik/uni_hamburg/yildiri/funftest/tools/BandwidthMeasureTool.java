package de.informatik.uni_hamburg.yildiri.funftest.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import de.informatik.uni_hamburg.yildiri.funftest.utils.AsyncResponse;
import de.informatik.uni_hamburg.yildiri.funftest.utils.BandwidthResultRecord;

/**
 * This abstract class lays the foundations for a tool to actually run the bandwidth measurement.
 * The code scopes concerning technical implementational details of the measurement have been located into abstract methods, so that an implementing class can hook into and implement these parts accordingly.
 */
public abstract class BandwidthMeasureTool extends AsyncTask<String, Integer, BandwidthResultRecord> {

    /**
     * Callback interface to be delegated to the outside
     */
    public AsyncResponse delegate;

    /**
     * The context of the app. Primarily needed to get the files dir of this app.
     */
    protected Context context;
    /**
     * URL of the file to be downloaded for the measurement
     */
    protected String fileUrl;
    protected int connectionType;
    /**
     * Time in milliseconds when the measurement has started
     */
    protected long startTime;
    /**
     * Time in milliseconds when the whole measurement has completed
     */
    protected long endTime;
    /**
     * Results of the bandwidth measurement
     */
    private BandwidthResultRecord bandwidthResultRecord;

    /**
     * Granularity control of how often the progress of the AsyncTask should roughly be updated. E.g 0.1 would mean that the progress should update roughly in steps of 10%
     */
    protected static final double UPDATE_PROGRESS_EACH_X_PERCENT = 0.1;
    /**
     * Block size in kilobytes at which micro measurements should occur
     */
    protected static final int MEASURE_BLOCK_SIZE_IN_KB = 100;
    /**
     * Block size in bytes at which micro measurements should occur
     */
    protected static final int MEASURE_BLOCK_SIZE_IN_BYTES = MEASURE_BLOCK_SIZE_IN_KB * 1000;


    /**
     *
     * @param asyncResponse delegated callback interface that has to be implemented to process the async finish response
     * @param context the context of the app
     * @param fileUrl the url of the file to be downloaded for the measurement
     * @param connectionType the type of the current connection
     */
    public BandwidthMeasureTool(AsyncResponse asyncResponse, Context context, String fileUrl, int connectionType) {
        this(fileUrl, connectionType);
        this.delegate = asyncResponse;
        this.context = context;
    }

    /**
     *
     * @param fileUrl the url of the file to be downloaded for the measurement
     * @param connectionType the type of the current connection
     */
    public BandwidthMeasureTool(String fileUrl, int connectionType) {
        if (validArguments(fileUrl, connectionType)) {
            this.fileUrl = fileUrl;
            this.connectionType = connectionType;
        } else throw new IllegalArgumentException();
    }

    /**
     * Runs the actual bandwidth measurement
     * @return the result of the bandwidth measurement
     * @throws IOException if some errors occur while trying to establish the connection and download the file
     */
    abstract public BandwidthResultRecord measureBandwidth() throws IOException;

    @Override
    protected BandwidthResultRecord doInBackground(String... fileUrl) {
        try {
            if (!this.fileUrl.equals("") && this.fileUrl.equals(fileUrl[0])) {
                bandwidthResultRecord = measureBandwidth();
                return bandwidthResultRecord;
            } else
                throw new IllegalArgumentException("doInBackground fileUrl param != initialized field fileUrl");
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error measuring the bandwidth. " + e.toString());
            e.printStackTrace();
            BandwidthResultRecord failedBandwidthResultRecord = new BandwidthResultRecord(e);
            return failedBandwidthResultRecord;
        }
    }

    @Override
    protected void onPostExecute(BandwidthResultRecord bandwidthResultRecord) {
        // delegate the measurement result to the callback interface
        delegate.processFinish(bandwidthResultRecord);
    }

    /**
     * Validate the arguments passed to the constructor
     * @param fileUrl URL of the file to be downloaded for the measurement
     * @param connectionType the type of the current connection
     * @return whether the arguments seem to be valid
     */
    protected boolean validArguments(String fileUrl, int connectionType) {
        boolean connectionTypeValid = true;
        boolean fileUrlValid = true;
        // boolean connectionTypeValid = connectionType == ConnectivityManager.TYPE_WIFI II connectionType == ConnectivityManager.TYPE_MOBILE;
        return connectionTypeValid && fileUrlValid;
    }

    /**
     * Utility method to calculate the download rate of a file
     * @param startTime time in milliseconds when the measurement was started
     * @param endTime   time in milliseconds when the measurement completed
     * @param fileSize  size of the downloaded file in bytes
     * @return the download rate in kbit/s
     */
    public static double calcDownloadrate(long startTime, long endTime, long fileSize) {
        double diffTimeSec = (endTime - startTime) / 1000.0;
        double fileSizeKB = fileSize / 1024.0;
        double rateKBytePerSec = fileSizeKB / diffTimeSec;
        double rateKbitPerSec = rateKBytePerSec * 8.0;
        return rateKbitPerSec;
    }

    /**
     * Get the url of the file that is being downloaded for the measurement
     * @return the url of the file that is being downloaded for the measurement
     */
    public String getFileUrl() {
        return this.fileUrl;
    }

    /**
     * Get the type of the current connection
     * @return type of the current connection
     */
    public int getConnectionType() {
        return this.connectionType;
    }

    /**
     * Get the time when this measurement has had started
     * @return time in milliseconds when this measurement has had started
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Get the time when this measurement has had completed
     * @return time in milliseconds when this measurement has had completed
     */
    public long getEndTime() {
        return this.endTime;
    }

    /**
     * Get the results of this measurement
     * @return results of this measurement
     */
    public BandwidthResultRecord getBandwidthResultRecord() {
        return this.bandwidthResultRecord;
    }
}
