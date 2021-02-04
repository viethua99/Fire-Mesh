package com.ceslab.firemesh.util;

/**
 * Created by Viet Hua on 11/24/2020.
 */

public class ConverterUtil {
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    // Gets value in hexadecimal system
    public static String getHexValue(byte value[]) {
        if (value == null) {
            return "";
        }

        char[] hexChars = new char[value.length * 3];
        int v;
        for (int j = 0; j < value.length; j++) {
            v = value[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    public static String getHexValueNoSpace(byte value[]) {
        if (value == null) {
            return "";
        }

        char[] hexChars = new char[value.length * 2];
        int v;
        for (int j = 0; j < value.length; j++) {
            v = value[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] inv_atou16(int value) {
        return new byte[] {(byte) (value >> 8),
                (byte) value};
    }

    public static byte[] inv_atou32(int value) {
        return new byte[] {(byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value};
    }
}