/*
 * Copyright (C) 2020 The Pixel Experience Project
 *               2022 StatiXOS
 *               2021-2022 crDroid Android Project
 *               2019-2024 The Evolution X Project
 *               2021-2024 ClownUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.clown;

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.R;
import com.android.internal.util.clown.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @hide
 */
public final class PixelPropsUtils {

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";
    private static final String PACKAGE_GOOGLE = "com.google";
    private static final String PACKAGE_SI = "com.google.android.settings.intelligence";
    private static final String SPOOF_PI = "persist.sys.pihooks.enable";
    private static final String SPOOF_PIXEL_PROPS = "persist.sys.pphooks.enable";

    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final String DEVICE = "ro.clown.device";
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.pihooks.debug", false);

    private static final String sDeviceModel =
            SystemProperties.get("ro.product.model", Build.MODEL);
    private static final String sDeviceFingerprint =
            SystemProperties.get("ro.product.fingerprint", Build.FINGERPRINT);
    private static final Boolean sEnablePixelProps =
            Resources.getSystem().getBoolean(R.bool.config_enablePixelProps);

    private static final Map<String, Object> propsToChangeGeneric;
    private static final Map<String, Object> propsToChangeRecentPixel;
    private static final Map<String, Object> propsToChangePixelTablet;
    private static final Map<String, ArrayList<String>> propsToKeep;

    // Packages to Spoof as the most recent Pixel device
    private static final String[] packagesToChangeRecentPixel = {
            "com.amazon.avod.thirdpartyclient",
            "com.android.chrome",
            "com.breel.wallpapers20",
            "com.disney.disneyplus",
            "com.google.android.aicore",
            "com.google.android.apps.accessibility.magnifier",
            "com.google.android.apps.aiwallpapers",
            "com.google.android.apps.bard",
            "com.google.android.apps.customization.pixel",
            "com.google.android.apps.emojiwallpaper",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.apps.pixel.agent",
            "com.google.android.apps.pixel.creativeassistant",
            "com.google.android.apps.pixel.support",
            "com.google.android.apps.privacy.wildlife",
            "com.google.android.apps.subscriptions.red",
            "com.google.android.apps.wallpaper",
            "com.google.android.apps.wallpaper.pixel",
            "com.google.android.apps.weather",
            "com.google.android.gms",
            "com.google.android.googlequicksearchbox",
            "com.google.android.soundpicker",
            "com.google.android.wallpaper.effects",
            "com.google.pixel.livewallpaper",
            "com.microsoft.android.smsorganizer",
            "com.nhs.online.nhsonline",
            "com.nothing.smartcenter",
            "com.realme.link",
            "in.startv.hotstar",
            "jp.id_credit_sp2.android"
    };

    private static final String[] customGoogleCameraPackages = {
            "com.google.android.MTCL83",
            "com.google.android.UltraCVM",
            "com.google.android.apps.cameralite"
    };

    // Codenames for currently supported Pixels by Google
    private static final String[] pixelCodenames = {
            "rango", // Pixel 10 Pro Fold
            "mustang", // Pixel 10 Pro XL
            "blazer", // Pixel 10 Pro
            "frankel", // Pixel 10
            "komodo",
            "caiman",
            "tokay",
            "comet",
            "akita",
            "husky",
            "shiba",
            "felix",
            "tangorpro",
            "lynx",
            "cheetah",
            "panther",
            "bluejay",
            "oriole",
            "raven",
            "barbet"
    };

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static volatile boolean sIsGms, sIsExcluded;
    private static volatile String sProcessName;

