package com.example.zxb.ctnclassifier;

import android.Manifest;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class pictureActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_IMAGE=1;
    private ImageView picture;
    private TextView resultTv;
    private Button addBtn;
    private static final String MODEL_NAME="frozen.pb";
    private static final String INPUT_NAME="input";
    private static final String[] OUTPUT_NAMES={"classes","scores"};
    private static final int INPUT_WH=224;
    private TFModelUtils utils;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        picture = findViewById(R.id.imageView);
        resultTv=findViewById(R.id.result);
        addBtn=findViewById(R.id.add);
        // 获取assets目录下的资源
        AssetManager assetManager = getAssets();
        utils = new TFModelUtils(assetManager,INPUT_WH,INPUT_NAME,OUTPUT_NAMES,MODEL_NAME);
        if(ContextCompat.checkSelfPermission(pictureActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(pictureActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE_PICK_IMAGE);
        }
        /*果然按照正常逻辑处理是正常的。*/
       else
           openAlbum();
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                  openAlbum();
            }
        });

    }

    public void openAlbum(){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,REQUEST_CODE_PICK_IMAGE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_IMAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                }
                else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
    /*获得数据的回调函数*/
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode,Intent data){
        if(Build.VERSION.SDK_INT >= 19)
            handleImageOnKitKat(data);
        else
            handleImageBeforeKitKat(data);
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleImageOnKitKat(Intent data){
        String imagePath = null;
        Uri uri = data.getData();
        Log.d("TAG", "handleImageOnKitKat:uri is"+uri);
        if(DocumentsContract.isDocumentUri(this, uri)){
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            }
            else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        }
        else if("content".equalsIgnoreCase(uri.getScheme()))
            imagePath = getImagePath(uri,null);
        else if("file".equalsIgnoreCase(uri.getScheme()))
            imagePath = uri.getPath();
        displayImage(imagePath);
    }
    private void handleImageBeforeKitKat(Intent data){
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }
    private String getImagePath(Uri uri,String selection){
        String path = null;
        Cursor cursor  = getContentResolver().query(uri,null,selection,null, null);
        if(cursor!=null){
            if(cursor.moveToFirst())
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
        }
        return path;
    }
    private void displayImage(String imagePath){
        if(imagePath!= null){
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(bitmap);
            Log.d("bitmap", "bitmap is"+bitmap);
            /*问题出现在getResult函数中，未recyle*/
            getResult(bitmap);
            if (bitmap != null && !bitmap.isRecycled())
            {
                bitmap=null;
            }
        }
        else
            Toast.makeText(this,"failed to get image", Toast.LENGTH_SHORT).show();
    }
    private void getResult(Bitmap bitmap){
        Map out = utils.run(bitmap);
        String[] labels=(String[]) out.get("classes");
        float[] scores=(float[]) out.get("scores");
        String s="";
        for(int i=0;i<3;i++){
            s=s+"类别"+labels[i]+", 识别概率"+scores[i]+"\n";
        }
        resultTv.setText(s);
    }

}
