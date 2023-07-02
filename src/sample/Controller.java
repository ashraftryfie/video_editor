package sample;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Position;
import net.coobird.thumbnailator.geometry.Positions;
import org.controlsfx.control.RangeSlider;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static sample.Main.primaryStage;


public class Controller implements Initializable {

    @FXML
    public VBox vBox;
    public MenuItem open, extractFrames, deleteFrame, saveVideo,
            divideVideo, merge2Videos, cutOrRemove, createVideo;
    public MenuItem textWatermark, imageWatermark, videoWatermark;
    public MediaView mediaView;
    public Slider theVolumeSlider, progressSlider;
    public Label volumeLbl;
    public Label lblMediaTitle;
    public Label currentTimeLabel;
    public Label totalTimeLabel;
    public Label fullscreenLabel;
    public String cutFrom;
    public String cutTo;
    public Label moveTo;
    public HBox hBoxVolume, hBoxControls, framesHBox;
    public ScrollPane scrollPane;
    public ImageView frameView;

    @FXML
    public RangeSlider range;

    private ImageView ivVolume,
            ivMute,
            ivFullScreen,
            ivExit;

    boolean logoEnabled = false,
            atEndOfVideo = false,
            isPlaying = true,
            isMuted = true;

    double videoSpeed = 1;
    int numFrames = 60;
    int frame_number_all;

    Media media;
    MediaPlayer mediaPlayer;

    String selectedImage = null, selectedImageID = "0";

    /******************* Paths *******************/
    String ICONS_URL = "src/sample/icons/";
    String SNAPSHOTS_URL = "snapshots/";
    String MEDIA_URL = "media/vid1.mp4";
    String FRAMES_URL = "src/sample/frames/";
    String FRAMES_TEMP_URL = "src/sample/tempfolder/";
    String WATERMARK_URL = null;
    String SAVING_URL = "media/saved/";
    String SAVING_URL1 = "media/save1/";
    String SAVING_URL2 = "media/save2/";


