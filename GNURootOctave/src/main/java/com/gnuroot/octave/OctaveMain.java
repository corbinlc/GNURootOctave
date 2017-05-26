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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.gnuroot.octave.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;

public class OctaveMain extends Activity {

	// GNURoot won't launch unless its version matches at least this.
	String GNURootVersion = "76";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent;
		String launchType = "launchTerm";
		PackageInfo packageInfo = null;

		try { packageInfo = getPackageManager().getPackageInfo("com.gnuroot.debian", 0); }
		catch (NameNotFoundException e) { showUpdateError(); }

		if(packageInfo == null || packageInfo.versionCode < Integer.parseInt(GNURootVersion))
			showUpdateError();

		/** To get the reactive app selection interface to appear, send an intent
		 *	to both OctaveTermSelect and OctaveXTermSelect. They send an intent
		 *	back to this class indicating the selection that was made.
		 */
		else {
			if(getIntent().getAction() == Intent.ACTION_MAIN) {
                copyAssets("com.gnuroot.octave");
				Intent getLaunch = new Intent("com.gnuroot.octave.LAUNCH_SELECTION");
				getLaunch.addCategory(Intent.CATEGORY_DEFAULT);
				startActivity(getLaunch);
				finish();
			}
			else if(getIntent().getAction() == "com.gnuroot.octave.LAUNCH_CHOICE") {
				launchType = getIntent().getStringExtra("launchType");
				intent = getLaunchIntent();
				intent.putExtra("launchType", launchType);
				startActivity(intent);
				finish();
			}

			// On the odd chance that Octave catches an intent it isn't meant to, display an error.
			else
				showIntentError();
		}
	}

	/**
	 * Displays an alert dialog if GNURoot Debian isn't updated to at least the version that included the
	 * new ecosystem changes. Sends the user to the market page for it if not.
	 */
	private void showUpdateError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setMessage(R.string.update_error_message);
		builder.setPositiveButton(R.string.button_affirmative, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=com.gnuroot.debian"));
				startActivity(intent);
				finish();
			}
		});
		builder.create().show();
	}

	private void showIntentError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setMessage(R.string.intent_error_message);
		builder.setPositiveButton(R.string.button_affirmative, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		});
		builder.create().show();
	}

	/**
	 * GNURoot Debian expects the following extras from install intents:
	 * 	1. launchType: This can be either launchTerm or launchXTerm. The command that is to be run after installation
	 * 		dictates this selection.
	 *	2. command: This is the command that will be executed in proot after installation. Often, this will be a
	 *		script stored in your custom tar file to install additional packages if needed or to execute the extension.
	 *	3. customTar: This is the custom tar file you've created for your extension.
	 * @return
	 */
	private Intent getLaunchIntent() {
		String command;
		Intent installIntent = new Intent("com.gnuroot.debian.LAUNCH");
		installIntent.setComponent(new ComponentName("com.gnuroot.debian", "com.gnuroot.debian.GNURootMain"));
		installIntent.addCategory(Intent.CATEGORY_DEFAULT);
		command =
			"#!/bin/bash\n" +
			"if [ ! -f /support/.octave_packages_passed ]; then\n" +
            "  sudo /support/installPackages octave_packages libgl1-mesa-swx11 octave less fonts-liberation gnuplot-nox octave-control octave-financial octave-io octave-missing-functions octave-odepkg octave-optim octave-signal octave-specfun octave-statistics octave-symbolic octave-image\n" +
			"fi\n" +
			"if [ -f /support/.octave_packages_passed ]; then\n" +
			"  if [ ! -f /support/.octave_custom_passed ] || [ ! -f /support/.octave_update_passed ]; then\n" +
            "    sudo /support/untargz octave_custom /support/octave_custom.tar.gz\n" +
			"    sudo chmod -R 755 /usr/share/octave\n" +
			"    if [ $? == 0 ]; then\n" +
			"      touch /support/.octave_update_passed\n" +
			"    fi\n" +
			"  fi\n" +
			"  if [ -f /support/.octave_custom_passed ]; then\n" +
			"    /usr/bin/octave\n" +
			"  fi\n" +
            "fi\n";
		installIntent.putExtra("command", command);
		installIntent.putExtra("GNURootVersion", GNURootVersion);
		installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		installIntent.setData(getTarUri());
		return installIntent;
	}

	/**
	 * Returns a Uri for the custom tar placed in the project's assets directory.
	 * @return
	 */
	private Uri getTarUri() {
		File fileHandle = new File(getFilesDir() + "/octave_custom.tar.gz");
		return FileProvider.getUriForFile(this, "com.gnuroot.octave.fileprovider", fileHandle);
	}

	/**
	 * Renames assets from .mp3 to .tar.gz.
	 * @param packageName
	 */
	private void copyAssets(String packageName) {
		Context friendContext = null;
		try {
			friendContext = this.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
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
		for (String filename : files) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = assetManager.open(filename);
				filename = filename.replace(".mp3", ".tar.gz");
				out = openFileOutput(filename, MODE_PRIVATE);
				copyFile(in, out);
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch (IOException e) {
				Log.e("tag", "Failed to copy asset file: " + filename, e);
			}
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}
}