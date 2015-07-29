package cn.edu.xmu.buildingdetection;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity implements Callback, PreviewCallback {

	//以下参数为摄像头预览所设参数
	private SurfaceHolder mSurfaceHolder = null;
	private Camera mCamera = null;
	private SurfaceView mSurfaceView = null;
	private boolean bIfPreview = false;
	
	//检测用到的参数
	Mat object = null;
	FeatureDetector detector = null;
	MatOfKeyPoint kp_object = null;
	DescriptorExtractor extractor = null;
	Mat des_object = null;
	DescriptorMatcher matcher = null;
	Mat yuv = null;
	Mat image = null;
	MatOfKeyPoint kp_image = null;
	Mat des_image = null;
	MatOfDMatch matches = null;
	LinkedList<DMatch> good_matches = null;
	LinkedList<Point> obj_list = null;
	LinkedList<Point> scene_list = null;
	MatOfPoint2f obj = null;
	MatOfPoint2f scene = null;
	Mat H = null;
	Mat obj_corners = null;
	Mat scene_corners = null;
	Mat img_matches = null;
	double min_dist = 100;
	
	DisplayView dv = null;
	
	private static boolean stateIsOK = false;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status){
			switch(status){
			case LoaderCallbackInterface.SUCCESS:
				Log.i("Success", "OpenCV loaded successfully");
				initTemplate();
				stateIsOK = true;
				break;
			default:
				Log.i("Fail", "OpenCV loaded failed");
				super.onManagerConnected(status);
				break;
			}
		}
	};
	
	@Override
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		mSurfaceView = new SurfaceView(getApplicationContext());
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		
		dv = new DisplayView(getApplicationContext(), null);
		dv.setVisibility(View.VISIBLE);
		
		FrameLayout fl = new FrameLayout(this);
		fl.addView(mSurfaceView);
		fl.addView(dv);
		setContentView(fl);

	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
	};
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		initCamera();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try{
			mCamera.setPreviewDisplay(mSurfaceHolder);
			
		}catch(Exception e){
			mCamera.release();
			mCamera = null;
			return;
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(mCamera != null){
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			bIfPreview = false;
			mCamera.release();
			mCamera = null;
		}
	};
	
	private void initCamera(){
		if(bIfPreview){
			mCamera.stopPreview();
		}
		if(mCamera != null){
			try{
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setFlashMode("off");
				parameters.setPictureFormat(PixelFormat.JPEG);
				parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
				
				parameters.setPictureSize(1920, 1080);
				parameters.setPreviewSize(1920, 1080);
				
				//横竖屏镜头自动调整
				if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
					parameters.set("orientation", "portrait");
					parameters.set("rotation", 90);
					mCamera.setDisplayOrientation(90);
				}
				else{
					parameters.set("orientation", "landscape");
					mCamera.setDisplayOrientation(0);
				}
				
				mCamera.setParameters(parameters);
				mCamera.setPreviewCallback(this);
				mCamera.startPreview();
				bIfPreview = true;
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	private void initTemplate(){
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root,"template.png");
		object = Highgui.imread(file.getAbsolutePath(),0);
		if(object.empty()){
			Log.i("Template Read", "template read failed");
		}
		
		detector = FeatureDetector.create(FeatureDetector.ORB);
		kp_object = new MatOfKeyPoint();
		detector.detect(object, kp_object);
		des_object = new Mat();
		extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
		extractor.compute(object, kp_object, des_object);
		matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
		
		//初始化一些参数
		image = new Mat();
		kp_image = new MatOfKeyPoint();
		des_image = new Mat();
		
		obj = new MatOfPoint2f();
		scene = new MatOfPoint2f();
		H = new Mat();
		obj_corners = new Mat(4,1,CvType.CV_32FC2);
		scene_corners = new Mat(4,1,CvType.CV_32FC2);
		img_matches = new Mat();
		
		obj_corners.put(0, 0, new double[]{0,0});
		obj_corners.put(1, 0, new double[]{object.cols(),0});
		obj_corners.put(2, 0, new double[]{object.cols(),object.rows()});
		obj_corners.put(3, 0, new double[]{0,object.rows()});
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		if(stateIsOK == true){
			//预览帧处理
			yuv = new Mat(1080+1080/2,1920,CvType.CV_8UC1);
			yuv.put(0, 0, data);
			Imgproc.cvtColor(yuv, image, Imgproc.COLOR_YUV420sp2BGR);
			Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
				
			//特征提取检测
			detector.detect(image, kp_image);
			extractor.compute(image, kp_image, des_image);
			matches = new MatOfDMatch();
			if(des_object.type() == des_image.type() && des_object.cols() == des_image.cols()){
				matcher.match(des_object, des_image, matches);
			}

			good_matches = new LinkedList<DMatch>();
			List<DMatch> matchesList = matches.toList();

			int length = Math.min(des_image.rows()-1, matchesList.size());

			for(int i=0;i<length;i++){
				Double distance = (double) matchesList.get(i).distance;
				if(distance < min_dist)
					min_dist = distance;
			}
			for(int i=0;i<length;i++){
				if(matchesList.get(i).distance < 3 * min_dist){
					good_matches.add(matchesList.get(i));
				}
			}

			if(good_matches.size()>=4){
				obj_list = new LinkedList<Point>();
				scene_list = new LinkedList<Point>();
				for(int i=0;i<good_matches.size();i++){
					obj_list.add(kp_object.toList().get(good_matches.get(i).queryIdx).pt);
					scene_list.add(kp_image.toList().get(good_matches.get(i).trainIdx).pt);
				}
				obj.fromList(obj_list);
				scene.fromList(scene_list);
				H = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 5);
				Core.perspectiveTransform(obj_corners, scene_corners, H);
				
//				Core.line(image, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0), 4);
//				Core.line(image, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0), 4);
//				Core.line(image, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0), 4);
//				Core.line(image, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0), 4);
//				Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2RGBA, 4);
//				
//				image.convertTo(image, CvType.CV_8UC3);
//				Mat mask = new Mat();
//				Rect rect = new Rect(new Point(scene_corners.get(0, 0)), new Point(2, 0));
//				Mat bgdModel = new Mat();
//				Mat fgdModel = new Mat();
//				Imgproc.grabCut(image, mask, rect, bgdModel, fgdModel, 2, Imgproc.GC_INIT_WITH_RECT);
//				Core.convertScaleAbs(mask, mask, 100, 0);
//				Imgproc.cvtColor(mask, mask, Imgproc.COLOR_GRAY2RGBA);
//				
//				Bitmap bm = Bitmap.createBitmap(image.cols(),image.rows(),Bitmap.Config.ARGB_8888);
//				Utils.matToBitmap(mask, bm);
//				dv.drawBox(bm);
				
				dv.drawBox(new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)));
				
			}
			else{
				Log.i("Detection", "Detect Fail!");
			}
			
		}
		
	}
	
}
