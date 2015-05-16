package de.informatik.uni_hamburg.yildiri.funftest;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

/**
 * Activity that displays the settings fragments
 */
public class SettingsActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable up action for the actionbar to be able to go up to the MainActivity
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Display the fragment as the main content
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
