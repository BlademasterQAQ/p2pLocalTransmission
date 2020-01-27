package com.blademaster.p2plocaltransmission;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;

public class Uri2RealPath {
    private String information;

    public String getFilePathByUri(Context context, Uri uri) {
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            if(isSystemAlbums(uri)){//文件来自系统相册、音乐或系统文件管理器时
                System.out.println("这是系统相册或系统文件管理器");
                information = "这是系统相册或系统文件管理器";
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor c = context.getContentResolver().query(uri, filePathColumns, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePathColumns[0]);
                path = c.getString(columnIndex);//（绝对路径）
                return path;
            }

            if (DocumentsContract.isDocumentUri(context, uri)) {//系统自身文件管理，不包括系统相册和第三方应用
                if (isExternalStorageDocument(uri)) {//在内部储存和外部储存中出现
                    // ExternalStorageProvider
                    System.out.println("这是ExternalStorageDocument");
                    information ="这是ExternalStorageDocument";
                    final String docId = DocumentsContract.getDocumentId(uri);
                    System.out.println(docId);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {//内部储存？
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];//(绝对路径)
                        return path;
                    }else{//外部储存？
                        path = "/mnt/ext_sdcard/"+split[1];//华为手机的外置SD卡的路径
                        return path;
                    }
                }else if (isDownloadsDocument(uri)) {//“下载内容”中出现，uri格式为“/document/raw:/storage/emulated/0/Download/QQMail/Simulink基本模块介绍.pdf”
                    // DownloadsProvider
                    System.out.println("这是DownloadsDocument");
                    information ="这是DownloadsDocument";
                    //该方法虽然多赞，但是可能对新版Android有错，public_downloads无法找到，all_downloads又需要ACCESS_ALL_DOWNLOADS权限，而这个权限在新版Android是没有的
                    /*final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);*/

                    //解决方法修改自：https://www.ojit.com/article/3644294
                    if(uri.getPath().indexOf("raw")==-1){//Uri形式为document/992时，此时必须通过getContentResolver().query查找文件
                        String fileName = getDownloadsDocumentPath(context, uri);//尝试解析文件的名字
                        System.out.println(fileName);
                        if (fileName != null) {
                            return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                        }
                    }else{
                        //其他形式时，得到的Uri中含有路径信息，只需除去冗余信息即可
                        return uri.getPath().split("raw:")[1];
//                        String id = DocumentsContract.getDocumentId(uri);
//                        System.out.println(id);
//                        if (id.startsWith("raw:")) {
//                            id = id.replaceFirst("raw:", "");
//                            System.out.println(id);
//                            File file = new File(id);
//                            if (file.exists())
//                                return id;
//                        }
                    }
                    //final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    //return getDataColumn(context, contentUri, null, null);

                } else if (isMediaDocument(uri)) {//在“图片”、“视频”、“音频”中出现，以id的形式存在，需要用getContentResolver().query方法查找（待学习）
                    // MediaProvider
                    System.out.println("这是MediaDocument");
                    information ="这是MediaDocument";
                    System.out.println(uri.getPath());
                    final String docId = DocumentsContract.getDocumentId(uri);//docId为“image:1329692”的形式
                    System.out.println(docId);
                    final String[] split = docId.split(":");
                    final String type = split[0];//“image”
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};//用id“1329692”去查找
                    path = getDataColumn(context, contentUri, selection, selectionArgs);//（绝对路径）
                    return path;
                }
            }
            //来自第三方软件的文件
            System.out.println("这是第三方软件"+uri.getAuthority());
            information ="这是第三方软件";
            if(isQQBrowserUri(uri)) {
                String str = uri.getPath().substring(10);//删除"/QQBrowser前缀"
                path = Environment.getExternalStorageDirectory() + str;//内部储存
                File file = new File(path);
                if(!file.exists()){//文件不存在，可能为外部储存
                    int local=-1;
                    for(int i=0;i<4;i++){//从头开始查找第四个"/"的位置
                        local = uri.getPath().indexOf("/",local+1);
                        System.out.println(local);
                    }
                    path = "/mnt/ext_sdcard/" + uri.getPath().substring(local+1);
                }
                return path;
            }
            if(isWPSUri(uri)){
                String str = uri.getPath().substring(9);//删除"/external前缀"
                path = Environment.getExternalStorageDirectory() + str;
                File file = new File(path);
                if(!file.exists()){//文件不存在，可能为外部储存
                    path = "/mnt/ext_sdcard" + str;
                }
                return path;
            }
        }
        return null;
    }

    private String getDownloadsDocumentPath(Context context, Uri uri) {

        Cursor cursor = null;
        final String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, null, null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                return cursor.getString(index);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};//返回的数据类型
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private boolean isSystemAlbums(Uri uri){
        return "media".equals(uri.getAuthority());
    }


    private boolean isQQBrowserUri(Uri uri) {//判断uri是否来自于QQ浏览器
        return "com.tencent.mtt.fileprovider".equals(uri.getAuthority());
    }

    private boolean isWPSUri(Uri uri) {//判断uri是否来自于QQ浏览器
        return "cn.wps.moffice_eng.fileprovider".equals(uri.getAuthority());
    }

    public String getInformation() {
        return information;
    }
}
