package cn.edu.xmu.buildingdetection;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DisplayView extends SurfaceView{

	protected SurfaceHolder sh;
	
	public DisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		sh = getHolder();  
        sh.setFormat(PixelFormat.TRANSPARENT); // 设置为透明 
        setZOrderOnTop(true);// 设置为顶端
	}

	public void drawBox(Bitmap bm){
		Canvas canvas = sh.lockCanvas();
		canvas.drawColor(Color.TRANSPARENT);
		
		Paint p = new Paint();
		p.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		canvas.drawPaint(p);
		p.setXfermode(new PorterDuffXfermode(Mode.SRC));
		p.setAntiAlias(true);
		p.setColor(Color.MAGENTA);
		p.setStrokeWidth(6);
		p.setStyle(Style.FILL_AND_STROKE);
		
		canvas.drawBitmap(bm, 0, 0, p);
		
		sh.unlockCanvasAndPost(canvas);
	}

	public void drawBox(Point p1, Point p2, Point p3, Point p4){
		final float p1_y = 1080-(float)p1.y;
		final float p1_x = (float)p1.x;
		final float p2_y = 1080-(float)p2.y;
		final float p2_x = (float)p2.x;
		final float p3_y = 1080-(float)p3.y;
		final float p3_x = (float)p3.x;
		final float p4_y = 1080-(float)p4.y;
		final float p4_x = (float)p4.x;
		
		(new Thread(new Runnable() {
			
			@Override
			public void run() {
				Canvas canvas = sh.lockCanvas(null);
				canvas.drawColor(Color.TRANSPARENT);
				
				Paint p = new Paint();
				p.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
				canvas.drawPaint(p);
				p.setXfermode(new PorterDuffXfermode(Mode.SRC));
				p.setAntiAlias(true);
				p.setColor(Color.MAGENTA);
				p.setStrokeWidth(6);
				p.setStyle(Style.FILL_AND_STROKE);
				
				canvas.drawLine(p1_y, p1_x, p2_y, p2_x, p);
				canvas.drawLine(p2_y, p2_x, p3_y, p3_x, p);
				canvas.drawLine(p3_y, p3_x, p4_y, p4_x, p);
				canvas.drawLine(p4_y, p4_x, p1_y, p1_x, p);
				
//				canvas.drawLine(Math.abs(((p1_x-p4_x)/2)), p1_y+Math.abs(((p1_y-p4_y)/2)), 0, 960, p);
				
				sh.unlockCanvasAndPost(canvas);
			}
		})).start();
		
	}
	
	public void drawMask(Mat img,Mat mask,org.opencv.core.Rect rect){
		
		
        
	}
	
}
