/* 
 * I developed some code for recognize motion detections with JavaCV.
 * Actually, it works with an array of Rect, performing, every cicle, an
 * intersection test with area of difference with the rect of interests
 * (this array is callad "sa", stands for SizedArea). I hope could this
 * helps someone.
 * 
 * Feel free to ask about any question regarding the code above, cheers!
 *
 * Angelo Marchesin <marchesin.angelo@gmail.com>
 */

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.*;

import javax.swing.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;

public class MotionDetector {
    public static void main(String[] args) throws Exception {
        final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(-1);
        grabber.start();

        IplImage frame = grabber.grab();
        IplImage image = null;
        IplImage prevImage = null;
        IplImage diff = null;

        CanvasFrame canvasFrame = new CanvasFrame("Motion");
        canvasFrame.setCanvasSize(64, 80);

        JFrame f = new JFrame();
        f.setUndecorated(true);
        //f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    grabber.stop();
                    System.exit(0);
                } catch (FrameGrabber.Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        f.setSize(64, 80);
        f.add(canvasFrame.getContentPane());
        f.setVisible(true);

        /*JFrame f = new JFrame();
        f.setUndecorated(true);
        //f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(64, 80);
        f.add(canvasFrame.getContentPane());*/

        CvMemStorage storage = CvMemStorage.create();

        while (canvasFrame.isVisible() && (frame = grabber.grab()) != null) {
            IplImage cropped = cvCreateImage(cvSize(64, 80), frame.depth(), frame.nChannels());
            //IplImage cropped; //cvCropMiddle(frame, width * one.getValue(), height * 2);
            cvResize(frame, cropped);
            frame = cropped;
            //frame = cvCropMiddle(frame, 64, 80);
            cvSmooth(frame, frame, CV_GAUSSIAN, 9, 9, 2, 2);
            if (image == null) {
                image = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
                cvCvtColor(frame, image, CV_RGB2GRAY);
            } else {
                prevImage = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
                prevImage = image;
                image = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
                cvCvtColor(frame, image, CV_RGB2GRAY);
            }

            if (diff == null) {
                diff = IplImage.create(frame.width(), frame.height(), IPL_DEPTH_8U, 1);
            }

            if (prevImage != null) {
                // perform ABS difference
                cvAbsDiff(image, prevImage, diff);
                // do some threshold for wipe away useless details
                cvThreshold(diff, diff, 20, 255, CV_THRESH_BINARY);

                canvasFrame.showImage(diff);

                // recognize contours
                CvSeq contour = new CvSeq(null);
                cvFindContours(diff, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

                while (contour != null && !contour.isNull()) {
                    if (contour.elem_size() > 0) {
                        CvBox2D box = cvMinAreaRect2(contour, storage);
                        // test intersection
                        if (box != null) {
                            CvPoint2D32f center = box.center();
                            CvSize2D32f size = box.size();
/*                            for (int i = 0; i < sa.length; i++) {
                                if ((Math.abs(center.x - (sa[i].offsetX + sa[i].width / 2))) < ((size.width / 2) + (sa[i].width / 2)) &&
                                    (Math.abs(center.y - (sa[i].offsetY + sa[i].height / 2))) < ((size.height / 2) + (sa[i].height / 2))) {

                                    if (!alarmedZones.containsKey(i)) {
                                        alarmedZones.put(i, true);
                                        activeAlarms.put(i, 1);
                                    } else {
                                        activeAlarms.remove(i);
                                        activeAlarms.put(i, 1);
                                    }
                                    System.out.println("Motion Detected in the area no: " + i +
                                            " Located at points: (" + sa[i].x + ", " + sa[i].y+ ") -"
                                            + " (" + (sa[i].x +sa[i].width) + ", "
                                            + (sa[i].y+sa[i].height) + ")");
                                }
                            }
*/
                        }
                    }
                    contour = contour.h_next();
                }
            }
        }
        grabber.stop();
        canvasFrame.dispose();
    }

    private static IplImage cvCrop(IplImage frame, int x, int y, int width, int height) {
        IplImage cropped = cvCreateImage(cvSize(width, height), IPL_DEPTH_8U, 3);
        cvSetImageROI(frame, cvRect(x, y, width, height));
        cvCopy(frame, cropped);
        cvResetImageROI(frame);
        return cropped;
    }

    private static IplImage cvCropMiddle(IplImage image, int width, int height) {
        return cvCrop(image, ((image.width() / 2) - (width / 2)), ((image.height() / 2) - (height / 2)), width, height);
    }
}
