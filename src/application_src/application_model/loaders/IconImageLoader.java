package application_src.application_model.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Loader for all icon images used in the application
 */
public class IconImageLoader {

    private static final String ENTRY_PREFIX = "/application_src/views/icons/";
    private static final String PATH_FROM_ROOT = "application_src/views/icons/";
    private static final String BACKWARD_PNG = "backward.png";
    private static final String FORWARD_PNG = "forward.png";
    private static final String PAUSE_PNG = "pause.png";
    private static final String PLAY_PNG = "play.png";
    private static final String EDIT_PNG = "edit.png";
    private static final String EYE_PNG = "eye.png";
    private static final String EYE_INV_PNG = "eye-invert.png";
    private static final String CLOSE_PNG = "close.png";
    private static final String PLUS_PNG = "plus.png";
    private static final String MINUS_PNG = "minus.png";
    private static final String COPY_PNG = "copy.png";
    private static final String PASTE_PNG = "paste.png";

    private static ImageView forward, backward, play, pause;
    private static Image plus, minus;
    private static Image edit, eye, eyeInvert, close;
    private static Image copy;
    private static ImageView paste;

    public static void loadImages() {

        try {
            final URL urlBack = IconImageLoader.class.getResource(ENTRY_PREFIX + BACKWARD_PNG);
            processImage(urlBack);

            final URL urlFor = IconImageLoader.class.getResource(ENTRY_PREFIX + FORWARD_PNG);
            processImage(urlFor);

            final URL urlClose = IconImageLoader.class.getResource(ENTRY_PREFIX + CLOSE_PNG);
            processImage(urlClose);

            final URL urlCopy = IconImageLoader.class.getResource(ENTRY_PREFIX + COPY_PNG);
            processImage(urlCopy);

            final URL urlEdit = IconImageLoader.class.getResource(ENTRY_PREFIX + EDIT_PNG);
            processImage(urlEdit);

            final URL urlEye = IconImageLoader.class.getResource(ENTRY_PREFIX + EYE_PNG);
            processImage(urlEye);

            final URL urlEyei = IconImageLoader.class.getResource(ENTRY_PREFIX + EYE_INV_PNG);
            processImage(urlEyei);

            final URL urlMinus = IconImageLoader.class.getResource(ENTRY_PREFIX + MINUS_PNG);
            processImage(urlMinus);

            final URL urlPaste = IconImageLoader.class.getResource(ENTRY_PREFIX + PASTE_PNG);
            processImage(urlPaste);

            final URL urlPause = IconImageLoader.class.getResource(ENTRY_PREFIX + PAUSE_PNG);
            processImage(urlPause);

            final URL urlPlay = IconImageLoader.class.getResource(ENTRY_PREFIX + PLAY_PNG);
            processImage(urlPlay);

            final URL urlPlus = IconImageLoader.class.getResource(ENTRY_PREFIX + PLUS_PNG);
            processImage(urlPlus);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void processImage(URL url) throws IOException {
        if (url != null) {
            final InputStream input = url.openStream();
            final Image image = new Image(input);
            String urlStr = url.getFile();
            urlStr = urlStr.substring(urlStr.indexOf("application_src"));
            switch (urlStr) {
                case PATH_FROM_ROOT + EDIT_PNG:
                    edit = image;
                    return;
                case PATH_FROM_ROOT + EYE_PNG:
                    eye = image;
                    return;
                case PATH_FROM_ROOT + EYE_INV_PNG:
                    eyeInvert = image;
                    return;
                case PATH_FROM_ROOT + CLOSE_PNG:
                    close = image;
                    return;
                case PATH_FROM_ROOT + COPY_PNG:
                    copy = image;
                    return;
                case PATH_FROM_ROOT + PLUS_PNG:
                    plus = image;
                    return;
                case PATH_FROM_ROOT + MINUS_PNG:
                    minus = image;
                    return;
            }
            final ImageView icon = new ImageView(image);
            switch (urlStr) {
                case PATH_FROM_ROOT + BACKWARD_PNG:
                    backward = icon;
                    break;
                case PATH_FROM_ROOT + FORWARD_PNG:
                    forward = icon;
                    break;
                case PATH_FROM_ROOT + PLAY_PNG:
                    play = icon;
                    break;
                case PATH_FROM_ROOT + PAUSE_PNG:
                    pause = icon;
                    break;
                case PATH_FROM_ROOT + PASTE_PNG:
                    paste = icon;
            }
        }
    }

    public static ImageView getForwardIcon() {
        return forward;
    }

    public static ImageView getBackwardIcon() {
        return backward;
    }

    public static ImageView getPlayIcon() {
        return play;
    }

    public static ImageView getPauseIcon() {
        return pause;
    }

    public static Image getPlusIcon() {
        return plus;
    }

    public static Image getMinusIcon() {
        return minus;
    }

    public static ImageView getEditIcon() {
        return new ImageView(edit);
    }

    public static ImageView getEyeIcon() {
        return new ImageView(eye);
    }

    public static ImageView getEyeInvertIcon() {
        return new ImageView(eyeInvert);
    }

    public static ImageView getCloseIcon() {
        return new ImageView(close);
    }

    public static ImageView getCopyIcon() {
        return new ImageView(copy);
    }

    public static ImageView getPasteIcon() {
        return paste;
    }
}