    @Override
    public void initialize(URL url, ResourceBundle rb) {

        deleteDirectoryFiles(new File(FRAMES_URL));
        deleteDirectoryFiles(new File(FRAMES_TEMP_URL));

        frameView.setOnMouseClicked(e -> {

            chooseWaterMark();

            int x = (int) e.getX();
            int y = (int) e.getY();
            Point p = new Point(x, y);

            System.out.println(x);
            System.out.println(y);

            File path = new File(FRAMES_URL); // base path of the images

            // load source images
            BufferedImage image = null;
            try {
                image = ImageIO.read(new File(path, selectedImageID + ".jpg"));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            BufferedImage overlay = null;
            try {
                overlay = ImageIO.read(new File(WATERMARK_URL));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }


            int xx = (int) (x * image.getWidth()) / 465;
            int yy = (int) (y * image.getHeight()) / 260;


            // create the new image, canvas size is the max. of both image sizes
            int w = Math.max(image.getWidth(), overlay.getWidth());
            int h = Math.max(image.getHeight(), overlay.getHeight());
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            // paint both images, preserving the alpha channels
            Graphics g = combined.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.drawImage(overlay, xx, yy, null);

            g.dispose();

            // Save as new image
            try {
                System.out.println(FRAMES_URL + selectedImageID + ".jpg");

                ImageIO.write(combined, "png", new File(FRAMES_URL + selectedImageID + ".jpg"));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            try {
                copyDirectory(new File(FRAMES_URL), new File(FRAMES_TEMP_URL));

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            save2(SAVING_URL,
                    29,
                    700,
                    700);
        });

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        /**************** Open an Initial video ****************/
        if (new File(MEDIA_URL).exists())
            setup(new File(MEDIA_URL));
        else
            openVideo();

        /**************** Control Play/Pause using key pressed ****************/
        keyPressed();

        /**************** Get Video Frames for Slider ****************/
        extractFrames.setOnAction(e -> {
            // getVideoFrames(MEDIA_URL, FRAMES_URL);
            // OR
            getVideoFrames2(MEDIA_URL, FRAMES_URL);

            viewFramesInSlider(numFrames);

            try {
                copyDirectory(new File(FRAMES_URL), new File(FRAMES_TEMP_URL));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        /**************** View Frames Slider ****************/
        if (isDirContainFiles(FRAMES_URL)) {
            viewFramesInSlider(numFrames);
        } else {
            System.out.println("There is no Frames to show");
            // hide slider until get video frames
            // or get video frames then calls viewFramesInSlider(30);
        }

        /**************** Save Video ****************/
        saveVideo.setOnAction(e -> openFrameInputDialog());

//        /**************** Divide Video ****************/
//        divideVideo.setOnAction(e -> save4("src/sample/"));

        /**************** Add Watermark ****************/
        addWaterMarks();

        /**************** Choose a video to open it ****************/
        open.setOnAction(e -> openVideo());

        /**************** Delete Selected Video Frame ****************/
        deleteFrame.setOnAction(e -> removeFrame());

        ChangeListener<Scene> initializer = new ChangeListener<>() {

            @Override
            public void changed(ObservableValue<? extends Scene> obs, Scene oldScene, Scene newScene) {

                if (newScene != null) {

                    //range.applyCss();

                    range.getParent().layout();

                    //Pane thumb = (Pane) range.lookup(".range-slider .low-thumb");

                    //System.out.println(thumb); // <-- No longer null

                    range = new RangeSlider(0, 200, 0, 100);
                    range.sceneProperty().removeListener(this);

                }

            }

        };

        //range.sceneProperty().addListener(initializer);

        /**************** Cut/Remove Video ****************/
        cutOrRemove.setOnAction(e -> {
            String[] str = getInputDialogCut();
            if (str[2].equals("cut"))
                cutVideo();
            else
                cutOutOfVideo();
        });

        /**************** Seek a part of Video ****************/
        // cutMoveVideo();

        /**************** Merge 2 Videos ****************/
        merge2Videos.setOnAction(e -> {
            mergeWithAnotherVideo();
        });

        /**************** Create Video from Frames ****************/
        createVideo.setOnAction(e -> {
            save7(SAVING_URL);
        });

    }


    public void setup(File file) {
        media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        // Get video name
        lblMediaTitle.setText("Video Title: " + file.getName());

        // Volume Slider
        // theVolumeSlider.setValue(mediaPlayer.getVolume() * 100);
        // theVolumeSlider.valueProperty().addListener(observable -> mediaPlayer.setVolume(theVolumeSlider.getValue() / 100));

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            progressSlider.setValue(newValue.toSeconds());
        });

        progressSlider.setOnMousePressed(event -> mediaPlayer.seek(Duration.seconds(progressSlider.getValue())));

        progressSlider.setOnMouseDragged(event -> mediaPlayer.seek(Duration.seconds(progressSlider.getValue())));

        mediaPlayer.setOnReady(() -> {
            Duration total = media.getDuration();
            // String minutes = new DecimalFormat("##").format((total.toSeconds() % 3600) / 60);
            // int seconds = (int) (total.toSeconds() % 60);
            totalTimeLabel.setText(getTime(total));
            progressSlider.setMax(total.toSeconds());
            mediaPlayer.play();
        });

        //------------------ George
        final int IV_SIZE = 25;
        Image imageVol = new Image(new File(ICONS_URL + "volume.png").toURI().toString());
        ivVolume = new ImageView(imageVol);
        ivVolume.setFitHeight(IV_SIZE);
        ivVolume.setFitWidth(IV_SIZE);

        Image imageMute = new Image(new File(ICONS_URL + "mute.png").toURI().toString());
        ivMute = new ImageView(imageMute);
        ivMute.setFitHeight(IV_SIZE);
        ivMute.setFitWidth(IV_SIZE);
        volumeLbl.setGraphic(ivMute);

        vBox.sceneProperty().addListener((observableValue, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                mediaView.fitHeightProperty().bind(
                        newScene.heightProperty().subtract(hBoxControls.heightProperty().add(240))
                );
            }
        });

        hBoxVolume.getChildren().remove(theVolumeSlider);

        // mediaPlayer.volumeProperty().bindBidirectional(theVolumeSlider.valueProperty());

        theVolumeSlider.valueProperty().addListener(observable -> {
            mediaPlayer.setVolume(theVolumeSlider.getValue());
            if (mediaPlayer.getVolume() != 0.0) {
                volumeLbl.setGraphic(ivVolume);
                isMuted = false;
            } else {
                volumeLbl.setGraphic(ivMute);
                isMuted = true;
            }
        });

        volumeLbl.setOnMouseClicked(mouseEvent -> {
            if (isMuted) {
                volumeLbl.setGraphic(ivVolume);
                theVolumeSlider.setValue(0.2);
                isMuted = false;
            } else {
                volumeLbl.setGraphic(ivMute);
                theVolumeSlider.setValue(0);
                isMuted = true;
            }
        });

        volumeLbl.setOnMouseEntered(mouseEvent -> {
            if (hBoxVolume.lookup("#theVolumeSlider") == null) {
                hBoxVolume.getChildren().add(theVolumeSlider);
                theVolumeSlider.setValue(mediaPlayer.getVolume());
            }
        });

        hBoxVolume.setOnMouseExited(mouseEvent -> hBoxVolume.getChildren().remove(theVolumeSlider));


        Image imageExit = new Image(new File(ICONS_URL + "exitscreen.png").toURI().toString());
        ivExit = new ImageView(imageExit);
        ivExit.setFitHeight(IV_SIZE);
        ivExit.setFitWidth(IV_SIZE);

        Image imageFull = new Image(new File(ICONS_URL + "fullscreen.png").toURI().toString());
        ivFullScreen = new ImageView(imageFull);
        ivFullScreen.setFitHeight(IV_SIZE);
        ivFullScreen.setFitWidth(IV_SIZE);

        fullscreenLabel.setGraphic(ivFullScreen);

        fullscreenLabel.setOnMouseClicked(mouseEvent -> {
            Label label = (Label) mouseEvent.getSource();
            Stage stage = (Stage) label.getScene().getWindow();
            if (stage.isFullScreen()) {
                stage.setFullScreen(false);
                fullscreenLabel.setGraphic(ivFullScreen);
            } else {
                stage.setFullScreen(true);
                fullscreenLabel.setGraphic(ivExit);
            }
            stage.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> fullscreenLabel.setGraphic(ivFullScreen));
        });

        progressSlider.valueProperty().addListener((observableValue, oldvalue, newvalue) -> {
            double currentTime = mediaPlayer.getCurrentTime().toSeconds();
            if (Math.abs(currentTime - newvalue.doubleValue()) > 0.5) {
                mediaPlayer.seek(Duration.seconds(newvalue.doubleValue()));
            }
//            labelMathEndVideo(currentTimeLabel.getText(), totalTimeLabel.getText());
        });

        mediaPlayer.totalDurationProperty().addListener((observableValue, oldDuration, newDuration) -> {
//           bindcurrenttimelable();
            progressSlider.setMax(newDuration.toSeconds());
            totalTimeLabel.setText(getTime(newDuration));
        });

        mediaPlayer.currentTimeProperty().addListener((observableValue, oldTime, newTime) -> {
            bindCurrentTimeLable();
            progressSlider.setValue(newTime.toSeconds());
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            atEndOfVideo = true;
            if (!currentTimeLabel.textProperty().equals(totalTimeLabel.textProperty())) {
                currentTimeLabel.textProperty().unbind();
                currentTimeLabel.setText(getTime(mediaPlayer.getTotalDuration()) + " / ");
            }
        });
        //------------------ George

        // mediaView.setPreserveRatio(true);

        // View video
        mediaView.setMediaPlayer(mediaPlayer);
    }

    public void openVideo() {

        deleteDirectoryFiles(new File(FRAMES_URL));
        deleteDirectoryFiles(new File(FRAMES_TEMP_URL));

        FileChooser fileChooser = new FileChooser();

        // Pausing current video
        if (mediaPlayer != null)
            mediaPlayer.pause();

        File file = fileChooser.showOpenDialog(primaryStage);
        fileChooser.setTitle("Select Media To Play");
        fileChooser.setInitialDirectory(new File("C:\\"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4")
        );

        // Choosing the file to play
        if (file != null) {
            MEDIA_URL = file.toString();
            lblMediaTitle.setText("Video Title: " + file.getName());

            setup(new File(MEDIA_URL));

            //------ Resize Video ------
            // DoubleProperty width = mediaView.fitWidthProperty();
            // DoubleProperty height = mediaView.fitHeightProperty();
            // width.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width"));
            // height.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height"));
            //--------------------------
        }

    }

    public void play() {
        mediaPlayer.play();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void stop() {
        mediaPlayer.stop();
    }

    public void restart() {
        mediaPlayer.seek(mediaPlayer.getStartTime());
        mediaPlayer.play();
    }

    public void skip5sec() {
        mediaPlayer.seek(mediaPlayer.getCurrentTime().add(javafx.util.Duration.seconds(5)));
    }

    public void back5sec() {
        mediaPlayer.seek(mediaPlayer.getCurrentTime().add(javafx.util.Duration.seconds(-5)));
    }

    public void speedUpVideo() {
        videoSpeed *= 2;
        mediaPlayer.setRate(videoSpeed);
    }

    public void slowDownVideo() {
        videoSpeed /= 2;
        mediaPlayer.setRate(videoSpeed);
    }

    public void snapshot() {
        int width = mediaPlayer.getMedia().getWidth();
        int height = mediaPlayer.getMedia().getHeight();
        WritableImage wimg = new WritableImage(width, height);
        // mediaView.setFitWidth(width);
        // mediaView.setFitHeight(height);
        mediaView.snapshot(null, wimg);

        try {
            String snapName = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()
            );
            ImageIO.write(SwingFXUtils.fromFXImage(wimg, null),
                    "png", new File(SNAPSHOTS_URL + snapName + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveVideo(ActionEvent actionEvent) {
        Media mediaToSave = mediaView.getMediaPlayer().getMedia();

        FileChooser imageSaver = new FileChooser();
        imageSaver.setTitle("Save Video File As");
        imageSaver.setInitialDirectory(new File("C:\\"));
        imageSaver.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File outputFile = imageSaver.showSaveDialog(null);

        if (outputFile != null) {
            String name = outputFile.getName();
            String extension = name.substring(1 + name.lastIndexOf(".")).toLowerCase();

            System.out.println("Saved Image: " + name);

            // mediaToSave.(bufferedImage, extension, outputFile);
        }
    }

    public void viewFramesInSlider(int numFrames) {

        framesHBox.getChildren().clear();
        Image image1 = null;

        try {

            copyDirectory(new File(FRAMES_URL), new File(FRAMES_TEMP_URL));

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }


        try {
            image1 = new Image(new FileInputStream(FRAMES_TEMP_URL + "0.jpg"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        frameView.setFitHeight(450);
        frameView.setFitWidth(470);
        frameView.setImage(image1);


        for (int i = 0; i < numFrames; i++) {

            Image image = null;

            try {
                File filetemp = new File(FRAMES_TEMP_URL + i + ".jpg");
                File file = new File(FRAMES_URL + i + ".jpg");


                if (!file.exists())
                    continue;


                selectedImage = String.valueOf(i);
                image = new Image(new FileInputStream(FRAMES_TEMP_URL + i + ".jpg"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            ImageView im = new ImageView();
            im.setImage(image);
            im.setFitHeight(100);
            im.setFitWidth(150);


            Button button = new Button();
            button.setId(String.valueOf(i));
            button.setPrefSize(100, 150);
            button.setGraphic(im);
            framesHBox.getChildren().add(button);
            final Delta dragDelta = new Delta();

            button.setOnMousePressed(e -> {

                Image image2 = null;

                try {

                    selectedImage = String.valueOf(framesHBox.getChildren().indexOf(button));
                    selectedImageID = button.getId();
                    image2 = new Image(new FileInputStream(FRAMES_TEMP_URL + button.getId() + ".jpg"));
                } catch (FileNotFoundException er) {
                    er.printStackTrace();
                }

                frameView.setFitHeight(450);
                frameView.setFitWidth(470);
                frameView.setImage(image2);

                // record a delta distance for the drag and drop operation.
                dragDelta.x = button.getLayoutX() - e.getSceneX();
                dragDelta.y = button.getLayoutY() - e.getSceneY();

                button.setCursor(Cursor.MOVE);
            });

            button.setOnMouseReleased(e -> {

                button.setCursor(Cursor.HAND);

            });

            button.setOnMouseDragged(e -> {

                ObservableList<Node> workingCollection = FXCollections.observableArrayList(framesHBox.getChildren());


                String ii = button.getId();
                int iii = framesHBox.getChildren().indexOf(button);
                int jj = iii + 1;

                Collections.swap(workingCollection, iii, jj);


                try {
                    Thread.sleep(500);

                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                framesHBox.getChildren().setAll(workingCollection);


                button.layoutXProperty().set(button.getLayoutX() + e.getX() - dragDelta.x);
                // button.setLayoutX(e.getSceneX() + dragDelta.x);
                button.setLayoutY(e.getSceneY() + dragDelta.y);

                System.out.println(button.getLayoutX() + e.getX() - dragDelta.x);

            });

            button.setOnMouseEntered(e -> {
                button.setCursor(Cursor.HAND);

            });

        }


    }

    public void removeFrame() {
        framesHBox.getChildren().remove(Integer.parseInt(selectedImage));
        int selectedIdx = Integer.parseInt(selectedImageID) + 1;

        File file = new File(FRAMES_URL + selectedIdx + ".jpg");

        if (!file.exists())
            for (int i = 1; i < 20; i++) {

                selectedIdx = selectedIdx + i;
                file = new File(FRAMES_URL + selectedIdx + ".jpg");
                if (!file.exists())
                    continue;

                else break;
            }

        try {
            frameView.setImage(new Image(new FileInputStream(FRAMES_URL + selectedIdx + ".jpg")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Path imagesPath = Paths.get(FRAMES_URL + selectedImageID + ".jpg");

//        File index = new File("D:\\video_editor_v4\\src\\sample\\output\\" + selectedImageTD + ".jpg");

        try {
            System.out.println(selectedImage);

            Files.delete(imagesPath);

            System.out.println("File "
                    + imagesPath.toAbsolutePath().toString()
                    + " successfully removed");


        } catch (IOException e) {
            System.err.println("Unable to delete "
                    + imagesPath.toAbsolutePath().toString()
                    + " due to...");
            e.printStackTrace();
        }
    }

    public void setTextWaterMark(File fram, String text, int opacityVal, String videPath, String outputPath) {
        String snapName = new SimpleDateFormat(
                "yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()
        );

        outputPath += ".jpg";

        //Reading the contents of an image

        File file = fram;
        BufferedImage img = null;
        int height = 0;
        int width = 0;
        try {
            img = ImageIO.read(file);
            height = img.getHeight();
            width = img.getWidth();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Creating an empty image for output
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        //Creating a graphics object
        Graphics graphics = res.getGraphics();
        graphics.drawImage(img, 0, 0, null);
        //Creating font for water mark
        Font font = new Font("Arial", Font.PLAIN, 45);
        graphics.setFont(font);
        graphics.setColor(new Color(248, 248, 248, opacityVal));
        FontMetrics fontMetrics = graphics.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, graphics);

        //Drawing the water mark string on the image
        // calculates the coordinate where the String is painted
        int centerX = (width - (int) rect.getWidth()) / 2;
        int centerY = height / 2;
        graphics.drawString(text, centerX, centerY);

        //Disposing the string
        graphics.dispose();
        //Writing the result image.
        file = new File(outputPath);
        try {
            ImageIO.write(res, "jpg", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImageWaterMark(String inputImage, String watermarkPath, String outputPath) throws IOException {
        String snapName = new SimpleDateFormat(
                "yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()
        );

        outputPath += ".jpg";
        BufferedImage waterMark = ImageIO.read(new File(watermarkPath));
        waterMark = Thumbnails.of(waterMark).size(100, 100).asBufferedImage();
        BufferedImage image = ImageIO.read(new File(inputImage));
        Thumbnails.of(image)
                .scale(1)
                .watermark(Positions.BOTTOM_RIGHT, waterMark, 0.25f)
                .toFile(new File(outputPath));
    }

    /**
     * New Function
     **/
    public void setEmojis(String inputImage, String outputPath) {
        // Choose an emoji
        String emoji = openImageGetPath();

        // Choose frame to add on [Click / TextField]

        String chosenFrame = "0.jpg"; // get it by click or by time

        try {
            setImageWaterMark(FRAMES_URL + chosenFrame,
                    emoji,
                    "watermarks/emojiWM");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    public void getVideoFrames(String filePath, String output) {

        if (!Paths.get(filePath).toFile().exists()) {
            System.out.println("File " + filePath + " does not exist!");
            return;
        }

        VideoCapture video = new VideoCapture(filePath);
        Mat frame = new Mat();

        System.out.println(video.read(frame));
        video.open(filePath);
        System.out.println(video.grab());

        if (video.isOpened()) {
            System.out.println("Test");

            int video_length = (int) video.get(Videoio.CAP_PROP_FRAME_COUNT);
            int frames_per_second = (int) video.get(Videoio.CAP_PROP_FPS);
            int frame_number = 0;

            System.out.println(video_length);
            System.out.println(frames_per_second);

            if (video.read(frame)) {
                for (int i = 0; i < video_length; i++) {
                    System.out.println("Frame Obtained");
                    System.out.println("Captured Frame Width " + frame.width() + " Height " + frame.height());

//                    BufferedImage bimg = matToBufferedImage(frame);
//                    String imgType = "jpg",
//                            path = output + "/" + frame_number + ".jpg", frame;
//                    try {
//                        ImageIO.write(bimg, imgType, new File(path));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    // Or Write image by OpenCV
                    Imgcodecs.imwrite(output + "/" + frame_number + ".jpg", frame);

                    System.out.println("OK");
                    frame_number++;
                }
            }

            video.release();
        }
    }

    public void getVideoFrames2(String input, String output) {
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        VideoCapture cap = new VideoCapture();

        cap.open(input);

        int video_length = (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) cap.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) cap.get(Videoio.CAP_PROP_POS_FRAMES);

        Mat frame = new Mat();

        if (cap.isOpened()) {
            System.out.println("Video is opened");
            System.out.println("Number of Frames: " + video_length);
            System.out.println(frames_per_second + " Frames per Second");
            System.out.println("Converting Video...");
//            cap.read(frame);
            while (frame_number <= video_length) {
//                System.out.println("Converting Video...");
                if (cap.read(frame))
                    Imgcodecs.imwrite(output + "/" + frame_number + ".jpg", frame);
                frame_number++;

            }
            cap.release();
            frame_number_all = frame_number - 2;
            System.out.println(video_length + " Frames extracted");
//            viewFramesInSlider(numFream);
        } else {
            System.out.println("Fail");
        }
    }

    public void getVideoFrames3(String input, String output) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture cap = new VideoCapture();

        cap.open(input);

        int video_length = (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) cap.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) cap.get(Videoio.CAP_PROP_POS_FRAMES);
        // frame_number_all=frame_number;
        Mat frame = new Mat();

        if (cap.isOpened()) {
            System.out.println("Video is opened");
            System.out.println("Number of Frames: " + video_length);
            System.out.println(frames_per_second + " Fra-mes per Second");
            System.out.println("Converting Video...");
//            cap.read(frame);
            while (frame_number <= video_length) {
                if (cap.read(frame))
                    Imgcodecs.imwrite(output + "/" + frame_number_all + ".jpg", frame);
                frame_number_all++;
                frame_number++;
            }
            cap.release();

            System.out.println(video_length + " Frames extracted");

        } else {
            System.out.println("Fail");
        }
    }

    public void save6(String outFilepath) {
        Mat frame = new Mat();

        String inputVid = MEDIA_URL;

        VideoCapture videoCapture = new VideoCapture(inputVid);
        videoCapture.open(inputVid);
        int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) videoCapture.get(Videoio.CAP_PROP_POS_FRAMES);
        Size frameSize = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));


        VideoWriter videoWriter = new VideoWriter(outFilepath + "mm.mp4", //3rd avi
//                VideoWriter.fourcc('M', 'P', 'E', 'G'),
//                VideoWriter.fourcc('M','J','P','G'),
//                VideoWriter.fourcc('x', '2','6','4'),
//                VideoWriter.fourcc('D', 'I', 'V', 'X'),
                //  VideoWriter.fourcc('a', 'v', 'c', '1'),
                VideoWriter.fourcc('X', 'V', 'I', 'D'),
                60,
                frameSize,
                true
        );

        File path = new File(SAVING_URL1);

        File[] files = path.listFiles();
        Imgcodecs imageCodecs = new Imgcodecs();

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                frame = Imgcodecs.imread(SAVING_URL1 + i + ".jpg");
                videoWriter.write(frame);
            }
        }
        videoWriter.release();

    }

    public void save7(String outFilepath) {
        Mat frame = new Mat();

        String inputVid = MEDIA_URL;

        VideoCapture videoCapture = new VideoCapture(inputVid);
        videoCapture.open(inputVid);
        int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) videoCapture.get(Videoio.CAP_PROP_POS_FRAMES);
        Size frameSize = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));


        VideoWriter videoWriter = new VideoWriter(outFilepath + "gkgk.mp4",
                VideoWriter.fourcc('X', 'V', 'I', 'D'),
                60,
                frameSize,
                true
        );

        File path = new File(SAVING_URL1);

        File[] files = path.listFiles();
        Imgcodecs imageCodecs = new Imgcodecs();

        DirectoryChooser imgOpener = new DirectoryChooser();
        imgOpener.setTitle("Choose Image File");
        imgOpener.setInitialDirectory(new File("./"));
        File dirPath = imgOpener.showDialog(null);

        String srcPath = dirPath.getAbsolutePath();

        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    frame = Imgcodecs.imread(srcPath + "\\" + file.getName());
                    videoWriter.write(frame);
                }
            }
        }
        videoWriter.release();

    }

    public void bindCurrentTimeLable() {
        currentTimeLabel.textProperty().bind(Bindings.createStringBinding(() -> getTime(mediaPlayer.getCurrentTime()) + " / ", mediaPlayer.currentCountProperty()));
    }

    public void save2(String outFilepath, int newFps, int width, int height) {
        Mat frame = new Mat();

        String inputVid = MEDIA_URL;

        VideoCapture videoCapture = new VideoCapture(inputVid);
        videoCapture.open(inputVid);
        int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) videoCapture.get(Videoio.CAP_PROP_POS_FRAMES);
        Size frameSize = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));

        Size newFrameSize = new Size(width, height);

        VideoWriter videoWriter = new VideoWriter(outFilepath + "newvid.mp4", //3rd avi
//                VideoWriter.fourcc('M', 'P', 'E', 'G'),
//                VideoWriter.fourcc('M','J','P','G'),
//                VideoWriter.fourcc('x', '2','6','4'),
//                VideoWriter.fourcc('D', 'I', 'V', 'X'),
                VideoWriter.fourcc('a', 'v', 'c', '1'),
                newFps,
                frameSize,
                true
        );

        File path = new File(FRAMES_URL);

        File[] files = path.listFiles();
        Imgcodecs imageCodecs = new Imgcodecs();

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                frame = Imgcodecs.imread(FRAMES_URL + i + ".jpg");
                videoWriter.write(frame);
            }
        }
        videoWriter.release();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        VideoShow();
    }

    public void save3(String inputVid) {
        VideoWriter videoWriter = null;

        VideoCapture videoCapture = new VideoCapture(inputVid);
        videoCapture.open(inputVid);

        if (videoCapture.isOpened()) {
            Mat m = new Mat();
            videoCapture.read(m);

            int fourcc = VideoWriter.fourcc('a', 'v', 'c', '1');

            double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
            Size s = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
            videoWriter = new VideoWriter("src/sample/test.mp4", fourcc, fps, s, true);

            while (videoCapture.read(m)) {
                videoWriter.write(m);

                // Have tried, did not work.
                // int i = 0;
                // Mat clonedMatrix = m.clone();
                // Imgproc.putText(clonedMatrix, ("frame" + i), new Point(100,100), 1, 2, new Scalar(200,0,0), 3);
                // videoWriter.write(clonedMatrix);
                // i++;
            }
        }

        videoCapture.release();
        if (videoWriter != null) {
            videoWriter.release();
        }
    }

    public void save4(String filepath) {
        Mat frame = new Mat();

        String inputVid = MEDIA_URL;

        VideoCapture videoCapture = new VideoCapture(inputVid);
        videoCapture.open(inputVid);
        int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) videoCapture.get(Videoio.CAP_PROP_POS_FRAMES);
        Size frameSize = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));


        VideoWriter videoWriter = null;

        File path = new File(FRAMES_URL);

        File[] files = path.listFiles();
        Imgcodecs imageCodecs = new Imgcodecs();

        int numOfSubVideos = 5;
//        int framesNum = video_length / numOfSubVideos;
        if (files != null) {
            int framesNum = files.length / numOfSubVideos;
            //System.out.println(files.length + " - aaaaa - " + frame_number);
            int count = 0;
            for (int i = 0; i < files.length; i++) {
                File f = new File(FRAMES_URL + i + ".jpg");
                if (f.exists() && !f.isDirectory()) {
                    if (count == frames_per_second + 5) {
                        videoWriter = new VideoWriter(filepath + "subVid_" + i + ".mp4", //3rd avi
                                VideoWriter.fourcc('a', 'v', 'c', '1'),
                                frames_per_second,
                                frameSize,
                                true
                        );
                        count = 0;
                    }
                    frame = Imgcodecs.imread(FRAMES_URL + i + ".jpg");
                    if (videoWriter != null)
                        videoWriter.write(frame);
                    count++;
                }
            }
        }
        if (videoWriter != null)
            videoWriter.release();
    }

