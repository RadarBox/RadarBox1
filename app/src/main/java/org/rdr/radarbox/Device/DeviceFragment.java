package org.rdr.radarbox.Device;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationBarView;

import org.rdr.radarbox.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class DeviceFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_fragment, container, false);
        FragmentActivity activity = this.getActivity();
        NavigationBarView barView = view.findViewById(R.id.device_navigation);
        barView.setOnItemSelectedListener(
                item -> {
            switch (item.getItemId()) {
                case R.id.config_page:
                    DeviceConfigurationFragment fragment = new DeviceConfigurationFragment();
                    Bundle args = new Bundle();
                    args.putString("DeviceConfiguration", "Real");
                    fragment.setArguments(args);
                    activity.getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.device_navigation_container,
                                    fragment, null)
                            .commit();
                    return true;
                case R.id.status_page:
                    activity.getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.device_navigation_container,
                                    DeviceStatusFragment.class, null)
                            .commit();
                    return true;
                case R.id.communication_page:
                    activity.getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.device_navigation_container,
                                    DeviceCommunicationFragment.class, null)
                            .commit();
                    return true;
            }
            return false;
        });
        barView.getMenu().performIdentifierAction(barView.getMenu().getItem(0).getItemId(),0);
        return view;
    }
}
