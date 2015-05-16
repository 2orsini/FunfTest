package de.informatik.uni_hamburg.yildiri.funftest.utils;

/**
 * This interface is part of a workaround like solution to implement class local async responses to the finish of an AsyncTask.
 * For this the BandwidthMeasureTool calls processFinish on it's onPostExecute with the resulting BandwidthResultRecord.
 * The trick is that the AsyncResponse implementation is delegated via the BandwidthMeasureTool constructor to the target class of interest. That way a class can locally implement the processing of the async response.
 */
public interface AsyncResponse {

    /**
     * Callback method when the measurement has finished
     *
     * @param bandwidthResultRecord contains the results of the finished measurement
     */
    void processFinish(BandwidthResultRecord bandwidthResultRecord);
}
