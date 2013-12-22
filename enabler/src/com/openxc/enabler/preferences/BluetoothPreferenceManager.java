package com.openxc.enabler.preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.openxc.enabler.R;
import com.openxc.interfaces.bluetooth.BluetoothException;
import com.openxc.interfaces.bluetooth.BluetoothVehicleInterface;
import com.openxc.interfaces.bluetooth.DeviceManager;
import com.openxc.util.SupportSettingsUtils;

/**
 * Enable or disable receiving vehicle data from a Bluetooth CAN device.
 */
public class BluetoothPreferenceManager extends VehiclePreferenceManager {
    private final static String TAG = "BluetoothPreferenceManager";

    private DeviceManager mBluetoothDeviceManager;
    private HashMap<String, String> mDiscoveredDevices =
            new HashMap<String, String>();

    public BluetoothPreferenceManager(Context context) {
        super(context);
        try {
            mBluetoothDeviceManager = new DeviceManager(context);
            fillBluetoothDeviceList();
        } catch(BluetoothException e) {
            Log.w(TAG, "This device most likely does not have " +
                    "a Bluetooth adapter");
        }
    }

    public Map<String, String> getDiscoveredDevices() {
        return (Map<String, String>) mDiscoveredDevices.clone();
    }

    @Override
    public void close() {
        super.close();
        if(mDiscoveryReceiver != null) {
            getContext().unregisterReceiver(mDiscoveryReceiver);
        }
        mBluetoothDeviceManager.stop();
    }

    protected PreferenceListener createPreferenceListener() {
        return new PreferenceListener() {
            private int[] WATCHED_PREFERENCE_KEY_IDS = {
                R.string.bluetooth_checkbox_key,
                R.string.bluetooth_mac_key,
            };

            protected int[] getWatchedPreferenceKeyIds() {
                return WATCHED_PREFERENCE_KEY_IDS;
            }

            public void readStoredPreferences() {
                setBluetoothStatus(getPreferences().getBoolean(
                            getString(R.string.bluetooth_checkbox_key), true));
            }
        };
    }

    private synchronized void setBluetoothStatus(boolean enabled) {
        if(enabled) {
            Log.i(TAG, "Enabling the Bluetooth vehicle interface");
            String deviceAddress = getPreferenceString(
                    R.string.bluetooth_mac_key);
            if(deviceAddress == null || deviceAddress.equals(
                        getString(R.string.bluetooth_mac_automatic_option))) {
                deviceAddress = null;
                Log.d(TAG, "No Bluetooth vehicle interface selected -- " +
                        "starting in automatic mode");
            }
            fillBluetoothDeviceList();
            getVehicleManager().addVehicleInterface(
                    BluetoothVehicleInterface.class, deviceAddress);
        } else {
            Log.i(TAG, "Disabling the Bluetooth vehicle interface");
            getVehicleManager().removeVehicleInterface(
                    BluetoothVehicleInterface.class);
        }
    }

    private void fillBluetoothDeviceList() {
        for(BluetoothDevice device :
                mBluetoothDeviceManager.getPairedDevices()) {
            mDiscoveredDevices.put(device.getAddress(),
                    device.getName() + " (" + device.getAddress() + ")");
        }

        persistCandidateDiscoveredDevices();
        mBluetoothDeviceManager.startDiscovery(mDiscoveryReceiver);
    }

    private BroadcastReceiver mDiscoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    String summary = device.getName() + " (" +
                            device.getAddress() + ")";
                    Log.d(TAG, "Found unpaired device: " + summary);
                    mDiscoveredDevices.put(device.getAddress(), summary);
                    persistCandidateDiscoveredDevices();
                }
            }
        }
    };

    private void persistCandidateDiscoveredDevices() {
        SharedPreferences.Editor editor =
                getContext().getSharedPreferences(
                        DeviceManager.KNOWN_BLUETOOTH_DEVICE_PREFERENCES,
                        Context.MODE_MULTI_PROCESS).edit();
        Set<String> candidates = new HashSet<String>();
        for(Map.Entry<String, String> device : mDiscoveredDevices.entrySet()) {
            if(device.getValue().startsWith(
                        BluetoothVehicleInterface.DEVICE_NAME_PREFIX)) {
                candidates.add(device.getKey());
            }
        }
        SupportSettingsUtils.putStringSet(editor,
                DeviceManager.KNOWN_BLUETOOTH_DEVICE_PREF_KEY, candidates);
        editor.commit();
    }
}
