package com.blademaster.p2plocaltransmission;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

public final class FileUtils {

    public static String getFilePathByUri(Context context, Uri uri) {
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
            //QQ浏览器文件
            /*if(isQQBrowserUri(uri)){//文件来自QQ浏览器时
                System.out.println("这是QQ浏览器");
                String str=uri.getPath();
                System.out.println(str);
                path = Environment.getExternalStorageDirectory()+uri.getPath();
                return path;
            }

            if(isWPSUri(uri)){//文件来自WPS时
                System.out.println("文件来自WPS");
                String str=uri.getPath();
                System.out.println(str);
                path = Environment.getExternalStorageDirectory()+uri.getPath();
                return path;
            }*/

            if(isSystemAlbums(uri)){//文件来自系统相册、音乐或系统文件管理器时
                System.out.println("这是系统相册或系统文件管理器");
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
                    System.out.println(uri.getPath());
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {//内部储存？
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];//(绝对路径)
                        return path;
                    }else{//外部储存？
                        path = split[1];//(非绝对路径)
                        return path;
                    }
                }else if (isDownloadsDocument(uri)) {//“下载内容”中出现，uri格式为“/document/raw:/storage/emulated/0/Download/QQMail/Simulink基本模块介绍.pdf”
                    // DownloadsProvider
                    System.out.println("这是DownloadsDocument");
                    //略有改版？按别人的代码打开下载下载内容会出现错误，直接用uri.getPath()即可
                    /*final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);*/
                    System.out.println(uri.getPath());
                    path=uri.getPath().split(":")[1];//取文件路径部分(绝对路径)
                    return path;

                } else if (isMediaDocument(uri)) {//在“图片”、“视频”、“音频”中出现，以id的形式存在，需要用getContentResolver().query方法查找（待学习）
                    // MediaProvider
                    System.out.println("这是MediaDocument");
                    System.out.println(uri.getPath());
                    final String docId = DocumentsContract.getDocumentId(uri);//docId为“image:1329692”的形式
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
            String str=uri.getPath();
            System.out.println(str);
            path = uri.getPath();
            return path;//这不是绝对路径
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
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
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isSystemAlbums(Uri uri){
        return "media".equals(uri.getAuthority());
    }


    public static boolean isQQBrowserUri(Uri uri) {//判断uri是否来自于QQ浏览器
        return "com.tencent.mtt.fileprovider".equals(uri.getAuthority());
    }

    public static boolean isWPSUri(Uri uri) {//判断uri是否来自于QQ浏览器
        return "cn.wps.moffice_eng.fileprovider".equals(uri.getAuthority());
    }
}
