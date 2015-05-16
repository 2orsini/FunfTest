package de.informatik.uni_hamburg.yildiri.funftest.tools;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import de.informatik.uni_hamburg.yildiri.funftest.utils.AsyncResponse;
import de.informatik.uni_hamburg.yildiri.funftest.utils.BandwidthResultRecord;

/**
 * Implementation of BandwidthMeasureTool using {@link java.net.HttpURLConnection}.
 * It performs an active measurement by opening an URLConnection of the supplied URL and trying to download the content at hand by reading the returned input stream.
 * Whilst downloading the file, some micro measurements occur at certain intervals.
 */
public class HttpURLConnectionMeasureTool extends BandwidthMeasureTool {

    /**
     * The HttpURLConnection used to download the data from the internet
     */
    private HttpURLConnection httpConn;
    /**
     * URL of the test file to be downloaded for the measurement
     */
    private URL testDownloadFileURL;
    /**
     * File object representing the test file
     */
    private File testDownloadFile;
    /**
     * The size of the test file in bytes
     */
    private int contentLength;
    /**
     * Estimation for when to update the progress, to be roughly in the desired intervals
     */
    private int xthPercentProgress;
    /**
     * Buffered input stream of the input stream returned by the HttpURLConnection to read the data
     */
    private BufferedInputStream bis;
    /**
     * Buffered output stream to write the read data to the file
     */
    private FileOutputStream fos;
    /**
     * Store for the results of the measurement
     */
    private BandwidthResultRecord bandwidthResultRecord;

    /**
     * Size in bytes for the cache between the input stream and output stream
     */
    private static final int BUFFER_SIZE = 1000;

    public HttpURLConnectionMeasureTool(AsyncResponse asyncResponse, Context context, String fileUrl, int connectionType) {
        super(asyncResponse, context, fileUrl, connectionType);
    }

    @Override
    public BandwidthResultRecord measureBandwidth() throws IOException {
        try {
            initEnvironmentComponents();

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Get some metadata of the content and set them in the bandwidth result record
                String contentType = httpConn.getContentType();
                contentLength = httpConn.getContentLength();
                bandwidthResultRecord.setFileURL(testDownloadFileURL);
                bandwidthResultRecord.setFileSize(contentLength);
                // Calculate some variables to be able to update the progress roughly in the desired intervals
                int blockNumEstimate = (int) Math.ceil(contentLength / BUFFER_SIZE);
                xthPercentProgress = (int) Math.ceil(blockNumEstimate * UPDATE_PROGRESS_EACH_X_PERCENT);

                actuallyConductMeasurement();
            } else {
                String badResponseMsg = "bad response from HTTP connection (response code: " + responseCode + ")";
                Log.e(getClass().getSimpleName(), "Measurement failed, " + badResponseMsg);
                throw new IOException(badResponseMsg);
            }
        } finally {
            if (fos != null) {
                fos.flush();
                fos.close();
            }
            if (bis != null) {
                bis.close();
            }
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }

        return bandwidthResultRecord;
    }

    /**
     * Initialize all the components and variables required for the measurement
     * @throws IOException if we fail to open a connection to the URL, fail to create the test file at the determined location or some other IOException occurs
     */
    private void initEnvironmentComponents() throws IOException{
        testDownloadFileURL = new URL(fileUrl);
        httpConn = (HttpURLConnection) testDownloadFileURL.openConnection();

        bis = null;
        fos = null;

        testDownloadFile = new File(context.getFilesDir() + "/tmp/testfile");
        if (testDownloadFile.exists()) {
            testDownloadFile.delete();
        }

        if (!testDownloadFile.exists()) {
            testDownloadFile.getParentFile().mkdir();
            testDownloadFile.createNewFile();
        }

        bandwidthResultRecord = new BandwidthResultRecord();
    }

    /**
     * Actually conduct the measurement and save all measurement results gotten into the bandwidth measurement record {@link de.informatik.uni_hamburg.yildiri.funftest.tools.HttpURLConnectionMeasureTool#bandwidthResultRecord}
     * @throws IOException if we fail to write the data to the file or some other IOException occurs
     */
    private void actuallyConductMeasurement() throws IOException{
        long blockEndTime;
        // Start timing
        startTime = System.currentTimeMillis();

        bis = new BufferedInputStream(httpConn.getInputStream());
        fos = new FileOutputStream(testDownloadFile.getPath());

        long totalBytesRead = 0; // bytes read overall in total
        int bytesRead; // bytes read into the buffer in the current passage of the while loop
        long currentBlockBytesRead = 0; // bytes read for the current block - might span over multiple passes of the while loop
        byte[] buf = new byte[BUFFER_SIZE]; // buffer to cache data between the input stream and output stream
        int currentBufferNum = 0;
        int currentBlockIndex = 0; // index of the current block in progress
        while ((bytesRead = bis.read(buf)) != -1) {
            totalBytesRead += bytesRead;
            currentBlockBytesRead += bytesRead;
            fos.write(buf, 0, bytesRead);

            // If we gotten to the roughly estimated point to update the progress, calculate the actual progress and publish it
            if (currentBufferNum % xthPercentProgress == 0) {
                int progressPercentage = (int) (((double) totalBytesRead / contentLength) * 100);
                Log.d(getClass().getSimpleName(), "Publishing current progress... progressPercentage: " + progressPercentage);
                publishProgress(progressPercentage);
            }

            // Check if we still have to perform partial measurements
            if(currentBlockIndex <= bandwidthResultRecord.TOTAL_BANDWIDTH_INDEX) {
                // Check if we finished working on the current block
                if (currentBlockBytesRead >= MEASURE_BLOCK_SIZE_IN_BYTES && currentBlockIndex <= bandwidthResultRecord.TOTAL_BANDWIDTH_INDEX) {
                    // Block has finished, stop timer of the block
                    blockEndTime = System.currentTimeMillis();
                    // Calculate the result for this block and set it in the bandwidth results record
                    double currentBlockBandwidth = BandwidthMeasureTool.calcDownloadrate(startTime, blockEndTime, totalBytesRead);
                    bandwidthResultRecord.setBandwidthMeasure(currentBlockIndex, currentBlockBandwidth);
                    Log.d(getClass().getSimpleName(), "Bandwidth measure finished on block-index " + currentBlockIndex + "  currently totalDownloadedBytes = " + totalBytesRead + "  blockEndTime = " + blockEndTime + "  diffTime = " + ((blockEndTime - startTime) / 1000.0) + "s  bytesRead = " + bytesRead + "  currentBlockTotalRead = " + currentBlockBytesRead + "  currentBlockBandwidth = " + currentBlockBandwidth);

                    // (re)set all block related variables
                    currentBlockIndex++;
                    currentBlockBytesRead = 0;
                }
            }
            currentBufferNum++;
        }

        // End timing
        endTime = System.currentTimeMillis();

        // Calculate the overall total bandwidth and set it in the bandwidth result record
        double totalDownloadRate = calcDownloadrate(startTime, endTime, testDownloadFile.length());
        bandwidthResultRecord.setBandwidthMeasure(bandwidthResultRecord.TOTAL_BANDWIDTH_INDEX, totalDownloadRate);

        Log.d(getClass().getSimpleName(), String.format("Download and measure finished. startTime = %d , endTime = %d , diffTime = %f s, file.length = %d , httpConn.getContentLength() = %d , downloadRate = %f kbit/s", startTime, endTime, ((endTime - startTime) / 1000.0), testDownloadFile.length(), contentLength, totalDownloadRate));
    }
}
