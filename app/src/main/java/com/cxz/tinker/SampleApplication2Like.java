package com.cxz.tinker;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.multidex.MultiDex;

import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.lib.util.UpgradePatchRetry;
import com.tencent.tinker.loader.app.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created by chenxz on 2017/9/8.
 *
 * 最简单的集成方式
 */
@DefaultLifeCycle(
        application = "com.cxz.tinker.SampleApplication2",
        flags = ShareConstants.TINKER_ENABLE_ALL,
        loadVerifyFlag = false
)
public class SampleApplication2Like extends DefaultApplicationLike {

    public SampleApplication2Like(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 编写自己Application的业务

    }

    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);

        initTinker(base);

    }

    private void initTinker(Context base) {
        //必须安装MultiDex的更新!
        MultiDex.install(base);
        //安装Tinker后加载multiDex
        TinkerInstaller.install(this);
        UpgradePatchRetry.getInstance(this.getApplication()).setRetryEnable(true);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
        getApplication().registerActivityLifecycleCallbacks(callback);
    }

}
