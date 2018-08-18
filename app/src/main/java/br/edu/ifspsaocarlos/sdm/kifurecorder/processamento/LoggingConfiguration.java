package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import java.util.HashSet;
import java.util.Set;

public class LoggingConfiguration {

    public static int LOGGING_ENABLED = 1;

    // Images
    public static int RAW_CAMERA_IMAGE = 2;
    public static int CAMERA_IMAGE_WITH_BOARD_CONTOUR = 3;
    public static int ORTOGONAL_BOARD_IMAGE = 4;
    public static int CORNER_REGIONS_IMAGES = 5;

    // Processing objects
    public static int CORNER_POSITIONS = 6;
    public static int STONE_DETECTION_INFORMATION = 7;

    private static Set<Integer> activatedFlags = new HashSet<>();

    private LoggingConfiguration() {};

    public static void activateLogging() {
        activatedFlags.add(LOGGING_ENABLED);
    }

    public static void activateLogging(int flag) {
        activatedFlags.add(flag);
    }

    public static void deactivateLogging() {
        activatedFlags.remove(LOGGING_ENABLED);
    }

    public static void deactivateLogging(int flag) {
        activatedFlags.remove(flag);
    }

    public static boolean shouldLog(int flag) {
        return isLoggingEnabled() && activatedFlags.contains(flag);
    }

    private static boolean isLoggingEnabled() {
        return activatedFlags.contains(LOGGING_ENABLED);
    }

}
