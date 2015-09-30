/*
Copyright (c) 2014 Corbin Leigh Champion

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

/* Author(s): Corbin Leigh Champion */

package com.gnuroot.octave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.gnuroot.library.GNURootCoreActivity;
import com.gnuroot.octave.R;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.Toast;

public class OctaveMain extends GNURootCoreActivity {

	private ViewPager viewPager;
	private OctaveTabsPagerAdapter mAdapter; 
	private ActionBar actionBar;
	// Tab titles
	private String[] tabs = { "Install/Update", "Launch" };
	private boolean installingPackages = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initilization
		viewPager = (ViewPager) findViewById(R.id.pager);
		actionBar = getSupportActionBar();
		mAdapter = new OctaveTabsPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(mAdapter);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);		

		// Adding Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name)
					.setTabListener(this));
		}

		/**
		 * on swiping the viewpager make respective tab selected
		 * */
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// on changing the page
				// make respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

        onNewIntent(getIntent());
	}

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals("com.gnuroot.debian.NEW_WINDOW"))
            launchTerm();
        else if (action.equals("com.gnuroot.debian.NEW_XWINDOW"))
            launchXTerm();
    }

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// on tab selected
		// show respected fragment view
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	public void installOctave(ArrayList<String> packageNameArrayList) {
		//check whether GNURoot is setup - TODO keep track of what has been succeeded in the past
		//install packages
		installingPackages = true;
		ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
    	prerequisitesArrayList.add("gnuroot_rootfs");
		installPackages(packageNameArrayList, "octave_packages", prerequisitesArrayList);
		//install custom packages on return
	}	


	private void copyAssets(String packageName) {
		Context friendContext = null;
		try {
			friendContext = this.createPackageContext(packageName,Context.CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e1) {
			return;
		}
		AssetManager assetManager = friendContext.getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			Log.e("tag", "Failed to get asset file list.", e);
		}
		for(String filename : files) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = assetManager.open(filename);
				filename = filename.replace(".mp3", ".tar.gz");
				out = openFileOutput(filename,MODE_PRIVATE);
				copyFile(in, out);
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch(IOException e) {
				Log.e("tag", "Failed to copy asset file: " + filename, e);
			}       
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}
	
	@Override
	//override this with what you want to happen when the GNURoot Debian service completes a task
	public void nextStep(Intent intent) {
		super.nextStep(intent);
		if (intent.getStringExtra("packageName").equals(getPackageName())) {
			int resultCode = intent.getIntExtra("resultCode",0);
			int requestCode = intent.getIntExtra("requestCode",0);

			if (resultCode == MISSING_PREREQ) {
				if (intent.getStringExtra("missingPreq").equals("octave_packages"))
					this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(), "Octave packages have not been installed, please click install/update from GNURoot Octave.", Toast.LENGTH_LONG).show();
						}
					});
				if (intent.getStringExtra("missingPreq").equals("octave_custom"))
					this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(), "Octave custom files have not been installed, please click install/update from GNURoot Octave.", Toast.LENGTH_LONG).show();
						}
					});
			}
			
			if ((installingPackages == true) && (requestCode == CHECK_STATUS) && (resultCode != STATUS_FILE_NOT_FOUND)) {
				//install custom tar file
				installingPackages = false;
				copyAssets("com.gnuroot.octave");
				File fileHandle = new File(getFilesDir() + "/octave_custom.tar.gz");
				ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
		    	prerequisitesArrayList.add("gnuroot_rootfs");
		    	prerequisitesArrayList.add("octave_packages");
				installTar(FileProvider.getUriForFile(this, "com.gnuroot.octave.fileprovider", fileHandle), "octave_custom", prerequisitesArrayList);
			} else if (((requestCode == CHECK_STATUS) && (resultCode != STATUS_FILE_NOT_FOUND) && (installingPackages == false)) || (requestCode == RUN_SCRIPT))  {
				Thread thread = new Thread() {
					@Override
					public void run() {
						// Block this thread for 1 second. There is a race case if the progressDialog is dismissed too quickly
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
						// After sleep finished blocking, create a Runnable to run on the UI Thread.
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressDialog.dismiss();
							}
						});

					}

				};
				thread.start();
			}
		}

	}

	public void launchTerm() {
		ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
		prerequisitesArrayList.add("gnuroot_rootfs");
		prerequisitesArrayList.add("octave_packages");
		prerequisitesArrayList.add("octave_custom");
        runCommand("/usr/bin/octave", prerequisitesArrayList);
	}

    public void launchXTerm() {
        ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
        prerequisitesArrayList.add("gnuroot_rootfs");
        prerequisitesArrayList.add("gnuroot_x_support");
        prerequisitesArrayList.add("octave_packages");
        prerequisitesArrayList.add("octave_custom");
        //runXCommand("/usr/bin/octave --force-gui", prerequisitesArrayList);
        runXCommand("/usr/bin/octave", prerequisitesArrayList);
    }

}