package com.kawika.blueetoothtracker.wheel;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.kawika.blueetoothtracker.Alerts.CustomAlerts;
import com.kawika.blueetoothtracker.BTConnection;
import com.kawika.blueetoothtracker.R;
import com.kawika.blueetoothtracker.config.AppConfiguration;
import com.skyfishjy.library.RippleBackground;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static android.view.View.VISIBLE;
import static com.kawika.blueetoothtracker.config.AppConfiguration.LOCATION_PERMISSION;
import static com.kawika.blueetoothtracker.wheel.CircleMenuLayout.pos_count;


public class MainCircleActivity extends AppCompatActivity {

    private CircleMenuLayout mCircleMenuLayout;
    private BluetoothAdapter mBluetoothAdapter;
    public static ArrayList<BluetoothDevice> blueToothDeviceArray = new ArrayList<>();
    private ArrayList<BluetoothDevice> plotBluetoodthDeviceArray = new ArrayList<>();
    private ArrayList<Integer> plotRssiArray = new ArrayList<>();
    private ArrayList<Integer> rssiArray = new ArrayList<>();
    private RippleBackground rippleBackground;
    private FloatingActionButton refreshFab;
    private ImageView clickedBlueToothImageView;
    private ProgressBar clickedPairingProgressBar;
    BTConnection mBluetoothConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        mCircleMenuLayout = findViewById(R.id.id_menulayout);
        registerBluetoothReceiver();

        mCircleMenuLayout.setOnMenuItemClickListener(new CircleMenuLayout.OnMenuItemClickListener() {

            @Override
            public void itemClick(View view, int pos, ImageView blueToothImageView, ProgressBar pairingProgressBar) {
                clickedBlueToothImageView = blueToothImageView;
                clickedPairingProgressBar = pairingProgressBar;
                if (blueToothDeviceArray.get(pos).getBondState() == BluetoothDevice.BOND_BONDED) {

                    Intent alarmIntent = new Intent(MainCircleActivity.this, AlarmActivity.class);
                    alarmIntent.putExtra("position", pos);
                    alarmIntent.putExtra("rssi", rssiArray.get(pos));
                    alarmIntent.putExtra("deviceAddress", blueToothDeviceArray.get(pos).getAddress());
                    startActivity(alarmIntent);

                    finish();

                } else {
                    pairDevice(blueToothDeviceArray.get(pos));
                }
            }


            @Override
            public void itemCenterClick(View view) {
                Toast.makeText(MainCircleActivity.this, "you can do something just like ccb  ", Toast.LENGTH_SHORT).show();
            }
        });


        rippleBackground = findViewById(R.id.content);
        refreshFab = findViewById(R.id.refreshFab);


        refreshFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if (ContextCompat.checkSelfPermission(MainCircleActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != 0) {
                            ActivityCompat.requestPermissions(MainCircleActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, AppConfiguration.LOCATION_PERMISSION);
                        } else {
                            if (!mBluetoothAdapter.isEnabled()) {
                                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(intent, 1000);
                            } else {
                                if (!mBluetoothAdapter.isDiscovering()) {
                                    mBluetoothAdapter.startDiscovery();
                                }


                            }
                        }


                    } else {
                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(intent, 1000);
                        } else {

                            if (!mBluetoothAdapter.isDiscovering()) {
                                mBluetoothAdapter.startDiscovery();

                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("Welcome activity", "PermissionM: ", e);
                }


            }
        });

    }


    private boolean checkViewFound(int count) {
        return (count != 1);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConfiguration.LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 1000);
                } else {
                    mBluetoothAdapter.startDiscovery();
                }
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                    CustomAlerts.permissionDialog(MainCircleActivity.this, "Turn on LOCATION Permission from SETTINGS --> PERMISSIONS", AppConfiguration.SETTINGS_PERMISSIONS);
                } else {
                    CustomAlerts.alertDialog(MainCircleActivity.this, "You must accept SMS permission to continue", LOCATION_PERMISSION);

                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000) {
            if (!mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.startDiscovery();
            }
        }

    }

    @Override
    public void onPause() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }

