package com.hy.mipaycard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.hy.mipaycard.Utils.CardList;
import com.hy.mipaycard.Utils.FileMd5;
import com.hy.mipaycard.Utils.HttpUtil;
import com.hy.mipaycard.Utils.UnzipFromAssets;
import com.hy.mipaycard.Utils.cmdUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.hy.mipaycard.Config.git_url;
import static com.hy.mipaycard.Config.pay_pic;
import static com.hy.mipaycard.Utils.cmdUtil.runRootShell;

public class MainUtils {

    public static void getTsm(Context context){
        if(!isInstallApp(context,"com.miui.tsmclient")){
            Toast.makeText(context,"您的设备可能不支持本APP",Toast.LENGTH_LONG).show();
        }
    }

    public static void getCard(Context context){
        String[] cardList = cmdUtil.getTsmclient();
        String[] cardName = CardList.getCardName(cardList);
        StringBuilder info = new StringBuilder();
        String filePath;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MiPayCard");
        if(!file.exists()){
            file.mkdirs();
        }
        List<String> cmdList = new ArrayList<String>();
        for (int i =0;i<cardList.length;i++){
            filePath = getFilePath(file,"card"+(i+1));
            cmdList.add("cp "+pay_pic+"/"+cardList[i]+" "+filePath);
            info.append("CardFile: ").append(cardList[i]).append("\nCardName: ").append(cardName[i]).append("\nFileName: ").append(filePath.substring(filePath.lastIndexOf("/")+1)).append("\n\n");
        }
        String log = runRootShell(cmdList.toArray(new String[cmdList.size()]));
        CardList.save(info.toString());
        Toast.makeText(context,"已提取，储存在\n"+file.getPath(),Toast.LENGTH_LONG).show();
    }

    private static String getFilePath(File filePath, String fileName){
        File file = new File(filePath,fileName+".png");
        if(!file.exists()){
            return file.getPath();
        } else {
            int i=0;
            Boolean bool = true;
            do{
                i++;
                file = new File(filePath,fileName+"_"+i+".png");
                if(!file.exists()){
                    bool=false;
                }
            }while(bool);
            return file.getPath();
        }
    }

    public static void copyCard(Context context){
        File miku = new File(context.getFilesDir(),"miku.png");
        File lty = new File(context.getFilesDir(),"luotianyi.png");
        String mikuMd5 = "1912841bea156df9208609d05a85f390";
        String ltyMd5 = "e574821c45805b03cb2b72cb7d2d6024";

        if(!miku.exists() || !lty.exists() || !mikuMd5.equals(FileMd5.md5(miku)) || !ltyMd5.equals(FileMd5.md5(lty))){
            try{
                UnzipFromAssets.unZip(context, "assets.zip", context.getFilesDir().getPath(), true);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void initOther(final Context context){
        new Thread(new Runnable() {
            @Override
            public void run() {
                copyCard(context);
                final String str = CardList.initLocalCardList(context);
                HttpUtil.sendOkHttpRequest(git_url+"card_list.json", new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String data = response.body().string();
                        CardList.getListFromJson(data);
                        int local = CardList.getJsonLine(str);
                        int netWork = CardList.getJsonLine(data);
                        if(netWork>local){
                            CardList.saveJsonData(context, data);
                        }
                    }
                });
            }
        }).run();
    }

    public static void copyToClipboard(Context context, String text) {
        ClipboardManager systemService = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        systemService.setPrimaryClip(ClipData.newPlainText("text", text));
    }

    public static void showAboutDialog(final Context context){
        String aboutText = CardList.getAboutText();
        if (aboutText.equals("关于信息")) {
            aboutText = context.getString(R.string.about_text);
        }
        TextView textView = new TextView(context);
        textView.setText(R.string.about_text2);
        new AlertDialog.Builder(context)
                .setTitle("关于")
                .setMessage(aboutText)
                .setView(textView)
                .setPositiveButton("返回",null)
                .show();
    }

    public static boolean isInstallApp(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();// 获取packagemanager
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);// 获取所有已安装程序的包信息
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName.toLowerCase(Locale.ENGLISH);
                if (pn.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }


    //http://hautxsh.iteye.com/blog/1495012
    public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    //https://blog.csdn.net/weixin_33739627/article/details/89690304
    public static void saveBitmapAsPng(Bitmap bmp,File f) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //来自https://blog.csdn.net/meixi_android/article/details/83509210，有修改
    public static Bitmap AddTextToBitmap(Bitmap mBitmap,String text) {
        //获取原始图片与水印图片的宽与高
        int mBitmapWidth = mBitmap.getWidth();
        int mBitmapHeight = mBitmap.getHeight();
        Bitmap mNewBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(mNewBitmap);
        //向位图中开始画入MBitmap原始图片
        mCanvas.drawBitmap(mBitmap,0,0,null);
        //添加文字
        Paint mPaint = new Paint();
        mPaint.setColor(getBitmapColorIsDark(mBitmap)? Color.WHITE:Color.BLACK);
        mPaint.setTextSize(48);
        Typeface font = Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD);
        mPaint.setTypeface(font);
        //水印的位置坐标
        mCanvas.drawText(text, 10,60,mPaint);
        mCanvas.save();//Canvas.ALL_SAVE_FLAG
        mCanvas.restore();

        return mNewBitmap;
    }

    //获取图片字体颜色 黑false 白true
    public static boolean getBitmapColorIsDark (Bitmap bitmap){
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] color = new int[5];
        color[0] = bitmap.getPixel(w / 2, h / 2);
        color[1] = bitmap.getPixel(w / 4, h / 4);
        color[2] = bitmap.getPixel(w / 4, h * 3 / 4);
        color[3] = bitmap.getPixel(w * 3 / 4, h / 4);
        color[4] = bitmap.getPixel(w * 3 / 4, h * 3 / 4);
        int c = 0;
        for (int i = 0; i <= 4; i++) {
            if (isDark(color[i])) c++;
            else c--;
        }
        return c >= 0;
    }

    //判断颜色深浅
    public static boolean isDark(int color) {
        Double r,g,b;
        r = (double) Color.red(color);
        g = (double) Color.green(color);
        b = (double) Color.blue(color);
        return isDark(r,g,b);
    }
    /**
     * 根据RGB值判断 深色与浅色
     * @param r
     * @param g
     * @param b
     * @return
     */
    private static boolean isDark(Double r, Double g, Double b){
        return !(r * 0.299 + g * 0.578 + b * 0.114 >= 192);
    }

    public static void toSelfSetting(Context context) {
        Intent mIntent = new Intent();
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        mIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
        context.startActivity(mIntent);
    }
}
