package com.company.exoplayer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends Fragment {
	private TextView usernameTextView;
	private TextView passwordTextView;
	private Button updateButton;
	private LoginHandler loginHandler;
	public SettingsFragment() {

		// Required empty public constructor
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		loginHandler = new LoginHandler();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_settings, container, false);

		usernameTextView = (EditText) view.findViewById(R.id.usernameTextView);
		passwordTextView = (EditText) view.findViewById(R.id.passwordTextView);
		updateButton = view.findViewById(R.id.updateButton);


		SharedPreference sharedPreference = new SharedPreference();
		String username = sharedPreference.getUser(getActivity());
		String password = sharedPreference.getPassword(getActivity());


		usernameTextView.setText(username);
		passwordTextView.setText(password);

		updateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				String username = usernameTextView.getText().toString().trim();
				String password = passwordTextView.getText().toString().trim();

				loginHandler.saveUser(getActivity(), username, password);
			}
		});

		return view;
	}
}
