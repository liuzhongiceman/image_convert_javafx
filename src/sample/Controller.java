package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Controller {
    @FXML
    private BorderPane borderPane;
    @FXML
    private ChoiceBox choiceBox;
    @FXML
    private Label chooseSize;
    @FXML
    private TextField textWidth;
    @FXML
    private TextField textHeight;
    @FXML
    private Label errDisplay;
    @FXML
    private GridPane gridpane;
    @FXML
    private CheckBox checkBoxblur;
    @FXML
    private CheckBox checkBoxedge;
    @FXML
    private CheckBox checkBoxflip;
    @FXML
    private Label content_header;
    @FXML
    private Label notChoose;
    @FXML
    private Button convertBtn;
    @FXML
    private Button uploadBtn;

    private Image image;
    private List<File> files;
    private boolean didUpload;
    private boolean uploadCheck;

    // this function handles when user click the upload button
    @FXML
    public void uploadHandle() {
        String text;
        FileInputStream input;
        ImageView imageView;
        String sourcePath;
        String imageName;
//        int total = 0;
        gridpane.getChildren().clear(); // each time user click the upload button we clear its previous content
        content_header.setVisible(true); // display content-header in case we hide it previously
//        errDisplay.setVisible(false);
        errDisplay.setText("");
        int count = 0; // we want each row has at most 5 images, so we need a count variable
        FileChooser chooser = new FileChooser();   // initial filechooser
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.gif"
                        , "*.psd", "*.jpeg")
        );
        files = chooser.showOpenMultipleDialog(borderPane.getScene().getWindow());

        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                try {
                    Label textArea = new Label();
                    File file = files.get(i);
                    int indexX = i % 5;
                    int indexY = i / 5; // we want each row has at most 5 images
                    count++; // update count variable

                    sourcePath = files.get(i).getPath(); // get the image path
                    javaxt.io.Image imageInfo = new javaxt.io.Image(sourcePath); // initial imageInfo with the javaxt tool
                    java.util.HashMap<Integer, Object> exif = imageInfo.getExifTags();
                    double[] coord = imageInfo.getGPSCoordinate(); // get the gps info of the image

                    imageName = file.getName();
                    image = new Image(file.toURI().toString());

                    // display images
                    input = new FileInputStream(file);
                    image = new Image(input);
                    System.out.println("width1: "+image.getWidth());
                    System.out.println("height1: "+image.getHeight());

                    imageView = new ImageView(image);

                    // display images with different size based on how many images we have to display
                    if (files.size() == 1) {
                        imageView.setFitHeight(200);
                        imageView.setFitWidth(200);
                    } else if (files.size() > 1 && files.size() < 5) {
                        imageView.setFitHeight(130);
                        imageView.setFitWidth(130);
                    } else if (files.size() >= 5) {
                        imageView.setFitHeight(100);
                        imageView.setFitWidth(100);
                    }

                    // if the image has gps info, we display the gps info, otherwise, we do not display
                    if (coord != null) {
                        text = "Name: " + imageName + "\n" + "Height: " + image.getHeight() + "\n" + "Width: " + image.getWidth() + "\n" +
                                "Date: " + (exif.get(0x0132).toString().substring(0, 10)) + "\n" + "Camera: " + exif.get(0x0110) + "\n" +
                                "Latitude: " + coord[0] + "\n" + "Longitude: " + coord[1] + "\n" +
                                "Manufacturer: " + exif.get(0x010F);
                    } else {
                        text = "name: " + imageName + "\n" + "height: " + image.getHeight() + "\n" +
                                "width: " + image.getWidth();
                    }
                    textArea.setText(text);
                    if (count > 5) {
                        indexY += 4; // when the current row displays 5 images, we display the following images in the next row
                    }
                    if (count > 10) {
                        break;
                    }
                    gridpane.add(imageView, indexX, indexY);
                    gridpane.add(textArea, indexX, indexY + 1);
                    uploadCheck = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Chooser was cancelled");
          }
    }

    // this function handles when convert button clicked
    @FXML
    public void handleConvert() throws IOException {
        if(!checkNumValid()){
            notChoose.setText("Check input number");
            notChoose.setVisible(true);
        } else if(checkNumValid() && uploadCheck) {
            int width = 0;
            int height = 0;
            int convertWidth;
            int convertHeight;
            FileInputStream input;

            errDisplay.setVisible(false); // we clear the errDisplay info in case user had a error before this use
            String filePath;
            String sourcePath;
            FileChooser chooser = new FileChooser();
            File file = chooser.showSaveDialog(borderPane.getScene().getWindow()); // save file dialog
            for (int i = 0; i < files.size(); i++) {
                chooser.setInitialFileName(file.getName());
                chooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Image Files", "*." + choiceBox.getValue())
                );
                String destinationPath = "";
                File imageFile = files.get(i);
                input = new FileInputStream(imageFile);
                image = new Image(input);
                sourcePath = files.get(i).getPath();
                try {
                    filePath = file.getPath();
                    if (files.size() > 1) {
                        destinationPath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
                        destinationPath += imageFile.getName().substring(0, imageFile.getName().lastIndexOf("."));
                        destinationPath += ("."+choiceBox.getValue().toString());
                    } else {
                        destinationPath = filePath;
                        destinationPath += ("." + choiceBox.getValue());
                    }

                    if (textHeight.getText().equals("") && textWidth.getText().equals("")) {
                        // if user do not input any number in the textwidth and textHeight feilds
                        // we assume user use the images' original width and height
                        width = (int) image.getWidth();
                        height = (int) image.getWidth();
                        System.out.println("width: "+width);
                        System.out.println("height: "+height);

                        convertImages(sourcePath,width,height,destinationPath);
                    } else if ((textHeight.getText().equals("") && !textWidth.getText().equals(""))
                            ||(!textHeight.getText().equals("") && textWidth.getText().equals("")) ){
                        // if user just input only one field, we output error info
                        errDisplay.setVisible(true);
                        chooseSize.setVisible(false);
                        didUpload = true;
                        break;
                    } else if (Integer.parseInt(textHeight.getText()) > 10000 || Integer.parseInt(textWidth.getText()) > 10000) {
                        // we do not allow user input textheight or textwidth bigger than 10000 px
                        errDisplay.setVisible(true);
                        errDisplay.setText("height or width not allowed \n to  be bigger than 10000");
                        chooseSize.setVisible(false);
                        didUpload = true;
                        break;
                    } else {
                        // if user input textwidth and textheight
                        // we use the user's input number
                        convertWidth = Integer.parseInt(textWidth.getText());
                        convertHeight = Integer.parseInt(textHeight.getText());

                        width = convertWidth;
                        height = convertHeight;
                        convertImages(sourcePath,width,height,destinationPath);
                    }
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
            }
            uploadCheck = false;
            checkBoxflip.setSelected(false);
            checkBoxedge.setSelected(false);
            checkBoxblur.setSelected(false);
            textWidth.clear();
            textHeight.clear();

        }else {
            notChoose.setText("upload image first");
            notChoose.setVisible(true);
        }
    }

    // helper function to help convert images
    private void convertImages(String sourcePath, int width, int height, String destinationPath){
        ConvertCmd cmd = new ConvertCmd();
        IMOperation op = new IMOperation();
        op.addImage(sourcePath); // add image from source path
        op.resize(width, height); // resize with width and height
        if (checkBoxblur.isSelected()) {
            op.blur(0.0, 8.0);  // if user select blur effect, blur the image
        }
        if (checkBoxedge.isSelected()) {
            op.edge(8.0);
        }
        if (checkBoxflip.isSelected()) {
            op.flip();
        }
        op.addImage(destinationPath); // out the converted image to destination path
        try {
            cmd.run(op);
            displayCon();// when successfully converted, show congratulations text and image

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    // force the textWidth and textHeight field to be numeric only
    @FXML
    private void numbericOnly() {
        textWidth.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    textWidth.setText(newValue.replaceAll("[^\\d]", ""));
                }

            }
        });

        textHeight.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    textHeight.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
    }

    // when successfully converted, show congratulations text and image
    private void displayCon() {
        chooseSize.setVisible(true); // show chooseSize info in case user had a error before this use
        errDisplay.setVisible(false); // clear errDisplay info in case user had a error before this use
        content_header.setVisible(false); // hide content-header if we the convert task is done
        gridpane.getChildren().clear(); // clear gridpane content

        // display congratulations text and image
        Label con1 = new Label("Congratulations! ");
        Label con2 = new Label("Your images have been converted!!! ");
        con1.setTextFill(javafx.scene.paint.Paint.valueOf("#ff0000"));
        con1.setFont(javafx.scene.text.Font.font("Calibri", FontWeight.EXTRA_BOLD, 20));
        con2.setTextFill(javafx.scene.paint.Paint.valueOf("#ff0000"));
        con2.setFont(javafx.scene.text.Font.font("Calibri", FontWeight.EXTRA_BOLD, 20));
        Class<?> clazz = this.getClass();
        InputStream input = clazz.getResourceAsStream("../image/con.png");

        Image image = new Image(input);
        ImageView imageView1 = new ImageView(image);
        imageView1.setFitWidth(200);
        imageView1.setFitHeight(200);

        gridpane.add(imageView1, 0, 1);
        gridpane.add(con1, 0, 2);
        gridpane.add(con2, 0, 3);
        // end of display congratulations text and image
    }

    // helper function to check the input number validation
    @FXML
    private boolean checkNumValid(){
        if (textHeight.getText().equals("") && textWidth.getText().equals("")) {
            errDisplay.setVisible(false);
            chooseSize.setVisible(true);
            didUpload = true;
            return true;

        }else if ((textHeight.getText().equals("") && !textWidth.getText().equals(""))
                ||(!textHeight.getText().equals("") && textWidth.getText().equals("")) ) {
            // if user just input only one field, we output error info
            errDisplay.setVisible(true);
            errDisplay.setText("Valid Number!");
            chooseSize.setVisible(false);
            return false;
        }else if (Integer.parseInt(textHeight.getText()) > 10000 || Integer.parseInt(textWidth.getText()) > 10000) {
            chooseSize.setVisible(false);
            // we do not allow user input textheight or textwidth bigger than 10000 px
            errDisplay.setVisible(true);
            errDisplay.setText("height or width not allowed \n to  be bigger than 10000");
            return false;
        } else {
            chooseSize.setVisible(true);
            errDisplay.setVisible(false);
            didUpload = true;
            return true;
        }
    }

    // clear the number invalid info
    @FXML
    private void clearNumValid(){
        errDisplay.setVisible(false);
        chooseSize.setVisible(true);
    }

    // when mouse enter button we change the background color
    @FXML
    private void uploadbuttonTextColorEnter(){
        uploadBtn.setStyle("-fx-background-color: #e35259");
    }
    @FXML
    private void choiceBoxColorChangeEnter(){
        choiceBox.setStyle(" -fx-background-color: #e35259");
    }
    @FXML
    private void convertbuttonTextColorEnter(){
        convertBtn.setStyle("-fx-background-color: #3bb1cb");
    }


    // when mouse exit button we change back the background color
    @FXML
    private void uploadbuttonTextColorExit(){
        uploadBtn.setStyle("-fx-background-color: #3bb1cb");
    }
    @FXML
    private void convertbuttonTextColorChangeExit(){
        convertBtn.setStyle("-fx-background-color:#e35259");
        notChoose.setVisible(false);
    }
    @FXML
    private void choiceBoxColorChangeExit(){
        choiceBox.setStyle(" -fx-background-color: #94cba4");
    }


    // when mouse enter checkbox we change the background color
    @FXML
    private void edgecheckboxChangColorEnter(){
        checkBoxedge.setStyle("box-color:#e35259");
    }
    @FXML
    private void blurcheckboxChangColorEnter(){
        checkBoxblur.setStyle("box-color:#e35259");
    }
    @FXML
    private void flipcheckboxChangColorEnter(){
        checkBoxflip.setStyle("box-color:#e35259");
    }


    // when mouse exit checkbox we change back the background color
    @FXML
    private void edgecheckboxChangColorExit(){
        checkBoxedge.setStyle("box-color: #94cba4");
    }
    @FXML
    private void blurcheckboxChangColorExit(){
        checkBoxblur.setStyle("box-color: #94cba4");
    }
    @FXML
    private void flipcheckboxChangColorExit(){
        checkBoxflip.setStyle("box-color: #94cba4");
    }
}