    static {
        propsToKeep = new HashMap<>();
        propsToKeep.put(PACKAGE_SI, new ArrayList<>(Collections.singletonList("FINGERPRINT")));
        propsToChangeGeneric = new HashMap<>();
        propsToChangeGeneric.put("TYPE", "user");
        propsToChangeGeneric.put("TAGS", "release-keys");
        propsToChangeRecentPixel = new HashMap<>();
        propsToChangeRecentPixel.put("BRAND", "google");
        propsToChangeRecentPixel.put("MANUFACTURER", "Google");
        propsToChangeRecentPixel.put("DEVICE", "komodo");
        propsToChangeRecentPixel.put("PRODUCT", "komodo");
        propsToChangeRecentPixel.put("HARDWARE", "komodo");
        propsToChangeRecentPixel.put("MODEL", "Pixel 9 Pro XL");
        propsToChangeRecentPixel.put("ID", "AP4A.241205.013");
        propsToChangeRecentPixel.put("FINGERPRINT", "google/komodo/komodo:15/AP4A.241205.013/12621605:user/release-keys");
        propsToChangePixelTablet = new HashMap<>();
        propsToChangePixelTablet.put("BRAND", "google");
        propsToChangePixelTablet.put("MANUFACTURER", "Google");
        propsToChangePixelTablet.put("DEVICE", "tangorpro");
        propsToChangePixelTablet.put("PRODUCT", "tangorpro");
        propsToChangePixelTablet.put("HARDWARE", "tangorpro");
        propsToChangePixelTablet.put("MODEL", "Pixel Tablet");
        propsToChangePixelTablet.put("ID", "AP4A.241205.013");
        propsToChangePixelTablet.put("FINGERPRINT", "google/tangorpro/tangorpro:15/AP4A.241205.013/12621605:user/release-keys");
    }