    public void chooseWaterMark() {
        WATERMARK_URL = openImageGetPath();
    }

    public void addWaterMarks() {

        File path = new File(FRAMES_URL);

        File[] files = path.listFiles();
        textWatermark.setOnAction(e -> {
                    TextWatermark twm = getInputDialogText();
                    if (twm.inputVal != null) {
                        for (int i = 0; i < files.length; i++) {
                            setTextWaterMark(new File(FRAMES_URL + i + ".jpg"), twm.inputVal,
                                    twm.opacityVal,
                                    MEDIA_URL,
                                    FRAMES_URL + i
                            );
                        }
                    }


                }
        );

        imageWatermark.setOnAction(e -> {
                    chooseWaterMark();
                    if (WATERMARK_URL != null) {
                        try {

                            for (int i = 0; i < files.length; i++) {
                                setImageWaterMark(FRAMES_URL + i + ".jpg",
                                        WATERMARK_URL,
                                        FRAMES_URL + i);
                            }
                            WATERMARK_URL = null;
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
        );

        videoWatermark.setOnAction(e -> {
                    chooseWaterMark();
                    //if (WATERMARK_URL != null)
                    // setVideoWaterMark();
                }
        );
    }

    /**
     * New Functions
     **/
    public void cutVideo() {

        VideoCapture videoCapture = new VideoCapture(MEDIA_URL);

        int cutFromSec = convertToSeconds(cutFrom);

        System.out.print("[Cut Video] cutFrom:" + cutFromSec);

        int cutToSec = convertToSeconds(cutTo);
        System.out.println(" - cutTo:" + cutToSec);

        int fps = (int) videoCapture.get(Videoio.CAP_PROP_FPS);

        int startFrame = cutFromSec * fps;

        int endFrame = cutToSec * fps;

        saveFramesAsVideoFromTo(SAVING_URL, startFrame, endFrame, true);

    }

    public void saveFramesAsVideoFromTo(String outFilepath, int from, int to, boolean isCut) {
        Mat frame = new Mat();

        String inputVid = MEDIA_URL;

        VideoCapture videoCapture = new VideoCapture(inputVid);
        videoCapture.open(inputVid);
        int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) videoCapture.get(Videoio.CAP_PROP_POS_FRAMES);
        Size frameSize = new Size((int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH), (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));

        VideoWriter videoWriter = new VideoWriter(outFilepath + "newvid.mp4",
                VideoWriter.fourcc('a', 'v', 'c', '1'),
                frames_per_second,
                frameSize,
                true
        );

        File path = new File(FRAMES_URL);

        File[] files = path.listFiles();
        Imgcodecs imageCodecs = new Imgcodecs();

        if (files != null && to < files.length) {
            if (isCut)
                for (int i = from; i < to; i++) {
                    frame = Imgcodecs.imread(FRAMES_URL + i + ".jpg");
                    videoWriter.write(frame);
                }
            else
                for (int i = 0; i < files.length; i++) {
                    if (i >= from && i <= to)
                        continue;
                    frame = Imgcodecs.imread(FRAMES_URL + i + ".jpg");
                    videoWriter.write(frame);
                }
        }
        videoWriter.release();
    }

    public void cutOutOfVideo() {
        VideoCapture videoCapture = new VideoCapture(MEDIA_URL);
        int cutFromSec = convertToSeconds(cutFrom);
        System.out.print("[Cut out of Video] From:" + cutFromSec);

        int cutToSec = convertToSeconds(cutTo);
        System.out.println(" - cutTo:" + cutToSec);

        int fps = (int) videoCapture.get(Videoio.CAP_PROP_FPS);

//        System.out.println("total frames: "+videoCapture.totalFrames());

        System.out.println("fps: " + fps);

        int startFrame = cutFromSec * fps;

        int endFrame = cutToSec * fps;

        saveFramesAsVideoFromTo(SAVING_URL, startFrame, endFrame, false);
    }

    public void cutMoveVideo() {
        VideoCapture videoCapture = new VideoCapture(MEDIA_URL);

        int cutFromSec = convertToSeconds("0");
        System.out.println("cutFrom:" + cutFromSec);

        int cutToSec = convertToSeconds("1");
        System.out.println("cutTo:" + cutToSec);

        int moveToSec = convertToSeconds("3");
        System.out.println("moveTo:" + moveToSec);

        int fps = (int) videoCapture.get(Videoio.CAP_PROP_FPS);

//        System.out.println("total frames: "+videoCapture.totalFrames());

        System.out.println("fps: " + fps);

        int startFrame = cutFromSec * fps;

        int endFrame = cutToSec * fps;

        int moveToFrame = moveToSec * fps;
        Mat frame = new Mat();
        int fourcc = VideoWriter.fourcc('a', 'v', 'c', '1');


        videoCapture.read(frame);
        VideoWriter cutWriter = new VideoWriter(SAVING_URL + "cut_temp.mp4", fourcc, fps, frame.size(), true);

        videoCapture.set(1, startFrame);

        while (videoCapture.get(1) < endFrame) {
            videoCapture.read(frame);
            cutWriter.write(frame);
        }
        cutWriter.release();

        VideoWriter cutAndMoveWriter = new VideoWriter(SAVING_URL + "cut_and_move.mp4", fourcc, fps, frame.size(), true);

        videoCapture = new VideoCapture(MEDIA_URL);
        videoCapture.set(1, 0);

        VideoCapture cutVideoCapture = new VideoCapture("video/cut_temp.mp4");

        while (true) {
            if (videoCapture.read(frame)) {
                if (videoCapture.get(1) == moveToFrame) {
                    Mat tempFrame = new Mat();
                    while (cutVideoCapture.read(tempFrame)) {
                        cutAndMoveWriter.write(tempFrame);
                    }
                }
                if (videoCapture.get(1) >= startFrame && videoCapture.get(1) <= endFrame) {
                    continue;
                }
            } else break;

            cutAndMoveWriter.write(frame);

        }

        cutAndMoveWriter.release();
        cutVideoCapture.release();
    }

    public void mergeWithAnotherVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("mp4", "*.mp4")
        );
        File video = fileChooser.showOpenDialog(null);

        if (video != null) {
            String secondPath = video.toURI().toString();
            VideoCapture videoCapture1 = new VideoCapture(MEDIA_URL);
            VideoCapture videoCapture2 = new VideoCapture(secondPath);

            Mat frame1 = new Mat();
            Mat frame2 = new Mat();

            getVideoFrames2(MEDIA_URL, SAVING_URL1);
            System.out.println(frame_number_all);
            getVideoFrames3(secondPath, SAVING_URL1);
            save6(SAVING_URL);

        } else {
            System.out.println("no files has been selected");
        }

    }

