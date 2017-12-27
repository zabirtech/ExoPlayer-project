package com.company.exoplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.util.Util;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

	private CharSequence mTitle;
	private ListView mDrawerList;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mDrawerLayout = findViewById(R.id.drawer_layout);
		mDrawerList = findViewById(R.id.left_drawer);

		// Set the adapter for the list view
		mDrawerList.setAdapter(new ArrayAdapter<String>(
				getSupportActionBar().getThemedContext(),
				android.R.layout.simple_list_item_activated_1,
				android.R.id.text1,
				new String[]{
						"Samples",
						"Settings",
						"Open by guid",
				}));



		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerLayout.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				mDrawerLayout.openDrawer(GravityCompat.START);


			}

		});

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
												  R.string.navigation_drawer_open,
												  R.string.navigation_drawer_close);

		mDrawerLayout.addDrawerListener(mDrawerToggle);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_banner);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		selectItem(0);

		mDrawerToggle.syncState();


	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == android.R.id.home) {
			if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
				mDrawerLayout.closeDrawer(GravityCompat.START);
			} else {
				mDrawerLayout.openDrawer(GravityCompat.START);
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		// Create a new fragment and specify the planet to show based on position
		Fragment fragment;
		switch (position){
			case 0:
				fragment = new SampleChooserFragment();
				break;
			default:
				fragment = new SettingsFragment();
				break;
			case 2:
				fragment = new OpenByGuidFragment();
				break;

		}

		FragmentManager fragmentManager = getSupportFragmentManager();
		// update the main content by replacing fragments
		fragmentManager.beginTransaction()
				.replace(R.id.container, fragment)
				.commit();
		// Highlight the selected item, update the title, and close the drawer
		mDrawerList.setItemChecked(position, true);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	public static UUID getDrmUuid(String typeString) throws ParserException {
		switch (Util.toLowerInvariant(typeString)) {
			case "widevine":
			case "widevine-custom":
				return C.WIDEVINE_UUID;
			case "playready":

				return C.PLAYREADY_UUID;
			case "cenc":
				return C.CLEARKEY_UUID;
			default:
				try {
					return UUID.fromString(typeString);
				}
				catch (RuntimeException e) {
					throw new ParserException("Unsupported drm type: " + typeString);
				}
		}
	}
}
