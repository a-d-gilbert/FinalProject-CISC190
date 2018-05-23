package application;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.videoio.VideoCapture;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * The class for handling window events and data processing for displaying window features
 * @author alexander gilbert
 *
 */
public class Controller {
	
	private Image initImg;
	private Scalar colorMin = new Scalar(0, 0, 0);
	private Scalar colorMax = new Scalar(180, 255, 255);
	private int filter = 0;
	@FXML
	private Button start_btn;
	
	@FXML
	private RadioButton orangeRB, greenRB, faceRB;
	
	@FXML
	private ToggleGroup colorGroup = new ToggleGroup();
	
	@FXML
	private ImageView currentFrame;
	
	private ScheduledExecutorService timer;
	private VideoCapture cap = new VideoCapture();
	private boolean camActive = false;
	private static int camId = 0;
	
	/**
	 * The method for initalizing event listeners and initial window content conditions
	 */
	public void initialize() {
		try {
			initImg = new Image(getClass().getResourceAsStream("/images/init.jpg"));
            currentFrame.setImage(initImg);
		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		
		colorGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
            
            @Override
            public void changed(ObservableValue<? extends Toggle> ov, Toggle oldToggle, Toggle newToggle){
               if(colorGroup.getSelectedToggle() != null){
                  String color = colorGroup.getSelectedToggle().getUserData().toString();
                  System.out.println(color);
                     if(color.equals("green")){
                        colorMin = new Scalar(40, 110, 85);
                        colorMax = new Scalar(85, 255, 255);
                        filter = 1;
                     } else if( color.equals("orange")){
                        colorMin = new Scalar(0, 110, 110);
                        colorMax = new Scalar(35, 255, 255);
                        filter = 1;
                     } else if(color.equals("face")) {
                    	 filter = 2;
                     }
               }
            }
         });
	}
	
	/**
	 * Method for starting the camera feed and calling the various image conversion methods and filters
	 * for displaying the image with features tracked to the user
	 */
	@FXML
	protected void startCamera() {
		
		if(!this.camActive) {
			
			this.cap.open(camId);
			
			if(this.cap.isOpened()) {
				
				this.camActive = true;
				
				Runnable frameGrabber = new Runnable() {
					@Override
					public void run() {
						
						Mat frame = grabFrame();
						Mat eFrame = frame.clone();
						if( filter == 1) {
						eFrame = ColorSegmenter.getEditedFrame(frame, colorMin, colorMax);
						} else if(filter == 2) {
						eFrame = FaceDetector.findAndLabelFaces(frame);
						}
						Image imageToShow = mat2Image(eFrame);
						updateImageView(currentFrame, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				this.start_btn.setText("Stop Camera");
			}
			else {
				System.err.println("Impossible to open camera connection ...");
			}
		}
		else {
			this.camActive = false;
			this.start_btn.setText("Start Camera");
			this.stopAcquisition();
		}
	}
	
	/**
	 * Method for grabbing an image from the camera feed stream
	 * @return returns an image frame as a Mat type object
	 */
	private Mat grabFrame() {
		Mat frame = new Mat();
		
		if(this.cap.isOpened()) {
			try {
				this.cap.read(frame);
			}
			catch(Exception e) {
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	
	/**
	 * A method for stopping the camera from capturing more frames
	 */
	private void stopAcquisition() {
		
		if(this.timer != null && !this.timer.isShutdown()) {
			try {
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch(Exception e) {
				System.err.println("Exception is stopping th eframe capture, trying to release the camera now...");
			}
		}
		
		if(this.cap.isOpened()){
			this.cap.release();
		}
	}
	
	/**
	 * A method for updating the camera feed
	 * @param view the ImageView container from the GUI
	 * @param image the image object to be loaded into the imageView container
	 */
	private void updateImageView(ImageView view, Image image) {
		onFXThread(view.imageProperty(), image);
	}
	
	/**
	 * A method that is called to shutdown camera feed on window close
	 */
	protected void setClosed(){
		this.stopAcquisition();
	}
	
	/**
	 * A method for loading a snapshot of an OpenCV Mat converted to Java Image
	 * @param frame the OpenCV Mat image to be converted
	 * @return returns an object of type Image usable by JavaFX
	 */
	private Image mat2Image(Mat frame) {
		try {
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch(Exception e){
			System.err.println("Cannot convert the Mat to Object:" + e);
			return null;
			
		}
	}
	
	/**
	 * The Method that converts OpenCv Mat to Java Image
	 * @param original Mat to be converted
	 * @return returns Java Image object
	 */
	private BufferedImage matToBufferedImage(Mat original) {
		
		BufferedImage image = null;
		int width = original.width(); 
		int height = original.height();
		int channels = original.channels();
		
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if(channels > 1) {
			image =  new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
	
	/**
	 * A method that sets an object property with a chosen value using the systems thread manager to update
	 * the given property with the given value
	 * @param property an object property
	 * @param value the value to update the objects property with
	 */
	private <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}
}