    public void addVideoWaterMark(int opacity) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("mp4", "*.mp4")
        );
        File video = fileChooser.showOpenDialog(null);
        String newVideoPath;
        if (video != null) {
            newVideoPath = video.toURI().toString();
        } else {
            System.out.println("no files has been selected");
            return;
        }
        VideoCapture videoCapture = new VideoCapture(MEDIA_URL);

        Mat frame = new Mat();
        int fourcc = VideoWriter.fourcc('a', 'v', 'c', '1');
        int fps = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
        String date = new SimpleDateFormat(
                "yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()
        );

        videoCapture.read(frame);
        VideoWriter writer = new VideoWriter(
                SAVING_URL + "water_mark_video" + date + ".mp4",
                fourcc,
                fps,
                frame.size(),
                true);
        VideoCapture waterMarkVideoCapture = new VideoCapture(newVideoPath);
        Mat waterMarkFrame = new Mat();

        while (true) {
            if (videoCapture.read(frame)) {
                if (waterMarkVideoCapture.read(waterMarkFrame)) {
                    Mat wmFrame = addVideoFrameWatermark(waterMarkFrame, frame, opacity);
                    writer.write(wmFrame);
                } else writer.write(frame);
            } else break;

        }
        writer.release();
    }

    public Mat addVideoFrameWatermark(Mat watermarkImageMat, Mat sourceImageMat, float opacity) {

        BufferedImage sourceImage = Mat2BufferedImage(sourceImageMat);
        BufferedImage watermarkImage = Mat2BufferedImage(watermarkImageMat);
        // initializes necessary graphic properties
        Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
        g2d.setComposite(alphaChannel);

        // calculates the coordinate where the image is painted
        int topLeftX = (sourceImage.getWidth() - watermarkImage.getWidth()) / 2;
        int topLeftY = (sourceImage.getHeight() - watermarkImage.getHeight()) / 2;

        // paints the image watermark
        g2d.drawImage(watermarkImage, topLeftX, topLeftY, null);

        g2d.dispose();
        try {
            return BufferedImage2Mat(sourceImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sourceImageMat;

    }


    /****************************** Utils ******************************/
    static class Delta {
        double x, y;
    }

    public void keyPressed() {
        vBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (isPlaying)
                    pause();
                else
                    play();
                isPlaying = !isPlaying;
            } else if (event.getCode() == KeyCode.ESCAPE) {
                exitWindow();
            }
        });
    }

    public int getOpacityDialogValue() {
        var ov = new Object() {
            int opacityVal;
        };
        TextInputDialog tid = new TextInputDialog("Video Editor");
        tid.setTitle("Input Dialog");
        tid.setHeaderText("Text Input");
        tid.setContentText("Enter a text: ");

        tid.setGraphic(new ImageView(getClass().getResource("icons/edit.png").toString()));

        // Set the button types.
//        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
//        tid.getDialogPane().getButtonTypes().setAll(okButtonType, ButtonType.CANCEL);

        Optional<String> result = tid.showAndWait();

        result.ifPresent(o -> ov.opacityVal = Integer.parseInt(o));
        return ov.opacityVal;
    }

    static class TextWatermark {
        String inputVal;
        int opacityVal;
    }

    public TextWatermark getInputDialogText() {
        TextWatermark twm = new TextWatermark();

        // Create the custom dialog.
        Dialog<Pair<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Text Watermark Dialog");
        dialog.setHeaderText(null);

        // Set the icon (must be included in the project).
        dialog.setGraphic(new ImageView(this.getClass().getResource("icons/edit.png").toString()));

        // Set the button types.
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField text = new TextField();
        text.setPromptText("Video Editor");
        Slider opacity = new Slider(0, 255, 255);
        // opacity.setMajorTickUnit(0.1);
        /// opacity.setMinorTickCount(1);
        // opacity.setBlockIncrement(1);
        opacity.setShowTickLabels(true);

        grid.add(new Label("Watermark Text"), 0, 0);
        grid.add(text, 1, 0);
        grid.add(new Label("Opacity:"), 0, 1);
        grid.add(opacity, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        text.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(text::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new Pair<>(text.getText(), (int) opacity.getValue());
            }
            return null;
        });

        Optional<Pair<String, Integer>> result = dialog.showAndWait();

        result.ifPresent(in -> {
            System.out.println("Entered Text=" + in.getKey() + ", Opacity=" + in.getValue());
            twm.inputVal = in.getKey();
            twm.opacityVal = in.getValue();
        });

        return twm;
    }

    public void openFrameInputDialog() {
        // Create the custom dialog.
        Dialog<Pair<String, Pair<String, String>>> dialog = new Dialog<>();
        dialog.setTitle("Save Video");
        dialog.setHeaderText(null);

        // Set the icon (must be included in the project).
        dialog.setGraphic(new ImageView(getClass().getResource("icons/fps.png").toString()));

        // Set the button types.
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField fps = new TextField();
        fps.setPromptText("Frame Per Second");
        TextField width = new TextField();
        width.setPromptText("Width");
        TextField height = new TextField();
        height.setPromptText("Height");

        grid.add(new Label("Frame Per Second:"), 0, 0);
        grid.add(fps, 1, 0);
        grid.add(new Label("Width:"), 0, 1);
        grid.add(width, 1, 1);
        grid.add(new Label("Height:"), 0, 2);
        grid.add(height, 1, 2);

        // Enable/Disable login button depending on whether a username was entered.
        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        fps.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(fps::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new Pair<>(fps.getText(), new Pair<>(width.getText(), height.getText()));
            }
            return null;
        });

        Optional<Pair<String, Pair<String, String>>> result = dialog.showAndWait();

        result.ifPresent(res -> {
            //System.out.println("FPS=" + res.getKey() +
            //        ", Width=" + res.getValue().getKey() +
            //        ", Height=" + res.getValue().getValue()
            //);

            // create video from origin Video
            // save3(MEDIA_URL);

            // create video from saved frames
            save2(SAVING_URL,
                    Integer.parseInt(res.getKey()),
                    Integer.parseInt(res.getValue().getKey()),
                    Integer.parseInt(res.getValue().getValue()));

        });
    }

    public String openImageGetPath() {
        FileChooser imgOpener = new FileChooser();
        imgOpener.setTitle("Choose Image File");
        imgOpener.setInitialDirectory(new File("./"));
        File openedImg = imgOpener.showOpenDialog(null);

        if (openedImg != null)
            return openedImg.getAbsolutePath();
        else
            return null;
    }

    public void exitWindow() {
        primaryStage.close();
    }

    public BufferedImage matToBufferedImage(Mat frame) {
        /*
        Java2DFrameConverter converter = new Java2DFrameConverter();
        BufferedImage bi = converter.convert(frame);
        */

        // Read image to Mat as before
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2GRAY, 0);

        // Create an empty image in matching format
        BufferedImage gray = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_BYTE_GRAY);

        // Get the BufferedImage's backing array and copy the pixels directly into it
        byte[] data = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();
        frame.get(0, 0, data);
        return gray;
    }

    public Mat BufferedImage2Mat(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);
    }

    public BufferedImage Mat2BufferedImage(Mat m) {
        //Method converts a Mat to a Buffered Image
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public boolean isDirContainFiles(String dirPath) {
        boolean checkResult = false;
        try {
            checkResult = Files.list(Paths.get(FRAMES_URL)).findAny().isPresent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return checkResult;
    }

    public String getTime(Duration time) {
        int hours = (int) time.toHours();
        int minutes = (int) time.toMinutes();
        int seconds = (int) time.toSeconds();
        if (seconds > 59) seconds = seconds % 60;
        if (minutes > 59) minutes = minutes % 60;
        if (hours > 59) hours = hours % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        else return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    public static int convertToSeconds(String time) {
        String[] times = time.split(":");
        System.out.println(times.toString());
        if (times.length < 2)
            return Integer.parseInt(time);

        int minutes = Integer.parseInt(times[0]);
        int seconds = Integer.parseInt(times[1]);

        seconds = seconds + (minutes * 60);
        return seconds;
    }

    public void deleteDirectoryFiles(File dir) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory())
                deleteDirectoryFiles(file);
            file.delete();
        }
    }

    public void deleteFile(String filePath) {
        new File(filePath).delete();
    }


    public void playFrams() {
        VideoCapture videoCapture = new VideoCapture(MEDIA_URL);


        int fps = (int) videoCapture.get(Videoio.CAP_PROP_FPS);

        frameView.setFitHeight(450);
        frameView.setFitWidth(470);

        File path = new File(FRAMES_URL);

        File[] files = path.listFiles();

        for (int i = 0; i < files.length; i++) {


            Image image = null;
            try {
                selectedImage = String.valueOf(i);
                image = new Image(new FileInputStream(FRAMES_URL + i + ".jpg"));
                frameView.setImage(image);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
    }

    public void setEmoji(String inputImage, String watermarkPath, String outputPath, Point p) throws IOException {
        String snapName = new SimpleDateFormat(
                "yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()
        );

        outputPath += ".jpg";
        BufferedImage waterMark = ImageIO.read(new File(watermarkPath));
        waterMark = Thumbnails.of(waterMark).size(100, 100).asBufferedImage();
        BufferedImage image = ImageIO.read(new File(inputImage));
        Thumbnails.of(image)
                .scale(1)
                .watermark((Position) p, waterMark, 0.25f)
                .toFile(new File(outputPath));
    }


    public void VideoShow() {

        // Pausing current video
        mediaPlayer.pause();

        File file = new File(SAVING_URL + "newvid.mp4");


        // Choosing the file to play
        if (file != null) {

            MEDIA_URL = file.toString();
            lblMediaTitle.setText("Video Title: " + file.getName());

            setup(new File(MEDIA_URL));

            viewFramesInSlider(numFrames);

        }

    }

    public void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }


    public String[] getInputDialogCut() {
        final String[] input = new String[3];

        // Create the custom dialog.
        Dialog<Pair<String, Pair<String, String>>> dialog = new Dialog<>();
        dialog.setTitle("Cut/Remove Dialog");
        dialog.setHeaderText(null);

        // Set the icon (must be included in the project).
        dialog.setGraphic(null);

        // Set the button types.
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField fromInputField = new TextField();
        fromInputField.setPromptText("Video Editor");

        TextField toInputField = new TextField();
        toInputField.setPromptText("Video Editor");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().add("cut");
        comboBox.getItems().add("remove");

        grid.add(new Label("From:"), 0, 0);
        grid.add(fromInputField, 1, 0);
        grid.add(new Label("To:"), 0, 1);
        grid.add(toInputField, 1, 1);
        grid.add(new Label("Operation:"), 0, 2);
        grid.add(comboBox, 1, 2);

        // Enable/Disable login button depending on whether a username was entered.
        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        fromInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(fromInputField::requestFocus);

        String selectedItem = comboBox.getSelectionModel().getSelectedItem();

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {

                cutFrom = fromInputField.getText();
                cutTo = toInputField.getText();
                return new Pair<>(fromInputField.getText(), new Pair<>(toInputField.getText(), comboBox.getValue()));
            }
            return null;
        });

        Optional<Pair<String, Pair<String, String>>> result = dialog.showAndWait();

        result.ifPresent(in -> {
            //System.out.println("from=" + in.getKey() + "to=" +  in.getValue().getKey() + ", Operation=" + in.getValue());
            input[0] = in.getKey();
            input[1] = in.getValue().getKey();
            input[2] = in.getValue().getValue();
        });

        return input;
    }


}