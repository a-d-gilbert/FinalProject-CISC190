package application;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
// import java.awt.image.BufferedImage;
// import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
// import org.opencv.core.MatOfInt4;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
// import org.opencv.videoio.VideoCapture;

public class ColorSegmenter {
	
	/**
	 * A method for defining the edges of objects that are within the range of colors wanted
	 * @param f the image to filter
	 * @return returns the filtered image as a Mat
	 */
	public static Mat edgeDefiner(Mat im, Scalar min, Scalar max) {
		Mat eImg = new Mat();
		
		Imgproc.cvtColor(im, im, Imgproc.COLOR_BGR2HSV);
		Core.inRange(im,min,max, im);
		//Core.inRange(f,new Scalar(98,109,20),new Scalar(112,255,255), f);//blue
		Imgproc.erode(im, im, Imgproc.getStructuringElement(0, new Size(3, 3)));
		Imgproc.dilate(im, im, Imgproc.getStructuringElement(0, new Size(3, 3)));
		Imgproc.GaussianBlur(im, im, new Size(3, 3), 1.5);
		Imgproc.Canny(im, eImg, 100, 200);
		
		return eImg;
	}
	
	/**
	 * A method for filtering out contours below a certain size
	 * @param image the image to with contours to be filtered must be binarized image
	 * @return returns a List<MatOfPoint> containing the items with area > threshold
	 */
	public static List<MatOfPoint> getContoursBySize(Mat im, double sThresh){
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> filteredContours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		double sizeThreshold = sThresh;
		
		Imgproc.findContours(im, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		Iterator<MatOfPoint> iter = contours.iterator();
		
		while(iter.hasNext()) {
			MatOfPoint contour = iter.next();
			double contourArea = Imgproc.contourArea(contour);
			
			if(contourArea > sizeThreshold) {
				filteredContours.add(contour);
			}
			
		}
		
		return filteredContours;
	}
	
	/**
	 * Gets center point of contours of a detected object using spacial moment analysis
	 * @param contour a MatOfPoint type object that contains the contour of the object
	 * @return a Point type object representing the center that is calculated from the spatial moments of the contour
	 */
	public static Point getCenterPoint(MatOfPoint cont){
		Point p = new Point();
		
		Moments m = Imgproc.moments(cont);
		p = ( new Point((m.get_m10()/m.get_m00()),(m.get_m01()/m.get_m00())));
				
		return p;
	}
	
	/**
	 * this is a method that uses the number of vertices a shape has to classify its shape
	 * @param numV the number of vertices a set of contours has
	 * @return return a string describing the shape of the contours
	 */
	public static String labelShapes(int numV) {
		String shape = "";
		
		if(numV == 3) {
			shape = "Triangle";
		} else if(numV == 4) {
			shape = "Rectangle";
		} else if(numV >= 8) {
			shape = "Circle";
		} else {
			shape = "Unknown";
		}
		
		return shape;
	}
	
	/**
	 * This method gets the number of vertices a set of contours contains
	 * @param cont the contours to check for vertices
	 * @return returns an integer value representing the number of vertices
	 */
	public static int getVertices(MatOfPoint cont) {
		MatOfPoint2f aprox = new MatOfPoint2f();
		int numOfVertices = 0;
		double epsilon = 0.02 * Imgproc.arcLength(new MatOfPoint2f(cont.toArray()) , true);
		Imgproc.approxPolyDP(new MatOfPoint2f(cont.toArray()), aprox, epsilon, true);
		numOfVertices = aprox.toList().size();
		
		return numOfVertices;
	}
	
	/**
	 * this method draws the center points and labels the shape of the objects tracked in the image
	 * @param frame the image to draw on
	 * @param cs the list of contours to label and draw on the image
	 */
	public static void drawCenterPointsAndLabelShapes(Mat im, List<MatOfPoint> conts) {
		int nv = 0;
		
		Iterator<MatOfPoint> iter = conts.iterator();
		while(iter.hasNext()) {
			MatOfPoint contour = iter.next();
			Point cent = getCenterPoint(contour);
			nv = getVertices(contour);
			Imgproc.putText(im, labelShapes(nv), cent, Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(0));
			Imgproc.circle(im, cent, 10, new Scalar(255));
		}
	}
	
	/**
	 * this method creates the final edited image after all contours are drawn and shapes are labeled
	 * @return the edited image
	 */
	public static Mat getEditedFrame(Mat im, Scalar min, Scalar max) {
		Mat oImg = im.clone();
		Mat eImg = edgeDefiner(im, min, max);
		List<MatOfPoint> contours = getContoursBySize(eImg, 500); 
		Imgproc.drawContours(oImg, contours, -1, new Scalar(255), 5);
		drawCenterPointsAndLabelShapes(oImg, contours);
		
		return oImg;
	}
}
