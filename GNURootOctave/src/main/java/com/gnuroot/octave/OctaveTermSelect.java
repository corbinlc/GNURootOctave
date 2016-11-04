package com.gnuroot.octave;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class OctaveTermSelect extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent("com.gnuroot.octave.LAUNCH_CHOICE");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("launchType", "launchTerm");
        startActivity(intent);
        finish();
    }
}
