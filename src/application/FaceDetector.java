package application;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.Objdetect;

public class FaceDetector {
	
	private static CascadeClassifier frontClassifier = new CascadeClassifier("src\\resources\\haarcascade_frontalface_alt.xml");
	private static CascadeClassifier profileClassifier = new CascadeClassifier("src\\resources\\haarcascade_profileface.xml");
	private static CascadeClassifier eyeClassifier = new CascadeClassifier("src\\resources\\haarcascade_eye.xml");
	
	public static CascadeClassifier getFrontalHaarClassifier() {
		CascadeClassifier haarCascClass = new CascadeClassifier();
		haarCascClass.load("src\\resources\\haarcascade_frontalface_alt.xml");
		return haarCascClass;
	}
	
	public static CascadeClassifier getProfileHaarClassifier() {
		CascadeClassifier haarCascClass = new CascadeClassifier();
		haarCascClass.load("src\\resources\\haarcascade_profileface.xml");
		return haarCascClass;
	}
	
	private static Mat prepareImage(Mat im) {
		Mat gry = new Mat();
		Imgproc.cvtColor(im, gry, Imgproc.COLOR_BGR2GRAY );
		Imgproc.equalizeHist(gry, gry);
		return gry;
	}
	
	private static int getAbsoluteFaceSize(Mat im) {
		int absFaceSize = 0;
		
		if(absFaceSize == 0) {
			int height = im.rows();
			if(Math.round(height * 0.2f) > 0) {
				absFaceSize = Math.round(height * 0.2f);
			}
		}
		
		return absFaceSize;
	}
	
	public static Mat findAndLabelFaces(Mat im) {
		Rect[] facesArray;
		Mat ret = im.clone();
		Mat eIm = im.clone();
		MatOfRect frontFaces = new MatOfRect();
		MatOfRect profileFaces = new MatOfRect();
		MatOfRect eyes = new MatOfRect();
		int arrayToUse = 0;
		eIm = prepareImage(eIm);
		int absFS = getAbsoluteFaceSize(im);
		frontClassifier.detectMultiScale(eIm, frontFaces, 1.2, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(absFS, absFS), new Size());
		profileClassifier.detectMultiScale(eIm, profileFaces, 1.2, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(absFS, absFS), new Size());
		eyeClassifier.detectMultiScale(eIm, eyes, 1.2, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(absFS, absFS), new Size());
		
		if(!frontFaces.empty()) {
			facesArray = frontFaces.toArray();
			for(Rect r : facesArray) {
				Imgproc.rectangle(ret, r.tl(), r.br(), new Scalar(255), 3);
			}
			System.out.println("front");
		} else if(!profileFaces.empty()) {
			facesArray = profileFaces.toArray();
			for(Rect r : facesArray) {
				Imgproc.rectangle(ret, r.tl(), r.br(), new Scalar(255), 3);
			}
			System.out.println("profile");
		} else if(!eyes.empty()) {
			facesArray = eyes.toArray();
			for(Rect r : facesArray) {
				Imgproc.rectangle(ret, r.tl(), r.br(), new Scalar(255), 3);
			}
			System.out.println("eyes");
		} else {
			System.out.println("No Faces Found");
		}
		
		return ret;
	}
}
