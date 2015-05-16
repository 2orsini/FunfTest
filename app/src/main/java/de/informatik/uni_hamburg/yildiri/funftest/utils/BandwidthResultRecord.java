package de.informatik.uni_hamburg.yildiri.funftest.utils;

import java.net.URL;

/**
 * This is a basic record class to hold the data of a resulting bandwidth measure.
 * For now it is pretty much only used to organize the data neatly and be able to pass it between classes.
 * Failed measurements may also be represented by a non-null BandwidthResultRecord. For this the measurement exception should be passed accordingly by calling the non-default constructor which awaits the exception that has caused the measurement to fail.
 */
public class BandwidthResultRecord {

    /**
     * The URL of the file that has been downloaded in the measurement
     */
    private URL fileURL;
    /**
     * The size of the file that has been downloaded in the measurement
     */
    private long fileSize;

    /**
     * The number of blocks, for which micro measurements were gathered
     */
    private final int BANDWIDTH_BLOCK_RECORDS = 20;
    /**
     * The index for the bandwidthMeasures array, where the value for the overall total bandwidth is going to be stored
     */
    public final int TOTAL_BANDWIDTH_INDEX = BANDWIDTH_BLOCK_RECORDS; // interpreted as index - so the overall total bandwidth is the very last entry in the array
    /**
     * Array that holds all the bandwidth measures. For i = 0, ..., 19 it holds the bandwidth measures of the (i+1) * 100 KB - e.g 0 - 100KB, 1 - 200KB, etc.
     * Lastly, i = {@link de.informatik.uni_hamburg.yildiri.funftest.utils.BandwidthResultRecord#TOTAL_BANDWIDTH_INDEX}  holds the overall total bandwidth
     */
    private double[] bandwidthMeasures = new double[BANDWIDTH_BLOCK_RECORDS + 1];

    /**
     * If the measurement failed for some reasons, an exception will be thrown and saved in this field. Whenever <code>measurementException != null</code> the measurement can be interpreted as failed.
     */
    private Exception measurementException = null;


    public BandwidthResultRecord() {
    }

    /**
     * Constructor to use, if the bandwidth measurement failed for some reasons and an exception was thrown while measuring
     *
     * @param measurementException exception that has been thrown while measuring
     */
    public BandwidthResultRecord(Exception measurementException) {
        setMeasurementException(measurementException);
    }

    /**
     * Set the URL of the file that has been downloaded in the measurement
     *
     * @param fileURL the URL of the file that has been downloaded in the measurement
     */
    public void setFileURL(URL fileURL) {
        if (this.fileURL == null) {
            this.fileURL = fileURL;
        }
    }

    /**
     * Get the URL fo the file that has been downloaded in the measurement
     *
     * @return the URL of the file that has been downloaded in the measurement
     */
    public URL getFileURL() {
        return this.fileURL;
    }

    /**
     * Set the size of the file that has been downloaded in the measurement
     *
     * @param fileSize size of the file that has been downloaded in the measurement
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Get the size of the file that has been downloaded in the measurement
     *
     * @return size of the file that has been downloaded in the measurement
     */
    public long getFileSize() {
        return this.fileSize;
    }

    /**
     * Set the measured bandwidth for a specific block record in the bandwidthMeasures array
     *
     * @param index   index of the block in the bandwidthMeasures array
     * @param measure the measured bandwidth in kbit/s
     */
    public void setBandwidthMeasure(int index, double measure) {
        bandwidthMeasures[index] = measure;
    }

    /**
     * Get the measured bandwidth for a specific block record in the bandwidthMeasures array
     *
     * @param index index of the block in the bandwidthMeasures array
     * @return the measured bandwidth in kbit/s
     */
    public double getBandwidthMeasure(int index) {
        return bandwidthMeasures[index];
    }

    /**
     * Get the total overall bandwidth of the measurement in kbit/s
     *
     * @return total overall bandwidth of the measurement in kbit/s
     */
    public double getOverallTotalBandwidthMeasure() {
        return getBandwidthMeasure(TOTAL_BANDWIDTH_INDEX);
    }

    /**
     * Set the exception that has been thrown while measuring, that caused the measurement to fail
     *
     * @param measurementException exception that has been thrown while measuring
     */
    private void setMeasurementException(Exception measurementException) {
        this.measurementException = measurementException;
    }

    /**
     * Get the exception that has been thrown while measuring - if no exception has occurred this method will return null
     *
     * @return exception that has been thrown while measuring - if no exception has occurred this method will return null
     */
    public Exception getMeasurementException() {
        return this.measurementException;
    }

    /**
     * Return whether the measurement has succeeded
     *
     * @return whether the measurement has succeeded
     */
    public boolean hasMeasurementSucceeded() {
        return this.measurementException == null;
    }

}
