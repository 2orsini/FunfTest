package de.informatik.uni_hamburg.yildiri.funftest;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import de.informatik.uni_hamburg.yildiri.funftest.customProbe.BandwidthProbe;
import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.BatteryProbe;
import edu.mit.media.funf.probe.builtin.CellTowerProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.probe.Probe.DataListener;

public class MainActivity extends ActionBarActivity implements DataListener {

    /**
     * The name of the used pipeline, as it is denoted in the string resources
     */
    public static final String PIPELINE_NAME = "default";
    /**
     * SQL query to get the count of entries in the pipeline database
     */
    private static final String TOTAL_COUNT_SQL = "select count(*) from " + NameValueDatabaseHelper.DATA_TABLE.name;

    // UI-Objects
    private TextView archivePathAndCountView;
    private CheckBox enabledCheckbox;
    private Button archiveButton;
    private Button scanNowButton;
    private TextView dataCountView;
    private ScrollView receivedDataScroller;
    private TextView receivedDataView;

    // Probe objects (Battery, Mobile Network Info, Simple Location, Nearby Wifi Devices, Nearby Cellular Towers, Bandwidth measure, (Running Applications), (Screen On/Off))
    /**
     * A list of all our probes to have a comfortable overview of them and be able to do iterative processing when needed
     */
    private List<Probe> probes = new ArrayList<Probe>();
    private WifiProbe wifiProbe;
    private CellTowerProbe cellTowerProbe;
    private SimpleLocationProbe locationProbe;
    private BatteryProbe batteryProbe;
    private BandwidthProbe bandwidthProbe;

    private FunfManager funfManager;
    /**
     * The pipeline that will hold the data and periodically archive to an SQLite file.
     * The configuration we defined in the string resources, created a BasicPipeline which will run to the registered probes on the default schedule.
     */
    private BasicPipeline pipeline;
    private Handler handler;
    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            funfManager = ((FunfManager.LocalBinder) service).getManager();

            Gson gson = funfManager.getGson();
            // Get probes from JSON
            wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
            cellTowerProbe = gson.fromJson(new JsonObject(), CellTowerProbe.class);
            locationProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
            batteryProbe = gson.fromJson(new JsonObject(), BatteryProbe.class);
            bandwidthProbe = gson.fromJson(new JsonObject(), BandwidthProbe.class);

            // Add all the probes to our probe list
            probes.add(wifiProbe);
            probes.add(cellTowerProbe);
            probes.add(locationProbe);
            probes.add(batteryProbe);
            probes.add(bandwidthProbe);

            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

            // Register this data listener (this class) as a passive listener by default
            // This way the probes will be run automatically according to their default schedules respectively
            wifiProbe.registerPassiveListener(MainActivity.this);
            cellTowerProbe.registerPassiveListener(MainActivity.this);
            locationProbe.registerPassiveListener(MainActivity.this);
            batteryProbe.registerPassiveListener(MainActivity.this);
            bandwidthProbe.registerPassiveListener(MainActivity.this);

