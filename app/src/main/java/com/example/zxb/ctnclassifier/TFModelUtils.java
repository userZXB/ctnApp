package com.example.zxb.ctnclassifier;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by superhua on 2018/10/23.
 */

public class TFModelUtils {
    private TensorFlowInferenceInterface inferenceInterface;
    private int[] inputIntData ;
    private float[] inputFloatData ;
    private int inputWH;
    private String inputName;
    private String[] outputNames;
    //存放字典标签
    private Map<Integer,String> dict;

    public TFModelUtils(AssetManager assetMngr,int inputWH,String inputName,String[]outputNames,String modelName){
        this.inputWH=inputWH;
        this.inputName=inputName;
        this.outputNames=outputNames;
        this.inputIntData=new int[inputWH*inputWH];
        this.inputFloatData = new float[inputWH*inputWH*3];
        //从assets目录加载模型
        inferenceInterface= new TensorFlowInferenceInterface(assetMngr, modelName);
        //利用assetMngr获取assets目录下的资源
        this.loadLabel(assetMngr);
    }
    // 模型调用的入口
    public Map<String,Object> run(Bitmap bitmap){
        float[] inputData = getFloatImage(bitmap);

        //将输入数据复制到TensorFlow中,指定输入Shape=[1,INPUT_WH,INPUT_WH,3]
        inferenceInterface.feed(inputName, inputData, 1, inputWH, inputWH, 3);

        // 执行模型
        inferenceInterface.run( outputNames );

        //将输出Tensor对象复制到指定数组中
        int[] classes=new int[3];
        float[] scores=new float[3];
        inferenceInterface.fetch(outputNames[0], classes);
        inferenceInterface.fetch(outputNames[1], scores);
        Map<String,Object> results=new HashMap<>();
        results.put("scores",scores);
        String[] classesLabel = new String[3];
        for(int i =0;i<3;i++){
            int idx=classes[i];
            classesLabel[i]=dict.get(idx);
//            System.out.printf("classes:"+dict.get(idx)+",scores:"+scores[i]+"\n");
        }
        results.put("classes",classesLabel);
        return results;


    }
    //读取Bitmap像素值，并放入到浮点数数组中。归一化到[-1,1]
    private float[] getFloatImage(Bitmap bitmap){
        Bitmap bm = getResizedBitmap(bitmap,inputWH,inputWH);
        bm.getPixels(inputIntData, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
        for (int i = 0; i < inputIntData.length; ++i) {
            final int val = inputIntData[i];
            inputFloatData[i * 3 + 0] =(float) (((val >> 16) & 0xFF)/255.0-0.5)*2;
            inputFloatData[i * 3 + 1] = (float)(((val >> 8) & 0xFF)/255.0-0.5)*2;
            inputFloatData[i * 3 + 2] = (float)(( val & 0xFF)/255.0-0.5)*2 ;
        }
        return inputFloatData;
    }
    //对图像做Resize
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap( bm, 0, 0, width, height, matrix, false);
       // bm.recycle();
        return resizedBitmap;
    }

    private void loadLabel( AssetManager assetManager ) {
        dict=new HashMap<>();
        try {
            InputStream stream = assetManager.open("labels.txt");
            InputStreamReader isr=new InputStreamReader(stream);
            BufferedReader br=new BufferedReader(isr);
            String line;
            while((line=br.readLine())!=null){
                line=line.trim();
                String[] arr = line.split(",");
                if(arr.length!=2)
                    continue;
                int key=Integer.parseInt(arr[0]);
                String value = arr[1];
                dict.put(key,value);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ERROR",e.getMessage());
        }
    }
}
