package com.blademaster.p2plocaltransmission;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

/**
 * 由String数组获取系统权限，每一个String为一个权限的String名
 * <p>
 * 权限名可以通过调用{@link android.Manifest}的静态常量获得String对象，也可以直接写入AndroidManifest.xml中的权限名"android.permission.xxx"作为String对象
 *
 * @author blademaster
 */
public class getSystemPermission {

    public getSystemPermission(Activity activity, String[] PermissionName, int requestCode){//activity为申请权限的环境，PermissionName为申请的权限名组成的数组(以Manifest.permission.xxx的形式给出)，requestCode为识别回调函数的值
        ArrayList<String> permissionName=new ArrayList(0);
        for(int i=0;i<PermissionName.length;i++){
            if(ContextCompat.checkSelfPermission(activity, PermissionName[i])!= PackageManager.PERMISSION_GRANTED){
                //未获取读取权限时
                permissionName.add(PermissionName[i]);//添加要获得的权限名
            }
        }

        if(permissionName.size()!=0){
            ActivityCompat.requestPermissions(activity,permissionName.toArray(new String[permissionName.size()]),requestCode);
        }
    }
}
