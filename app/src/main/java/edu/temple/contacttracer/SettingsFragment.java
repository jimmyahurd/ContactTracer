package edu.temple.contacttracer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

//Fragment used to update app preferences
public class SettingsFragment extends PreferenceFragmentCompat {
    SettingsListener settingsListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Preferences that are only clicked will not make a call to OnSharedPreferencesChanged(),
        //so manually had to add an on click listener that tells Activity it is attached to to
        //generate a new UUID immediately
        Preference newUUId = findPreference(getString(R.string.NewUUIDPreference));
        newUUId.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                settingsListener.generateUUID();
                return false;
            }
        });
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof StartupFragment.StartupFragmentListener) {
            settingsListener = (SettingsListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement SettingsListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        settingsListener = null;
    }

    public interface SettingsListener{
        void generateUUID();
    }
}