            enabledCheckbox.setChecked(pipeline.isEnabled());
            enabledCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (funfManager != null) {
                        if (isChecked) {
                            funfManager.enablePipeline(PIPELINE_NAME);
                            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
                        } else {
                            funfManager.disablePipeline(PIPELINE_NAME);
                        }
                    }
                }
            });

            // Set UI ready to use by enabling the buttons and refresh some values
            updateScanCount();
            updateArchiveDatabasesCount(pipeline);
            enabledCheckbox.setEnabled(true);
            archiveButton.setEnabled(true);
            scanNowButton.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            funfManager = null;
            Log.d(getString(R.string.app_name), "onServiceDisconnected() of the service (funfManagerConn). ComponentName is " + name.flattenToString());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Workaround for SDK version 15 and 16
        asyncTaskWorkaround();

        // Read and load-in the default values for the app preferences if needed (pretty much only when running the app for the very first time)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        initializeUI();

        // Bind the app to the service (also create the connection with FunfManager)
        bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
        Log.d(getString(R.string.app_name), "onCreate(): bound the service (funfManagerConn)");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(getString(R.string.app_name), "onDestory() called");

        // Unbind the service from the app (otherwise it would cause memory leaks)
        // This doesn't stop the service, so the probes will still be run according to their schedules
        if (funfManagerConn != null) {
            unbindService(funfManagerConn);
            Log.d(getString(R.string.app_name), "onDestory(): unbound the service (funfManagerConn)");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(getString(R.string.app_name), "onResume() called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(getString(R.string.app_name), "onPause() called");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Open the Settings activity if we selected the settings item in the menu
        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClassName(this, SettingsActivity.class.getCanonicalName());
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Perform some workaround, that is needed for normal usage of AsyncTask for devices with SDK version 15 and 16.
     * Without this workaround, tests at SDK version 15 always threw an "sending message to a handler on a dead thread asynctask" error and wouldn't reach the onPostExectue method.
     * For more information about this problem see: https://stackoverflow.com/questions/4280330/onpostexecute-not-being-called-in-asynctask-handler-runtime-exception
     */
    private void asyncTaskWorkaround() {
        Looper looper = Looper.getMainLooper();
        Handler handler = new Handler(looper);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Class.forName("android.os.AsyncTask");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Initialize and setup some UI elements
     */
    private void initializeUI() {
        // Handler used to make interface changes on the main thread
        handler = new Handler();

        archivePathAndCountView = (TextView) findViewById(R.id.archivePathAndCount);

        dataCountView = (TextView) findViewById(R.id.dataCountText);
        dataCountView.setVisibility(View.INVISIBLE);
        dataCountView.setEnabled(false);

        receivedDataScroller = (ScrollView) findViewById(R.id.receivedDataScroller);
        receivedDataView = (TextView) findViewById(R.id.receivedDataText);
        receivedDataView.setEnabled(true);

        enabledCheckbox = (CheckBox) findViewById(R.id.enabledCheckbox);
        enabledCheckbox.setEnabled(false);

        archiveButton = (Button) findViewById(R.id.archiveButton);
        archiveButton.setEnabled(false);
        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                archiveData();
            }
        });

        scanNowButton = (Button) findViewById(R.id.scanNowButton);
        scanNowButton.setEnabled(false);
        scanNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateImmediateScan();
            }
        });
    }

    /**
     * Manually trigger and force the archiving of the pipeline data to the foreseen archive destination.
     * Notice: Archiving also occurs automatically in certain time intervals (that interval can be configured in the JSON-configuration of the pipeline, which is located in the string resources)
     */
    private void archiveData() {
        // Try to archive the data of the pipeline
        if (pipeline.isEnabled()) {
            pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);

            // Wait 1 second for archive to finish, then refresh the UI
            // Notice: this is kind of a hacky approach
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), getString(R.string.archive_success), Toast.LENGTH_SHORT).show();
                    updateScanCount();
                }
            }, 1000L);
            updateArchiveDatabasesCount(pipeline);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), getString(R.string.archive_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Forces the probes to be run immediately (just once). This does not seem to alter or rearrange the usual probe schedule.
     */
    private void initiateImmediateScan() {
        if (pipeline.isEnabled()) {
            // Register the pipeline on the probes as a non-passive listener to run them manually once
            for (Probe probe : probes) {
                probe.registerListener(pipeline);
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), getString(R.string.scan_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Queries the count of data entries in the pipeline database and refreshes the UI (specifically the dataCountView) accordingly
     */
    private void updateScanCount() {
        // Query the pipeline db for the count of rows in the data table
        SQLiteDatabase db = pipeline.getDb();
        Cursor cursor = db.rawQuery(TOTAL_COUNT_SQL, null);
        cursor.moveToFirst();
        final int count = cursor.getInt(0);
        Log.d(getString(R.string.app_name), "updateScanCount(): Querying count from pipeline database at path: " + db.getPath());
        Log.d(getString(R.string.app_name), "updateScanCount(): Data count: " + count + " , cursor content dump: " + DatabaseUtils.dumpCursorToString(cursor));
        cursor.close();
        // Update the interface via the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataCountView.setEnabled(true);
                dataCountView.setVisibility(View.VISIBLE);
                dataCountView.setText(getString(R.string.data_count, count));
            }
        });
    }

    /**
     * Get the number of database files in the archive directory and refresh the UI (specifically the archivePathAndCountView) accordingly
     *
     * @param pipeline the pipeline being used (is needed in order to get the FileArchive object)
     */
    private void updateArchiveDatabasesCount(BasicPipeline pipeline) {
        final String dbPath = Environment.getExternalStorageDirectory() + "/" + getPackageName() + "/default/archive/";
        final int dbCount = pipeline.getArchive().getAll().length;         // previously (counting files in the dir): final int dbCount = new File(dbPath).listFiles().length;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                archivePathAndCountView.setText(getString(R.string.archive_path_and_count, dbPath, dbCount));
            }
        });
    }

    /**
     * Callback method when probes emit data. We use this to forward the data to the UI to display the received data in the TextView 'receivedDataView'
     * Important notice: This callback does seem to pick-up the data of manually run probes only, i.e probes which were registered by a reigsterListener(DataListener) call (e.g when the scan now button is clicked). This means that the data, that is being gathered by "normal" passive/scheduled probes is not getting intercepted and consequently not displayed.
     *
     * @param probeConfig configuration and parameters of the probe
     * @param data        data that was recorded/gathered by the probe
     */
    @Override
    public void onDataReceived(final IJsonObject probeConfig, IJsonObject data) {
        Log.d(getString(R.string.app_name), "Now in onDataReceived()... ProbeConfig: " + probeConfig.toString() + "  data: " + data.toString());
        if (!data.isJsonNull()) {
            final String dataString = data.toString();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    receivedDataView.setText(receivedDataView.getText() + "-*-  ProbeConfig  " + probeConfig.toString() + "\nData:  " + dataString + "  -+-\n");
                }
            });
        } else {
            receivedDataView.setText(receivedDataView.getText() + "-*-  failed receive data (isJsonNull) from the probe:  " + probeConfig.toString() + "  -+-\n");
        }

        // Focus the most recent messages by scrolling the ScrollView 'receivedDataScroller', in which the refreshed TextView is located in, to the very bottom
        // Fix for sometimes not scrolling all the way to the bottom: Delay the scroll for approx 200-250ms, so that the last element still can be registered and so the view scrolls all the way to the bottom (instead of only to the second last); source: https://stackoverflow.com/questions/14801215/scrollview-not-scrolling-down-completely
        receivedDataScroller.postDelayed(new Runnable() {
            @Override
            public void run() {
                receivedDataScroller.fullScroll(View.FOCUS_DOWN);
            }
        }, 200);
    }

    /**
     * Callback method when a probe finishes sending a stream of data. We use this to update the count of the database entries and also re-register the probes to continue their service
     *
     * @param probeConfig configuration and parameters of the probe
     * @param checkpoint  checkpoint of stream progress of the probe. Only sent if the probe is a continuable probe
     */
    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        String checkpointString = "null";
        if (checkpoint != null) {
            if (!checkpoint.isJsonNull()) {
                checkpointString = checkpoint.getAsString();
            }
        }
        Log.d(getString(R.string.app_name), "Now in onDataCompleted()... ProbeConfig: " + probeConfig.toString() + "  checkpoint: " + checkpointString);
        updateScanCount();
        // Re-register to keep listening after probe completes
        wifiProbe.registerPassiveListener(this);
        cellTowerProbe.registerPassiveListener(this);
        locationProbe.registerPassiveListener(this);
        batteryProbe.registerPassiveListener(this);
        bandwidthProbe.registerPassiveListener(this);
    }
}