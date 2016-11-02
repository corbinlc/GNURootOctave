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

import com.gnuroot.octave.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent;
		SharedPreferences prefs = getSharedPreferences("MAIN", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		PackageInfo packageInfo = null;
		int version = 0;

		try { packageInfo = getPackageManager().getPackageInfo("com.gnuroot.debian", 0); }
		catch (NameNotFoundException e) { showUpdateError(); }

		if(packageInfo == null || packageInfo.versionCode < 40)
			showUpdateError();

		else {
			if (!prefs.getBoolean("firstTime", false)) {
				copyAssets("com.gnuroot.octave");
				intent = getInstallIntent();
				editor.putBoolean("firstTime", true);
				editor.commit();
			} else
				intent = getLaunchIntent();

			//TODO Would like to replace this Alert Dialog with the app selection interface
			AlertDialog.Builder builder = new AlertDialog.Builder(OctaveMain.this);
			builder.setMessage(R.string.launch_preference);
			builder.setPositiveButton(R.string.term_preference, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					intent.putExtra("launchType", "launchTerm");
					startActivity(intent);
					dialog.cancel();
					finish();
				}
			});
			builder.setNegativeButton(R.string.xterm_preference, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					intent.putExtra("launchType", "launchXTerm");
					startActivity(intent);
					dialog.cancel();
					finish();
				}
			});
			builder.create().show();
		}
	}

	private void showUpdateError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Octave is attempting to launch GNURoot Debian. It is either missing or out of date. Please update it and try again.");
		builder.setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		});
		builder.create().show();
	}

	private Intent getInstallIntent() {
		Intent installIntent = new Intent("com.gnuroot.debian.LAUNCH");
		installIntent.setComponent(new ComponentName("com.gnuroot.debian", "com.gnuroot.debian.GNURootMain"));
		installIntent.addCategory(Intent.CATEGORY_DEFAULT);
		installIntent.putExtra("statusFile", "octave_custom");
		installIntent.putExtra("command", "/support/octave_install_packages.sh");
		installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		installIntent.setData(getTarUri());
		return installIntent;
	}

	private Intent getLaunchIntent() {
		Intent launchIntent = new Intent("com.gnuroot.debian.LAUNCH");
		launchIntent.setComponent(new ComponentName("com.gnuroot.debian", "com.gnuroot.debian.GNURootMain"));
		launchIntent.addCategory(Intent.CATEGORY_DEFAULT);
		launchIntent.putExtra("command", "/usr/bin/octave");
		return launchIntent;
	}

	private Uri getTarUri() {
		File fileHandle = new File(getFilesDir() + "/octave_custom.tar.gz");
		return FileProvider.getUriForFile(this, "com.gnuroot.octave.fileprovider", fileHandle);
	}

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