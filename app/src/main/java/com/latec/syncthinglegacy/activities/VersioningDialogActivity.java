package com.latec.syncthinglegacy.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.latec.syncthinglegacy.R;
import com.latec.syncthinglegacy.databinding.ActivityVersioningDialogBinding;
import com.latec.syncthinglegacy.fragments.dialog.ExternalVersioningFragment;
import com.latec.syncthinglegacy.fragments.dialog.NoVersioningFragment;
import com.latec.syncthinglegacy.fragments.dialog.SimpleVersioningFragment;
import com.latec.syncthinglegacy.fragments.dialog.StaggeredVersioningFragment;
import com.latec.syncthinglegacy.fragments.dialog.TrashCanVersioningFragment;

import java.util.Arrays;
import java.util.List;

public class VersioningDialogActivity extends ThemedAppCompatActivity {

    private Fragment mCurrentFragment;

    private static final List<String> mTypes = Arrays.asList("none", "trashcan", "simple", "staggered", "external");

    private Bundle mArguments;

    private ActivityVersioningDialogBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVersioningDialogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            mArguments = savedInstanceState.getBundle("arguments");
        } else {
            mArguments = getIntent().getExtras();
        }

        updateFragmentView(mTypes.indexOf(getIntent().getExtras().getString("type")));
        initiateFinishBtn();
        initiateSpinner();
    }

    private void initiateFinishBtn() {
        binding.finishBtn.setOnClickListener(v -> {
            saveConfiguration();
            finish();
        });
    }

    private void saveConfiguration() {
        Intent intent = new Intent();
        intent.putExtras(mCurrentFragment.getArguments());
        setResult(Activity.RESULT_OK, intent);
    }

    private void initiateSpinner() {
        binding.versioningTypeSpinner.setSelection(mTypes.indexOf(getIntent().getExtras().getString("type")));
        binding.versioningTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mTypes.indexOf(getIntent().getExtras().getString("type"))) {
                    updateVersioningType(position);
                    updateFragmentView(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void updateVersioningType(int position) {
        mArguments.putString("type", mTypes.get(position));
    }

    private void updateFragmentView(int selection) {
        if (mCurrentFragment != null){
            mArguments = mCurrentFragment.getArguments();
        }

        mCurrentFragment = getFragment(selection);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        //This Activtiy (VersioningDialogActivity) contains all the file versioning parameters that have been passed from the FolderActivity in the intent extras, so we simply
        // pass that to the currentfragment.
        mCurrentFragment.setArguments(mArguments);
        transaction.replace(R.id.versioningFragmentContainer, mCurrentFragment);
        transaction.commit();
    }

    private Fragment getFragment(int selection) {
        Fragment fragment = null;

        switch (selection) {
            case 0:
                fragment = new NoVersioningFragment();
                break;
            case 1:
                fragment = new TrashCanVersioningFragment();
                break;
            case 2:
                fragment = new SimpleVersioningFragment();
                break;
            case 3:
                fragment = new StaggeredVersioningFragment();
                break;
            case 4:
                fragment = new ExternalVersioningFragment();
                break;
        }

        return fragment;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("arguments", mCurrentFragment.getArguments());
    }

    @Override
    public void onBackPressed() {
        saveConfiguration();
        super.onBackPressed();
    }
}
