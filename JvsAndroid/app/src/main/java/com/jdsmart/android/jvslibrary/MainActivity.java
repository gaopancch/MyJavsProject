package com.jdsmart.android.jvslibrary;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jdsmart.android.alexa.AlexaManager;
import com.jdsmart.android.jvslibrary.actions.ActionsFragment;
import com.jdsmart.android.jvslibrary.actions.BaseListenerFragment;

import static com.jdsmart.android.jvslibrary.R.id.frame;

/**
 * Our main launch activity where we can change settings, see about, etc.
 */
public class MainActivity extends BaseActivity implements ActionsFragment.ActionFragmentInterface, FragmentManager.OnBackStackChangedListener {
    private final static String TAG = "MainActivity";
    private final static String TAG_FRAGMENT = "CurrentFragment";

    private View statusBar;
    private TextView status;
    private View loading;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();

        statusBar = findViewById(R.id.status_bar);
        status = (TextView) findViewById(R.id.status);
        loading = findViewById(R.id.loading);

        ActionsFragment fragment = new ActionsFragment();
        loadFragment(fragment, false);
    }

    @Override
    protected void startListening() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
        if (fragment != null && fragment.isVisible()) {
            // add your code here
            if(fragment instanceof BaseListenerFragment){
                ((BaseListenerFragment) fragment).startListening();
            }
        }
    }

    @Override
    public void loadFragment(Fragment fragment, boolean addToBackStack){
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(frame, fragment, TAG_FRAGMENT);
        if(addToBackStack){
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }


    protected void stateListening(){

        if(status != null) {
            status.setText(R.string.status_listening);
            loading.setVisibility(View.GONE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateProcessing(){

        if(status != null) {
            status.setText(R.string.status_processing);
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateSpeaking(){

        if(status != null) {
            status.setText(R.string.status_speaking);
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void statePrompting(){

        if(status != null) {
            status.setText("");
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateFinished(){
        if(status != null) {
            status.setText("");
            loading.setVisibility(View.GONE);
            statusBar.animate().alpha(0);
        }
    }
    protected void stateNone(){
        statusBar.animate().alpha(0);
    }


    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only  if there are entries in the back stack
        boolean canback = (getSupportFragmentManager().getBackStackEntryCount() > 0);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(canback);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }
}
