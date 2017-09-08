package com.cxz.tinker;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tv_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_load_patch).setOnClickListener(this);
        findViewById(R.id.btn_clean_patch).setOnClickListener(this);
        findViewById(R.id.btn_kill_app).setOnClickListener(this);
        tv_content = (TextView) findViewById(R.id.tv_content);


        /**
         * 第一次运行安装之后，在将这里注释部分取消，调用tinkerPatchDebug或者tinkerPatchRelease命令生成补丁文件。
         * 将补丁文件patch_signed_7zip.apk复制到手机指定地址加载补丁可看到实际效果。
         */
//        tv_content.setText("Tinker");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_load_patch:// 加载本地补丁，Tinker还提供了其他集中加载方式，更多请浏览Tinker官方文档
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch_signed_7zip.apk";
                TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), path);
                break;
            case R.id.btn_clean_patch:// 卸载补丁
                Tinker.with(getApplicationContext()).cleanPatch();
                break;
            case R.id.btn_kill_app:// 退出
                ShareTinkerInternals.killAllOtherProcess(getApplicationContext());
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
        }
    }
}