//        System.out.println("mCircleMenuLayout = " + mCircleMenuLayout);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    blueToothDeviceArray.clear();
                    pos_count = 0;
                    System.out.println(" discovery started= " );
                    refreshFab.setEnabled(false);
                    refreshFab.setAlpha(0.5f);

                    if (!rippleBackground.isRippleAnimationRunning()) {
                        rippleBackground.startRippleAnimation();
                    }
                    rssiArray.clear();

                    refreshFab.setEnabled(false);
                    refreshFab.setAlpha(0.5f);
                    do {
                        mCircleMenuLayout.removeAllViews();
                    } while (checkViewFound(mCircleMenuLayout.getChildCount()));


                    break;
                case BluetoothDevice.ACTION_FOUND:
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    System.out.println("device = " + device);


                    if (!blueToothDeviceArray.contains(device) && blueToothDeviceArray.size() < 15) {
                        plotBluetoodthDeviceArray.clear();
                        plotRssiArray.clear();

                        final int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                        blueToothDeviceArray.add(device);
                        rssiArray.add(rssi);

                        plotBluetoodthDeviceArray.add(device);
                        plotRssiArray.add(rssi);

                        mCircleMenuLayout.setMenuItemIconsAndTexts(plotBluetoodthDeviceArray, plotRssiArray);

                    }
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    rippleBackground.stopRippleAnimation();
                    refreshFab.setEnabled(true);
                    refreshFab.setAlpha(1f);


                    break;

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:

                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    final BluetoothDevice state_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    System.out.println(" hitted  = " + state);


                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                        Toast.makeText(MainCircleActivity.this, "Paired to " + state_device.getName(), Toast.LENGTH_SHORT).show();
                        clickedPairingProgressBar.setVisibility(View.GONE);
                        clickedBlueToothImageView.setImageDrawable(getResources().getDrawable(R.drawable.paired_bluetooth));
//                        startBTConnection(state_device, MY_UUID_INSECURE);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (clickedPairingProgressBar.getVisibility() == VISIBLE){
                                    clickedPairingProgressBar.setVisibility(View.GONE);
                                }
                                Intent alarmIntent = new Intent(MainCircleActivity.this, AlarmActivity.class);
                                startActivity(alarmIntent);
                            }
                        }, 300);


                    } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(MainCircleActivity.this, "Unpaired from " + state_device.getName(), Toast.LENGTH_SHORT).show();
                        clickedBlueToothImageView.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth));
                        clickedPairingProgressBar.setVisibility(View.GONE);

                    } else if (state == BluetoothDevice.BOND_NONE) {
                        clickedPairingProgressBar.setVisibility(View.GONE);
                    }
//                    else if (state == BluetoothDevice.BOND_BONDED){
//                        Toast.makeText(MainCircleActivity.this, "Paired to " + state_device.getName(), Toast.LENGTH_SHORT).show();
//                        clickedPairingProgressBar.setVisibility(View.GONE);
//                        clickedBlueToothImageView.setImageDrawable(getResources().getDrawable(R.drawable.paired_bluetooth));
//                        startBTConnection(state_device, MY_UUID_INSECURE);
//                    }

                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    final BluetoothDevice connection_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(MainCircleActivity.this, "Connected to " + connection_device.getName(), Toast.LENGTH_SHORT).show();

                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    final BluetoothDevice connection_device1 = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(MainCircleActivity.this, "disonnected to " + connection_device1.getName(), Toast.LENGTH_SHORT).show();

                    break;

                default:

                    break;
            }

        }
    };


    private void pairDevice(BluetoothDevice device) {
        try {
            if (clickedPairingProgressBar != null && clickedPairingProgressBar.getVisibility() == VISIBLE){
                clickedPairingProgressBar.setVisibility(View.GONE);
            }
            clickedPairingProgressBar.setVisibility(View.VISIBLE);
            Toast.makeText(MainCircleActivity.this, "Pairing to " + device.getName(), Toast.LENGTH_SHORT).show();
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        mBluetoothConnection = new BTConnection(MainCircleActivity.this);
        mBluetoothConnection.startClient(device, uuid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (ContextCompat.checkSelfPermission(MainCircleActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != 0) {
                    ActivityCompat.requestPermissions(MainCircleActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, AppConfiguration.LOCATION_PERMISSION);
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, 1000);
                    } else {
//                        registerBluetoothReceiver();
                        mBluetoothAdapter.startDiscovery();
                    }
                }


            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 1000);
                } else {
//                    registerBluetoothReceiver();
                    mBluetoothAdapter.startDiscovery();
                }
            }
        } catch (Exception e) {
            Log.e("Welcome activity", "PermissionM: ", e);
        }
    }

    private void registerBluetoothReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }
}