package com.kawika.blueetoothtracker.wheel;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kawika.blueetoothtracker.Alerts.CustomAlerts;
import com.kawika.blueetoothtracker.R;
import com.kawika.blueetoothtracker.config.AppConfiguration;
import com.kawika.blueetoothtracker.speedometer.ImageSpeedometer;

import static com.kawika.blueetoothtracker.config.AppConfiguration.LOCATION_PERMISSION;
import static com.kawika.blueetoothtracker.wheel.MainCircleActivity.blueToothDeviceArray;

/**
 * Created by senthiljs on 16/12/17.
 */

public class AlarmActivity extends AppCompatActivity {

    private ImageSpeedometer imageSpeedometer;
    private TextView deviceNameTextView, distanceTextView, selectedMetersTextView;
    private ImageView bluetoothImageView;
    private int array_pos = -1;
    private ProgressDialog progressDialog;
    private BluetoothAdapter mBluetoothAdapter;
    private String deviceAddress;
    private CountDownTimer Count;
    private int distance_progress = 75;
    private MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_activity);

        imageSpeedometer = findViewById(R.id.imageSpeedometer);
        deviceNameTextView = findViewById(R.id.deviceNameTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        bluetoothImageView = findViewById(R.id.bluetoothImageView);
        progressDialog = new ProgressDialog(AlarmActivity.this);


        final Bundle deviceBundle = getIntent().getExtras();

        if (deviceBundle != null) {
            array_pos = deviceBundle.getInt("position");
            int rssi = deviceBundle.getInt("rssi");
            deviceAddress = deviceBundle.getString("deviceAddress");

            deviceNameTextView.setText(blueToothDeviceArray.get(array_pos).getName());

            int distance = Integer.parseInt(String.valueOf(rssi).substring(1));
            int calculated_distance = distance - 20;

            distanceTextView.setText(calculated_distance + " m");

            imageSpeedometer.speedTo(calculated_distance, 4000);

        }
        mPlayer = MediaPlayer.create(AlarmActivity.this, R.raw.alert_alarm);


        bluetoothImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayer.isPlaying()) {
                    mPlayer.stop();
                }
            }
        });

//        ImageIndicator imageIndicator = new ImageIndicator(getApplicationContext(), R.drawable.image_indicator1);
//        imageSpeedometer.setIndicator(imageIndicator);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        registerBluetoothReceiver();
        ImageView distanceImageView = findViewById(R.id.diastanceImageView);
        distanceImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDistanceSetDialog();
            }
        });

        selectedMetersTextView = findViewById(R.id.selectedMetersTextView);


    }


    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:

                    break;
                case BluetoothDevice.ACTION_FOUND:
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    System.out.println("printing both " + device.getAddress() + "--" + deviceAddress);

                    if (device.getAddress().equals(deviceAddress)) {
                        int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                        int distance = Integer.parseInt(String.valueOf(rssi).substring(1));
//                        int calculated_distance = distance - 30;
                        distanceTextView.setText(distance + " m");
                        imageSpeedometer.speedTo(distance, 4000);
                        deviceNameTextView.setText(device.getName());
                        System.out.println("inside came device = " + distance + "--" + distance_progress);

                        if (distance > distance_progress) {
                            System.out.println(" if is showing= ");
                            mPlayer.start();
                        } else {
                            if (mPlayer.isPlaying()) {
                                mPlayer.stop();
                            }
                            System.out.println(" else is showing= ");
                        }
                    }

                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    mBluetoothAdapter.startDiscovery();

                    break;

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:

                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    final BluetoothDevice state_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    System.out.println(" hitted  = " + state);


                    if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(AlarmActivity.this, "Unpaired from " + state_device.getName(), Toast.LENGTH_SHORT).show();
                        bluetoothImageView.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth));
                        progressDialog.dismiss();

                    } else if (state == BluetoothDevice.BOND_NONE) {
                        progressDialog.dismiss();
                    }

                    break;


                default:

                    break;
            }

        }
    };

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
                    CustomAlerts.permissionDialog(AlarmActivity.this, "Turn on LOCATION Permission from SETTINGS --> PERMISSIONS", AppConfiguration.SETTINGS_PERMISSIONS);
                } else {
                    CustomAlerts.alertDialog(AlarmActivity.this, "You must accept SMS permission to continue", LOCATION_PERMISSION);

                }
            }
        }
    }

    private void startCountdown() {
        Count = new CountDownTimer(122000, 1000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                if (Count != null) {
                    Count.cancel();
                }
            }
        };

        Count.start();
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
    public void onDestroy() {
        unregisterReceiver(bluetoothReceiver);

        System.out.println(" destroyed the alarm activity= ");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(AlarmActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != 0) {
                    ActivityCompat.requestPermissions(AlarmActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, AppConfiguration.LOCATION_PERMISSION);
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, 1000);
                    } else {
                        mBluetoothAdapter.startDiscovery();
                    }
                }

            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 1000);
                } else {
                    mBluetoothAdapter.startDiscovery();
                }
            }
        } catch (Exception e) {
            Log.e("Welcome activity", "PermissionM: ", e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();

            Intent main_intent = new Intent(AlarmActivity.this, MainCircleActivity.class);
            startActivity(main_intent);
            finish();
        }

    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    public void showDistanceSetDialog() {

        final Dialog dialog = new Dialog(AlarmActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.distance_dialog);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        dialog.getWindow().setAttributes(lp);

        final TextView distanceTextView = dialog.findViewById(R.id.distanceTextView);
        SeekBar distanceSeekbar = dialog.findViewById(R.id.distanceSeekbar);
        distanceSeekbar.setMax(100);

        Button setDistanceButton = dialog.findViewById(R.id.setDistanceButton);
        setDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                selectedMetersTextView.setText(distance_progress + " m");
            }
        });

        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        distanceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distanceTextView.setText(progress + " meters");
                distance_progress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        dialog.show();

    }
}
