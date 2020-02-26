package com.example.damian.monitorapp.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.example.damian.monitorapp.activities.MainActivity;

public class CustomPrivileges {
    private static final int REQUEST_WRITE_STORAGE_CAMERA_REQUEST_CODE = 1;
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5469;

    public static void setUpPrivileges(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.SYSTEM_ALERT_WINDOW
                },
                REQUEST_WRITE_STORAGE_CAMERA_REQUEST_CODE);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(activity)) {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                        Uri.parse("package:" + activity.getPackageName()));
//                activity.startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
//            }
//        }
    }
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void enableAutoStart(final Activity activity) {
        if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
            new MaterialDialog.Builder(activity).title("Enable AutoStart")
                    .content("Please allow monitorApp to always run in the background \n" +
                            "Find monitorApp and Deselect \" Manage automatically \" \n" +
                            "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .positiveColor(Color.GRAY)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.miui.securitycenter",
                                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                            activity.startActivity(intent);
                        }
                    })
                    .show();

        }else if (Build.BRAND.equalsIgnoreCase("huawei")) {
            new MaterialDialog.Builder(activity).title("Allow Background work.")
                    .content(
                            "Find monitorApp and Deselect \"Manage automatically\" \n\n" +
                                    "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .positiveColor(Color.GRAY)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                            activity.startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.BRAND.equalsIgnoreCase("Letv")) {
            new MaterialDialog.Builder(activity).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background \n" +
                                    "Find monitorApp and Deselect \" Manage automatically \" \n" +
                                    "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .positiveColor(Color.GRAY)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.letv.android.letvsafe",
                                    "com.letv.android.letvsafe.AutobootManageActivity"));
                            activity.startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.BRAND.equalsIgnoreCase("Honor")) {
            new MaterialDialog.Builder(activity).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background \n" +
                                    "Find monitorApp and Deselect \" Manage automatically \" \n" +
                                    "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                            activity.startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.MANUFACTURER.equalsIgnoreCase("oppo")) {
            new MaterialDialog.Builder(activity).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background \n" +
                                    "Find monitorApp and Deselect \" Manage automatically \" \n" +
                                    "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName("com.coloros.safecenter",
                                        "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setClassName("com.oppo.safe",
                                            "com.oppo.safe.permission.startup.StartupAppListActivity");
                                    activity.startActivity(intent);
                                } catch (Exception ex) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setClassName("com.coloros.safecenter",
                                                "com.coloros.safecenter.startupapp.StartupAppListActivity");
                                        activity.startActivity(intent);
                                    } catch (Exception exx) {
                                    }
                                }
                            }
                        }
                    })
                    .show();
        } else if (Build.MANUFACTURER.contains("vivo")) {
            new MaterialDialog.Builder(activity).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background.Our app runs in background else our services can't be accesed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            try {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName("com.iqoo.secure",
                                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                                    activity.startActivity(intent);
                                } catch (Exception ex) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setClassName("com.iqoo.secure",
                                                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager");
                                        activity.startActivity(intent);
                                    } catch (Exception exx) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
                    })
                    .show();
        }
    }
}