    public static String getBuildID(String fingerprint) {
        Pattern pattern = Pattern.compile("([A-Za-z0-9]+\\.\\d+\\.\\d+\\.\\w+)");
        Matcher matcher = pattern.matcher(fingerprint);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String getDeviceName(String fingerprint) {
        String[] parts = fingerprint.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "";
    }

    private static boolean isGoogleCameraPackage(String packageName) {
        return packageName.contains("GoogleCamera")
                || Arrays.asList(customGoogleCameraPackages).contains(packageName);
    }

    private static boolean shouldTryToCertifyDevice() {
        if (!sIsGms) return false;

        final String processName = Application.getProcessName();
        if (!processName.toLowerCase().contains("unstable")) {
            return false;
        }

        final boolean was = isGmsAddAccountActivityOnTop();
        final String reason = "GmsAddAccountActivityOnTop";
        if (!was) {
            return true;
        }
        dlog("Skip spoofing build for GMS, because " + reason + "!");
        TaskStackListener taskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                final boolean isNow = isGmsAddAccountActivityOnTop();
                if (isNow ^ was) {
                    dlog(String.format("%s changed: isNow=%b, was=%b, killing myself!", reason, isNow, was));
                    Process.killProcess(Process.myPid());
                }
            }
        };
        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener!", e);
            return true;
        }
    }

    public static void spoofBuildGms(Context context) {
        if (!SystemProperties.getBoolean(SPOOF_PI, true))
            return;

        String packageName = "com.goolag.pif";

        if (!Utils.isPackageInstalled(context, packageName)) {
            Log.e(TAG, "'" + packageName + "' is not installed.");
            return;
        }

        PackageManager pm = context.getPackageManager();

        try {
            Resources resources = pm.getResourcesForApplication(packageName);

            int resourceId = resources.getIdentifier("device_arrays", "array", packageName);
            if (resourceId != 0) {
                String[] deviceArrays = resources.getStringArray(resourceId);

                if (deviceArrays.length > 0) {
                    int randomIndex = new Random().nextInt(deviceArrays.length);
                    int selectedArrayResId = resources.getIdentifier(deviceArrays[randomIndex], "array", packageName);
                    String selectedArrayName = resources.getResourceEntryName(selectedArrayResId);
                    String[] selectedDeviceProps = resources.getStringArray(selectedArrayResId);

                    setPropValue("MANUFACTURER", selectedDeviceProps[0]);
                    setPropValue("MODEL", selectedDeviceProps[1]);
                    setPropValue("FINGERPRINT", selectedDeviceProps[2]);
                    setPropValue("BRAND", selectedDeviceProps[3]);
                    setPropValue("PRODUCT", selectedDeviceProps[4]);
                    setPropValue("DEVICE", selectedDeviceProps[5].isEmpty() ? getDeviceName(selectedDeviceProps[2]) : selectedDeviceProps[5]);
                    setVersionFieldString("RELEASE", selectedDeviceProps[6]);
                    setPropValue("ID", selectedDeviceProps[7].isEmpty() ? getBuildID(selectedDeviceProps[2]) : selectedDeviceProps[7]);
                    setVersionFieldString("INCREMENTAL", selectedDeviceProps[8]);
                    setPropValue("TYPE", selectedDeviceProps[9].isEmpty() ? "user" : selectedDeviceProps[9]);
                    setPropValue("TAGS", selectedDeviceProps[10].isEmpty() ? "release-keys" : selectedDeviceProps[10]);
                    setVersionFieldString("SECURITY_PATCH", selectedDeviceProps[11]);
                    setVersionFieldInt("DEVICE_INITIAL_SDK_INT", Integer.parseInt(selectedDeviceProps[12]));

                    Settings.System.putString(context.getContentResolver(), Settings.System.PPU_SPOOF_BUILD_GMS_ARRAY, selectedArrayName);
                } else {
                    Log.e(TAG, "No device arrays found.");
                }
            } else {
                Log.e(TAG, "Resource 'device_arrays' not found.");
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting resources for '" + packageName + "': " + e.getMessage());
        }
    }

    public static void setProps(Context context) {
        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();
        Map<String, Object> propsToChange = new HashMap<>();
        Context appContext = context.getApplicationContext();
        final boolean sIsTablet = isDeviceTablet(appContext);
        sProcessName = processName;
        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsExcluded = isGoogleCameraPackage(packageName);
        propsToChangeGeneric.forEach((k, v) -> setPropValue(k, v));
        if (packageName == null || processName == null || packageName.isEmpty()) {
            return;
        }
        if (sIsExcluded) {
            return;
        }
        if (sIsGms) {
            if (shouldTryToCertifyDevice()) {
                if (!SystemProperties.getBoolean(SPOOF_PI, true)) {
                    return;
                } else {
                    spoofBuildGms(context);
                }
            }
        } else if (Arrays.asList(packagesToChangeRecentPixel).contains(packageName) && !sIsGms) {

            boolean isPixelDevice = Arrays.asList(pixelCodenames).contains(SystemProperties.get(DEVICE));
            if (isPixelDevice || !sEnablePixelProps || !SystemProperties.getBoolean(SPOOF_PIXEL_PROPS, true)) {
                return;
            } else if (SystemProperties.getBoolean(SPOOF_PIXEL_PROPS, true)) {
                if (sIsTablet) {
                    propsToChange.putAll(propsToChangePixelTablet);
                } else {
                    propsToChange.putAll(propsToChangeRecentPixel);
                }
            }
        }
        dlog("Defining props for: " + packageName);
        for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            if (propsToKeep.containsKey(packageName) && propsToKeep.get(packageName).contains(key)) {
                dlog("Not defining " + key + " prop for: " + packageName);
                continue;
            }
            dlog("Defining " + key + " prop for: " + packageName);
            setPropValue(key, value);
        }
        // Set proper indexing fingerprint
        if (packageName.equals(PACKAGE_SI)) {
            setPropValue("FINGERPRINT", String.valueOf(Build.TIME));
            return;
        }
        if (packageName.equals(PACKAGE_ARCORE)) {
            setPropValue("FINGERPRINT", sDeviceFingerprint);
            return;
        }
        // Show correct model name on gms services
        if (packageName.toLowerCase().contains("com.google.android.gms")) {
            if (processName.toLowerCase().contains("ui")) {
                setPropValue("MODEL", sDeviceModel);
                return;
            }
        }
    }

    private static boolean isDeviceTablet(Context context) {
        if (context == null) {
            return false;
        }
        Configuration configuration = context.getResources().getConfiguration();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXHIGH
                || displayMetrics.densityDpi == DisplayMetrics.DENSITY_XXXHIGH;
    }

    private static void setPropValue(String key, Object value) {
        try {
            dlog("Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setVersionField(String key, Object value) {
        try {
            dlog("Defining version field " + key + " to " + value.toString());
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set version field " + key, e);
        }
    }

    private static void setVersionFieldString(String key, String value) {
        try {
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void setVersionFieldInt(String key, int value) {
        try {
            dlog("Defining version field " + key + " to " + value);
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final int gmsUid;
        try {
            gmsUid = context.getPackageManager().getApplicationInfo("com.google.android.gms", 0).uid;
            dlog("shouldBypassTaskPermission: gmsUid:" + gmsUid + " callingUid:" + callingUid);
        } catch (Exception e) {
            Log.e(TAG, "shouldBypassTaskPermission: unable to get gms uid", e);
            return false;
        }
        return gmsUid == callingUid;
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                        .anyMatch(elem -> elem.getClassName().toLowerCase()
                            .contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        if (!SystemProperties.getBoolean(SPOOF_PI, true))
            return;
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() && !sIsExcluded) {
            dlog("Blocked key attestation");
            throw new UnsupportedOperationException();
        }
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, "[" + sProcessName + "] " + msg);
    }
}
