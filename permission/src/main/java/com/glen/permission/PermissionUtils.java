package com.glen.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2017/10/16.
 * 6.0权限检测处理工具类
 */
public class PermissionUtils {

    public static int CODE_REQUEST_PERMISSION = 0x9527;
    private static OnPermissionCallBack permissionCallBack;
    private static ArrayMap<String,Boolean> systemPermission = new ArrayMap<>();
    private static ArrayMap<String,Boolean> appopsPermission = new ArrayMap<>();

    public static boolean isOverMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static Activity getActivity(Object object) {
        if (object instanceof Fragment) {
            return ((Fragment) object).getActivity();
        } else if (object instanceof Activity) {
            return (Activity) object;
        }
        return null;
    }

    private static void initSystemPermission(String permission,boolean flag){
        if(systemPermission.containsKey(permission)){
            systemPermission.remove(permission);
            systemPermission.put(permission,flag);
        }else{
            systemPermission.put(permission,flag);
        }
    }

    private static void initAppopsPermission(String permission,boolean flag){
        if(appopsPermission.containsKey(permission)){
            appopsPermission.remove(permission);
            appopsPermission.put(permission,flag);
        }else{
            appopsPermission.put(permission,flag);
        }
    }

    private static boolean isOpenSystem(){
        int len = systemPermission.size();
        for (int i = 0; i < len;i++){
            if(!systemPermission.valueAt(i)){
                return true;
            }
        }
        return false;
    }

    private static boolean isOpenAppops(){
        int len = appopsPermission.size();
        for (int i = 0; i < len;i++){
            if(!appopsPermission.valueAt(i)){
                return true;
            }
        }
        return false;
    }

    // 判断是否缺少权限
    @TargetApi(value = Build.VERSION_CODES.M)
    public static boolean checkPermission(Context context, String permission) {

    // V4提供的申请权限
    // return PermissionChecker.checkPermission(context, permission, Process.myPid(), Process.myUid(), context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        //判断是否为小米系统
        /*
        关于MIUI 6.0 权限检测，运行时允许，但Appops拒绝 也是不能访问的
        第一步检测运行时权限
        第二步检测Appops权限
        */
        //小米机型上 拒绝权限后返回0 - 0:GRANTED,-1:DENIED，

        //第一步检测运行时权限
        boolean flag = ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED;
        initSystemPermission(permission,flag);
        boolean opFlag = true;

        if (TextUtils.equals(BrandUtils.getSystemInfo().getOs(), BrandUtils.SYS_MIUI)) {
            if (flag) {
                //第二步检测Appops权限
                AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                String op = appOpsManager.permissionToOp(permission);
                int checkOp = appOpsManager.checkOp(op, Process.myUid(), context.getPackageName());
                if (checkOp == AppOpsManager.MODE_ALLOWED) {
                    opFlag = true;
                } else {
                    opFlag = false;
                }
                initAppopsPermission(permission,opFlag);
            }
        }
        return flag && opFlag;
    }

    @TargetApi(value = Build.VERSION_CODES.M)
    public static List<String> findDeniedPermissions(Activity activity, String... permission) {
        List<String> denyPermissions = new ArrayList<>();
        for (String value : permission) {
            if (!checkPermission(activity, value)) {
                denyPermissions.add(value);
            }
        }
        return denyPermissions;
    }

    @TargetApi(value = Build.VERSION_CODES.M)
    public static void requestPermissions(Object object, String[] permissions, int requestCode, OnPermissionCallBack callBack) {
        if (!isOverMarshmallow()) {
            //6.0以下 do nothing
            return;
        }
        permissionCallBack = callBack;

        List<String> deniedPermissions = findDeniedPermissions(getActivity(object), permissions);
        //只申请拒绝的权限
        if (deniedPermissions.size() > 0) {
            // V4提供的申请权限
            //ActivityCompat.requestPermissions(getActivity(object),deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
            if (object instanceof Activity) {
                ((Activity) object).requestPermissions(deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
            } else if (object instanceof Fragment) {
                ((Fragment) object).requestPermissions(deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
            }
        }
    }

    @TargetApi(value = Build.VERSION_CODES.M)
    public static void onRequestPermissionsResult(Object object, int requestCode, String[] permissions, int[] grantResults) {
        Log.d("Permission", "onRequestPermissionsResult permissions.length = " + permissions.length);
        Log.d("Permission", "onRequestPermissionsResult grantResults.length = " + grantResults.length);
        if (permissions.length > 0 && grantResults.length > 0) {
            List<String> deniedPermissions = new ArrayList<>();
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    deniedPermissions.add(permissions[i]);
                }
                Log.d("Permission", "permission{" + permissions[i] + "},value = " + grantResults[i]);
            }

            Log.d("Permission", "deniedPermissions.size() = " + deniedPermissions.size());

            if (deniedPermissions.size() > 0) {
                if (object instanceof Activity) {
                    if (((Activity) object).shouldShowRequestPermissionRationale(deniedPermissions.get(0))) {
                        Log.d("Permission", deniedPermissions.get(0) + ",shouldShowRequestPermissionRationale = true");
                    } else {
                        Log.d("Permission", deniedPermissions.get(0) + ",shouldShowRequestPermissionRationale = false");
                    }
                    showRationableDialog(getActivity(object));
                }
                if (permissionCallBack != null)
                    permissionCallBack.onPermissionDenied();
            } else {
                if (permissionCallBack != null)
                    permissionCallBack.onPermissionAllowed();
            }
        }

    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Permission", "onActivityResult: resultCode = " + resultCode + ",data = " + data);
        if (requestCode == CODE_REQUEST_PERMISSION && resultCode == Activity.RESULT_OK) {//-1

        } else if (requestCode == CODE_REQUEST_PERMISSION && resultCode == Activity.RESULT_CANCELED) {//0

        }
    }

    public static void settingPermissionActivity(Activity activity) {
        Log.d("Permission", "机型：" + BrandUtils.getSystemInfo().getOs());
        //判断是否为小米系统
        if (TextUtils.equals(BrandUtils.getSystemInfo().getOs(), BrandUtils.SYS_MIUI)) {
            if(isOpenAppops()){
                Intent miuiIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                miuiIntent.putExtra("extra_pkgname", activity.getPackageName());
                //检测是否有能接受该Intent的Activity存在
                List<ResolveInfo> resolveInfos = activity.getPackageManager().queryIntentActivities(miuiIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (resolveInfos.size() > 0) {
                    activity.startActivityForResult(miuiIntent, CODE_REQUEST_PERMISSION);
                    return;
                }
            }else if(isOpenSystem()){
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivityForResult(intent, CODE_REQUEST_PERMISSION);
                return;
            }
        }

        //如果不是小米系统 则打开Android系统的应用设置页
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, CODE_REQUEST_PERMISSION);
    }

    private static void showRationableDialog(final Activity activity) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setMessage(activity.getResources().getString(R.string.dialog_msg))
                .setPositiveButton(activity.getResources().getString(R.string.dialog_btn_set), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settingPermissionActivity(activity);
                    }
                })
                .setNegativeButton(activity.getResources().getString(R.string.dialog_btn_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }

    public interface OnPermissionCallBack {
        void onPermissionAllowed();

        void onPermissionDenied();
    }

}
