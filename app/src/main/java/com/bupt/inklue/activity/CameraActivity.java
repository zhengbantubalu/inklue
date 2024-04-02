package com.bupt.inklue.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.bupt.inklue.R;
import com.bupt.inklue.data.CardData;
import com.bupt.inklue.data.CardsData;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

//拍照页面
public class CameraActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnTouchListener {

    private Context context;//环境
    private CardsData imageCardsData;//图像卡片数据
    private int position = 0;//当前拍摄的汉字在卡片列表中的位置
    private PreviewView preview_view;//相机预览视图
    private ImageView imageview_above;//预览视图上层的视图
    private ImageCapture imageCapture;//图像捕捉器，用于拍照
    private CameraControl cameraControl;//相机控制器，用于对焦
    private Scalar edgeColor;//绘制汉字边缘的颜色

    @SuppressWarnings("unchecked")//忽略取得图像卡片数据时类型转换产生的警告
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏系统顶部状态栏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        context = this;

        //取得视图
        preview_view = findViewById(R.id.preview_view);
        imageview_above = findViewById(R.id.imageview_top);

        //加载OpenCV
        System.loadLibrary("opencv_java3");

        //取得图像卡片数据
        imageCardsData = new CardsData((ArrayList<CardData>)
                (getIntent().getSerializableExtra("imageCardsData")));

        //初始化相机
        initCamera();

        //初始化预览上层视图
        initImageView();

        //设置预览框的触摸监听器
        preview_view.setOnTouchListener(this);

        //设置按钮的点击监听器
        findViewById(R.id.button_back).setOnClickListener(this);
        findViewById(R.id.button_shot).setOnClickListener(this);
    }

    //点击事件回调
    public void onClick(View view) {
        if (view.getId() == R.id.button_back) {
            finish();
        } else if (view.getId() == R.id.button_shot) {
            takePhoto();
        }
    }

    //触摸事件回调
    @SuppressLint("ClickableViewAccessibility")//触摸事件不认为是点击事件
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            SurfaceOrientedMeteringPointFactory factory =
                    new SurfaceOrientedMeteringPointFactory(
                            preview_view.getHeight(), preview_view.getWidth());
            if (view.getId() == R.id.preview_view) {
                FocusMeteringAction action = new FocusMeteringAction.Builder(
                        factory.createPoint(event.getY(), event.getX())).build();
                cameraControl.startFocusAndMetering(action);
            }
        }
        return true;
    }

    //启动确认页面
    private void startConfirmActivity() {
        Intent intent = new Intent();
        intent.setClass(this, ConfirmActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("imageCardsData", imageCardsData);
        bundle.putString("practiceName", getIntent().getStringExtra("practiceName"));
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    //初始化预览上层视图
    private void initImageView() {
        //关闭硬件加速
        imageview_above.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //获取绘制汉字边缘的颜色
        TypedValue typedValue = new TypedValue();
        this.getTheme().resolveAttribute(R.attr.colorTheme, typedValue, true);
        int colorResId = typedValue.resourceId;
        int color = ContextCompat.getColor(this, colorResId);
        edgeColor = new Scalar(Color.red(color), Color.green(color), Color.blue(color));
        //更新预览上层视图
        updateImageView();
    }

    //更新预览上层视图
    private void updateImageView() {
        if (position == imageCardsData.size()) {
            startConfirmActivity();
        }
        Bitmap bitmap = BitmapFactory.decodeFile(imageCardsData.get(position).getStdImgPath());
        Mat alpha = new Mat();
        Utils.bitmapToMat(bitmap, alpha);
        Imgproc.cvtColor(alpha, alpha, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(alpha, alpha, 50, 150);
        Imgproc.threshold(alpha, alpha, 127, 255, Imgproc.THRESH_BINARY);
        List<Mat> channels = new ArrayList<>();
        channels.add(new Mat(alpha.size(), CvType.CV_8UC3, edgeColor));
        channels.add(alpha);
        Mat rgba = new Mat();
        Core.merge(channels, rgba);
        Utils.matToBitmap(rgba, bitmap);
        imageview_above.setImageBitmap(bitmap);
    }

    //初始化相机
    private void initCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder().
                        requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
                preview.setSurfaceProvider(preview_view.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();
                cameraControl = cameraProvider.bindToLifecycle(
                                this, cameraSelector, preview, imageCapture).
                        getCameraControl();
            } catch (ExecutionException | InterruptedException ignored) {
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //拍照
    private void takePhoto() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault());
        String time = sdf.format(new Date());
        String writtenImgPath = getExternalCacheDir() + "/" + time + ".jpg";
        File photoFile = new File(writtenImgPath);
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        imageCardsData.get(position).setWrittenImgPath(writtenImgPath);
                        position++;
                        updateImageView();//更新预览上层视图
                    }

                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
