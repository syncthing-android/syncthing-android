package com.latec.syncthinglegacy.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.latec.syncthinglegacy.databinding.ActivityPullorderDialogBinding;
import java.util.Arrays;
import java.util.List;

public class PullOrderDialogActivity extends ThemedAppCompatActivity {

    public static final String EXTRA_PULL_ORDER = "com.latec.syncthinglegacy.activities.PullOrderDialogActivity.PULL_ORDER";
    public static final String EXTRA_RESULT_PULL_ORDER = "com.latec.syncthinglegacy.activities.PullOrderDialogActivity.EXTRA_RESULT_PULL_ORDER";

    private String selectedType;

    private ActivityPullorderDialogBinding binding;

    private static final List<String> mTypes = Arrays.asList(
        "random",
        "alphabetic",
        "smallestFirst",
        "largestFirst",
        "oldestFirst",
        "newestFirst"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPullorderDialogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState == null) {
            selectedType = getIntent().getStringExtra(EXTRA_PULL_ORDER);
        }
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
        intent.putExtra(EXTRA_RESULT_PULL_ORDER, selectedType);
        setResult(Activity.RESULT_OK, intent);
    }

    private void initiateSpinner() {
        binding.pullOrderTypeSpinner.setSelection(mTypes.indexOf(selectedType));
        binding.pullOrderTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mTypes.indexOf(selectedType)) {
                    selectedType = mTypes.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // This is not allowed.
            }
        });
    }

    @Override
    public void onBackPressed() {
        saveConfiguration();
        super.onBackPressed();
    }
}
