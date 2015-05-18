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

import java.util.ArrayList;

import com.gnuroot.library.GNURootCoreActivity;
import com.gnuroot.octave.R;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class OctaveInstallFragment extends ListFragment {

	ListView listView;
	ArrayAdapter<String> adapter;
	Button button;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View fragmentView = inflater.inflate(R.layout.fragment_install, container, false);
		
		if (getActivity() != null) {
			listView = (ListView)fragmentView.findViewById(android.R.id.list);
			String[] packageNames = getResources().getStringArray(R.array.package_disp_array);
	        adapter = new ArrayAdapter<String>(inflater.getContext(),android.R.layout.simple_list_item_multiple_choice, packageNames);
	        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	        listView.setAdapter(adapter);
	        button = (Button) fragmentView.findViewById(R.id.install_button);
	        button.setOnClickListener(new OnClickListener()
	        {
	            @Override
	            public void onClick(View view)
	            {
	            	ArrayList<String> packageNameArrayList = new ArrayList<String>();
	            	SparseBooleanArray checked = listView.getCheckedItemPositions();
	            	String[] packageNames = getResources().getStringArray(R.array.package_name_array);
	            	packageNameArrayList.add("libgl1-mesa-swx11");
	            	packageNameArrayList.add("octave");
	            	packageNameArrayList.add("less");
	            	packageNameArrayList.add("fonts-liberation");
	            	for (int i = 0; i < checked.size(); i++) {
	            	    if(checked.valueAt(i) == true) {
	            	        packageNameArrayList.add(packageNames[checked.keyAt(i)]);
	            	    }
	            	}
	                ((OctaveMain)getActivity()).installOctave(packageNameArrayList);
	            }
	        });
		}
	
		return fragmentView;
	}
	
}
