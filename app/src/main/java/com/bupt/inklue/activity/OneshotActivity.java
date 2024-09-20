package com.bupt.inklue.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
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

import com.bupt.data.pojo.HanZi;
import com.bupt.evaluate.core.Evaluation;
import com.bupt.evaluate.core.Evaluator;
import com.bupt.inklue.R;
import com.bupt.inklue.util.BitmapHelper;
import com.bupt.inklue.util.DirectoryHelper;
import com.bupt.inklue.util.ResourceHelper;
import com.bupt.preprocess.Preprocessor;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.core.Scalar;

import java.io.File;
import java.util.concurrent.ExecutionException;

//单拍页面
public class OneshotActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnTouchListener {

    private Context context;//环境
    private HanZi hanZi;//汉字数据
    private ImageButton button_shot;//拍照按钮
    private ImageButton button_torch;//手电筒按钮
    private ImageButton button_again;//重拍按钮
    private TextView textview_score;//评分文本框
    private TextView textview_advice;//建议文本框
    private PreviewView preview_view;//相机预览视图
    private ImageView imageview_top;//预览视图上层的视图
    private ImageCapture imageCapture;//图像捕捉器，用于拍照
    private CameraControl cameraControl;//相机控制器，用于对焦
    private Bitmap stdBitmap;//标准汉字半透明图像
    private boolean canShot = true;//是否可以拍照
    private boolean isTorchOn = false;//手电筒是否开启

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏系统顶部状态栏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_oneshot);
        context = this;

        //取得视图
        button_shot = findViewById(R.id.button_shot);
        button_torch = findViewById(R.id.button_torch);
        button_again = findViewById(R.id.button_again);
        textview_score = findViewById(R.id.textview_score);
        textview_advice = findViewById(R.id.textview_advice);
        preview_view = findViewById(R.id.preview_view);
        imageview_top = findViewById(R.id.imageview_top);

        //取得汉字数据
        hanZi = (HanZi) getIntent().getSerializableExtra(getString(R.string.han_zi_bundle));

        //初始化相机
        initCamera();

        //初始化相机预览
        initPreview();

        //设置预览框的触摸监听器
        preview_view.setOnTouchListener(this);

        //设置按钮的点击监听器
        findViewById(R.id.button_back).setOnClickListener(this);
        button_shot.setOnClickListener(this);
        button_torch.setOnClickListener(this);
        button_again.setOnClickListener(this);
    }

    //点击事件回调
    public void onClick(View view) {
        if (view.getId() == R.id.button_back) {
            finish();
        } else if (view.getId() == R.id.button_shot) {
            if (canShot) {
                takePhoto();
                canShot = false;
            }
        } else if (view.getId() == R.id.button_torch) {
            if (isTorchOn) {
                cameraControl.enableTorch(false);
                button_torch.setImageResource(R.drawable.ic_torch_on);
                isTorchOn = false;
            } else {
                cameraControl.enableTorch(true);
                button_torch.setImageResource(R.drawable.ic_torch_off);
                isTorchOn = true;
            }
        } else if (view.getId() == R.id.button_again) {
            canShot = true;
            //隐藏重拍按钮
            button_again.setVisibility(View.GONE);
            //显示拍照按钮
            button_shot.setVisibility(View.VISIBLE);
            //重置预览上层视图
            imageview_top.setImageBitmap(stdBitmap);
            //清空评分和建议
            textview_score.setText("");
            textview_advice.setText("");
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

    //初始化相机预览
    private void initPreview() {
        //设置相机预览宽高
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int topBarHeight = getResources().getDimensionPixelSize(R.dimen.top_bar);
        int bottomBarHeight = getResources().getDimensionPixelSize(R.dimen.camera_bottom_bar);
        int size = Math.min(screenWidth, screenHeight - topBarHeight - bottomBarHeight);
        RelativeLayout preview_layout = findViewById(R.id.layout_preview);
        preview_layout.getLayoutParams().width = size;
        preview_layout.getLayoutParams().height = size;
        //关闭硬件加速
        imageview_top.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //获取绘制标准汉字的颜色
        Scalar color = ResourceHelper.getScalar(this, R.attr.colorTheme);
        //取得标准汉字半透明图像
        stdBitmap = BitmapHelper.toTransparent(hanZi.getPath(), color);
        imageview_top.setImageBitmap(stdBitmap);
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
        String writtenPath = DirectoryHelper.generateCacheJPG(this);
        File photoFile = new File(writtenPath);
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @SuppressLint("SetTextI18n")
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        hanZi.setWrittenPath(writtenPath);
                        //预处理图像并保存
                        Bitmap bitmapWritten = BitmapFactory.decodeFile(writtenPath);
                        Bitmap bitmapStd = BitmapFactory.decodeFile(hanZi.getPath());
                        Bitmap bitmapProc = Preprocessor.preprocess(bitmapWritten, bitmapStd);
                        BitmapHelper.saveBitmap(bitmapProc, writtenPath);
                        //取得评价所需数据
                        String name = hanZi.getName();
                        String className = hanZi.getCode();
                        Bitmap inputBmp = BitmapFactory.decodeFile(hanZi.getWrittenPath());
                        Bitmap stdBmp = BitmapFactory.decodeFile(hanZi.getPath());
                        //调用评价模块
                        Evaluation evaluation = Evaluator.evaluate(name, className, inputBmp, stdBmp);
                        //显示评价结果
                        imageview_top.setImageBitmap(evaluation.outputBmp);
                        textview_score.setText(Integer.toString(evaluation.score));
                        textview_advice.setText(evaluation.advice);
                        //隐藏拍照按钮
                        button_shot.setVisibility(View.INVISIBLE);
                        //显示重拍按钮
                        button_again.setVisibility(View.VISIBLE);
                    }

                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(context, R.string.camera_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
