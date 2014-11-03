package com.wifindus.meshtester.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.R;
import com.wifindus.meshtester.Static;

public class UserFragment extends BaseFragment  {
    private TextView id, name, since;
    private Button signInOutButton;
    private static final String TAG = UserFragment.class.getName();
    private Handler timerHandler = new Handler();

    public UserFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_user, container, false);
        id = (TextView)view.findViewById(R.id.field_user_id);
        since = (TextView)view.findViewById(R.id.field_user_since);
        name = (TextView)view.findViewById(R.id.field_user_name);
        signInOutButton = (Button)view.findViewById(R.id.user_change_button);
        signInOutButton.setOnClickListener(signInClickListener);
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        update();
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void update()
    {
        int userID = MeshApplication.getUserID();
        id.setText(userID >= 0 ? Integer.toString(MeshApplication.getUserID()) : getResources().getString(R.string.user_no_id));
        name.setText(userID >= 0 ? MeshApplication.getUserName() : "");
        signInOutButton.setText(userID >= 0 ? R.string.user_logout : R.string.user_login);
        updateSignedInTime();
    }

    private View.OnClickListener signInClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view)
        {
            if (view != signInOutButton)
                return;

            if (MeshApplication.getUserID() >= 0)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(UserFragment.this.getActivity());
                builder.setMessage(R.string.user_really_logout)
                    .setPositiveButton(android.R.string.yes,  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int i) {
                            MeshApplication.updateUser(UserFragment.this.getActivity(),-1);
                            update();
                            Toast.makeText(UserFragment.this.getActivity(),
                                getResources().getString(R.string.user_logout_ok_toast),
                                Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            }
            else //signing in
            {
                //create text edit box
                final EditText text = new EditText(UserFragment.this.getActivity());
                text.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
                text.setFilters(new InputFilter[] {new InputFilter.LengthFilter(8)});

                //build the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(UserFragment.this.getActivity());
                builder.setTitle(R.string.user_login)
                    .setMessage(R.string.user_id_help_text)
                    .setView(text)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int i) {

                            int userID = -1;
                            try {
                                userID = Integer.parseInt(text.getText().toString().trim());
                            } catch (NumberFormatException ex) {
                            }
                            MeshApplication.updateUser(UserFragment.this.getActivity(), userID);
                            update();
                            if (userID > -1)
                                Toast.makeText(UserFragment.this.getActivity(),
                                        getResources().getString(R.string.user_login_ok_toast, userID),
                                        Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            }
        }
    };
    @Override
    public String logTag(){
        return TAG;
    }

    private void updateSignedInTime()
    {
        if (MeshApplication.getUserID() >= 0)
        {
            long time = System.currentTimeMillis();
            since.setText(
                    Static.formatTimer(time - MeshApplication.getUserSignedInSince()) + " ago");
        }
        else
        {
            since.setText("");
        }
    }

    private Runnable timerRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            updateSignedInTime();
            timerHandler.postDelayed(this, 1000);
        }
    };
}
