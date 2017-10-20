package com.permission.demo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.glen.permission.PermissionUtils;


public class MainActivity extends AppCompatActivity {

    private Button btn;
    private TextView info;
    private String[] permissions = {Manifest.permission.READ_PHONE_STATE,Manifest.permission.SEND_SMS};
    private int requestCode = 0x001;

    private void requestPermissions(){
        PermissionUtils.requestPermissions(MainActivity.this, permissions, requestCode, new PermissionUtils.OnPermissionCallBack() {
            @Override
            public void onPermissionAllowed() {
                info.setText(getDeviceId(MainActivity.this));
            }

            @Override
            public void onPermissionDenied() {
                info.setText("Permission Denied");
            }
        });
    }

    private void requestPermission(){
        boolean allowed  = PermissionUtils.checkPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE);
        if(allowed){
            info.setText(getDeviceId(MainActivity.this));
        }else{
            PermissionUtils.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, requestCode, new PermissionUtils.OnPermissionCallBack() {
                @Override
                public void onPermissionAllowed() {
                    info.setText(getDeviceId(MainActivity.this));
                }

                @Override
                public void onPermissionDenied() {
                    info.setText("Permission Denied");
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.btn);
        info = (TextView) findViewById(R.id.info);

        /*请求多个权限*/
//        requestPermissions();

        /*请求单个权限*/
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                requestPermission();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(this,requestCode,permissions,grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PermissionUtils.onActivityResult(requestCode,resultCode,data);
    }

    public static String getDeviceId(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getDeviceId();
    }
}
