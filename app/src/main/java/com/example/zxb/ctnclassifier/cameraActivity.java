package com.example.zxb.ctnclassifier;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;


public class cameraActivity extends AppCompatActivity  implements Camera.PreviewCallback{

    private static final String MODEL_NAME="frozen.pb";
    private static final String INPUT_NAME="input";
    private static final String[] OUTPUT_NAMES={"classes","scores"};
    private static final int INPUT_WH=224;

    private Camera camera;
    private CameraPreview preview;
    private TFModelUtils utils;
    private TextView outputTv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 保存当前Activity的状态信息，可以通过保存这些信息，让用户感觉和之前的界面一样，提升用户体验
        super.onCreate(savedInstanceState);
        // 设置窗体全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        // 设置窗体内容
        setContentView(R.layout.activity_camera);
        outputTv=findViewById(R.id.output);
        // 获取assets目录下的资源
        AssetManager assetManager = getAssets();
        utils = new TFModelUtils(assetManager,INPUT_WH,INPUT_NAME,OUTPUT_NAMES,MODEL_NAME);
        // Bitmap test = getBitmapFromAsset("test.jpg");
        if(ContextCompat.checkSelfPermission(cameraActivity.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(cameraActivity.this,new String[]{Manifest.permission.CAMERA},1);
        }
        /*果然按照正常逻辑处理是正常的。*/
        else
           openCamera();
    }
    public void openCamera(){
        camera = getCameraInstance();
        if(camera!=null) {
            // 传递预览布局context和回调函数onPreviewFrame，this表示此对象
            preview = new CameraPreview(this, camera,this);
            FrameLayout fl = findViewById(R.id.camera_preview);
            fl.addView(preview);
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(cameraActivity.this);
            builder.setTitle("错误");
            builder.setMessage("无法获取相机，请检查是否开启相机权限！");
            builder.show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }
                else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
    /** A safe way to get an instance of the Camera object.这里需要询问开启相机权限*/
    public Camera getCameraInstance(){
        Camera c = null;
        try{
                c = Camera.open(); // attempt to get a Camera instance
                c.setDisplayOrientation(90);//android的摄像camera天生是横的
            }
            catch (Exception e){
                // Camera is not available (in use or does not exist)
                e.printStackTrace();
            }
        return c; // returns null if camera is unavailable
    }

    /* onPreviewFrame是camera的回调函数，重写了*/
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Bitmap bm = byte2bitmap(bytes,camera);
        Map out = utils.run(bm);
        String[] labels=(String[]) out.get("classes");
        float[] scores=(float[]) out.get("scores");
        String s="";
        for(int i=0;i<3;i++){
            s=s+"类别"+labels[i]+", 识别概率"+scores[i]+"\n";
        }
        outputTv.setText(s);
    }

    private Bitmap byte2bitmap(byte[] bytes, Camera camera) {
        Bitmap bitmap = null;
        Camera.Size size = camera.getParameters().getPreviewSize(); // 获取预览大小
        final int w = size.width; // 宽度
        final int h = size.height;
        //应该是解码过程
        final YuvImage image = new YuvImage(bytes, ImageFormat.NV21, w, h,
                null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(bytes.length);
        if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
            return null;
        }
        byte[] tmp = os.toByteArray();
        bitmap = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
        //虽然预览的图片是正常的，但拿到的bitmap和预览的仍有90的旋转角度
        Matrix matrix = new Matrix();
        matrix.setRotate(-90);
        bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return bitmap;
    }

}
