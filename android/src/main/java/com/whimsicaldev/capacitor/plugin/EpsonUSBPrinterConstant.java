package com.whimsicaldev.capacitor.plugin;

import java.util.HashMap;
import java.util.Map;

public class EpsonUSBPrinterConstant {

    public static Map<String, byte[]> EPSON_STYLE_LIST;
    public static Map<String, byte[]> EPSON_COMMAND_LIST;
    public static String LN = "LINE_BREAK";
    public static String RESET = "RESET";

    static {
        EPSON_STYLE_LIST = new HashMap<>();
        EPSON_COMMAND_LIST = new HashMap<>();

        // text horizontal alignment
        EPSON_STYLE_LIST.put("ALIGN_LEFT", new byte[] {(byte) 27, (byte) 97, (byte) 0});
        EPSON_STYLE_LIST.put("ALIGN_CENTER", new byte[] {(byte) 27, (byte) 97, (byte) 1});
        EPSON_STYLE_LIST.put("ALIGN_JUSTIFIED", new byte[] {(byte) 27, (byte) 97, (byte) 1});
        EPSON_STYLE_LIST.put("ALIGN_RIGHT", new byte[] {(byte) 27, (byte) 97, (byte) 2});

        EPSON_STYLE_LIST.put("WIDE", new byte[] {(byte) 27, (byte) 33, (byte) 4});
        EPSON_STYLE_LIST.put("EMPHASIZED", new byte[] {(byte) 27, (byte) 33, (byte) 8});
        EPSON_STYLE_LIST.put("STRETCHED_WIDTH", new byte[] {(byte) 27, (byte) 33, (byte) 16});
        EPSON_STYLE_LIST.put("STRETCHED_HEIGHT", new byte[] {(byte) 27, (byte) 33, (byte) 32});
        EPSON_STYLE_LIST.put("UNDERLINED", new byte[] {(byte) 27, (byte) 33, (byte) 128});

        // reset the styling commands that are already set
        EPSON_COMMAND_LIST.put("RESET", new byte[] {(byte) 27, (byte) 64});
        EPSON_COMMAND_LIST.put("PARTIAL_CUT", new byte[] {(byte) 27, (byte) 105});
        EPSON_COMMAND_LIST.put("FULL_CUT", new byte[] {(byte) 27, (byte) 109});
        EPSON_COMMAND_LIST.put("LINE_BREAK", new byte[] {(byte) 10});
    }
}