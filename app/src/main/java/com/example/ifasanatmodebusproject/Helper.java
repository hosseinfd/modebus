package com.example.ifasanatmodebusproject;

public class Helper {
    public static String ConvertFromBytes(String dataType, byte[] value) {
        try {
            {
                switch (dataType) {
                    case "uint32": {
                        return String.valueOf(
                                ((value[0] & 0xff) << 8) |
                                        (value[1] & 0xff) |
                                        ((value[2] & 0xff) << 24) |
                                        ((value[3] & 0xff) << 16)
                        );
                    }
                    case "":
                        return "" ;
                    default:
                        try {
                            throw new Exception("");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }
}
