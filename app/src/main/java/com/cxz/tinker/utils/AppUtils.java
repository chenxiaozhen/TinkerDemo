package com.cxz.tinker.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class AppUtils {

    /**
     * 读取AndroidManifest中的信息
     *
     * @param context
     * @return TINKER_ID
     */
    public static String getTinkerIdValue(Context context) {
        String channel = "";
        try {
            channel = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData.getString("TINKER_ID");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return channel;
    }
}
