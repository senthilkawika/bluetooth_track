package com.kawika.blueetoothtracker.Alerts;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import com.kawika.blueetoothtracker.config.AppConfiguration;

import static com.kawika.blueetoothtracker.config.AppConfiguration.LOCATION_PERMISSION;

/**
 * Created by senthiljs on 12/12/17.
 */

public class CustomAlerts {

    public static void permissionDialog(final Context context, String message, final int permission_id) {

        android.app.AlertDialog.Builder builder1 = new android.app.AlertDialog.Builder(context);
        builder1.setTitle("Permission");
        builder1.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("SETTINGS", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        if (permission_id == AppConfiguration.SETTINGS_PERMISSIONS) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.getPackageName(), null));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            dialog.cancel();
                        }

                    }
                });
        android.app.AlertDialog alert1 = builder1.create();
        alert1.show();
    }

    public static void alertDialog(final Context context, String message, final int permission_id) {

        android.app.AlertDialog.Builder builder1 = new android.app.AlertDialog.Builder(context);
        builder1.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        if (permission_id == LOCATION_PERMISSION) {
                            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.SEND_SMS}, AppConfiguration.LOCATION_PERMISSION);
                        }
                        dialog.cancel();
                    }
                });
        android.app.AlertDialog alert1 = builder1.create();
        alert1.show();
    }
}
