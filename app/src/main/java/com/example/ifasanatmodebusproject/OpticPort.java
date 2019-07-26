package com.example.ifasanatmodebusproject;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

public class OpticPort {

    private static final String TAG = "OpticPort";


    private String MeterSerialNumber;
    private FT_Device ftDevice = null;
    private D2xxManager ftdid2xx;
    int DevCount = -1;
    private byte[] password;
    private Context context;

    public OpticPort(Context context, D2xxManager ftdid2xx) {
        this.context = context;
        this.ftdid2xx = ftdid2xx;
    }

    public int ConnectOpticPort() {
        try {
            int openIndex = 0;

            if (DevCount > 0)
                return DevCount;

            DevCount = ftdid2xx.createDeviceInfoList(context);

            if (DevCount > 0) {
                ftDevice = ftdid2xx.openByIndex(context, openIndex);

                if (ftDevice == null) {
                    return DevCount;
                }

                if (ftDevice.isOpen()) {
                    ftDevice.setBaudRate(9600);
                    ftDevice.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
                    ftDevice.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8,
                            D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE);
                    ftDevice.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0x00, (byte) 0x00);
                    ftDevice.setLatencyTimer((byte) 16);
                    ftDevice.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                    DevCount = 1;
                } else {
                }
            } else {
                new Exception("");
            }
        } finally {
            return DevCount;
        }
    }

    private void FTDIWrite(byte[] data) {
        ftDevice.write(data, data.length);
    }

    private byte[] FTDIRead(int len) {
        int checkTimes = 0;
        boolean bGetData = true;

        int rxq = 0;
        while (rxq < len) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rxq = ftDevice.getQueueStatus();

            checkTimes++;
            if (40 == checkTimes) {
                bGetData = false;
                break;
            }
        }

        if (bGetData) {
            byte[] InData = new byte[rxq];
            ftDevice.read(InData);
            return InData;
        }
        return null;
    }

    private void SetPassword(String password) {
        String Password = password;
        if (Password.equals("Factory")) {
            int tmp = 0x8ABC;
            this.password = new byte[]{(byte) (tmp >> 8), (byte) (tmp & 0xff), (byte) 0xFF, (byte) 0xFF};
        } else {
            String tmp = Password.substring(0, Password.length() - 2);
            int id = Integer.parseInt(Password.substring(Password.length() - 2));
            int pass = CRC_Calculate(tmp.getBytes(), tmp.length());
            this.password = new byte[]{(byte) (pass >> 8), (byte) (pass & 0xff), (byte) (id >> 8), (byte) (id & 0xff)};
        }
    }


    public boolean Connect(String password) {
        Login(password);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] bytes = Read(MeterRegisters.MeterSerialNumber.getRegister(), MeterRegisters.MeterSerialNumber.getLen());
        if (bytes == null) return false;

        String sn = Helper.ConvertFromBytes("uint32", bytes);
        if (sn.isEmpty()) {
            return false;
        } else {
            this.MeterSerialNumber = sn;
            return true;
        }


    }

    private void WriteModbus(int register, int len, byte[] value) {
        int tx_packet_len = 6 + (len * 2);
        byte[] tx_buff = new byte[tx_packet_len];
        //check port is open
        tx_buff[0] = 0;                   // slave address
        tx_buff[1] = 0x06;                      // command code
        tx_buff[2] = (byte) (register >> 8);     // register address H
        tx_buff[3] = (byte) (register & 0xff);   // register address L

        for (int i = 0; i < len * 2; i++)
            tx_buff[4 + i] = value[i];           // Data Bytes

        int crc_temp = CRC_Calculate(tx_buff, tx_packet_len - 2);
        tx_buff[tx_packet_len - 2] = (byte) (crc_temp >>> 8);     // CRC H
        tx_buff[tx_packet_len - 1] = (byte) (crc_temp & 0xff);   // CRC L
        FTDIWrite(tx_buff);

    }

    private byte[] Read(int register, int len) {
        byte[] tx_buff = new byte[8];

        int rx_packet_len = 5 + (len * 2);
        //if port was not open
        if (ftDevice.isOpen()) {
            tx_buff[0] = (byte) 0;
            tx_buff[1] = 0x03;
            tx_buff[2] = (byte) (register >> 8);
            tx_buff[3] = (byte) (register & 0xff);
            tx_buff[4] = (byte) (len >> 8);
            tx_buff[5] = (byte) (len & 0xff);
            int crc_temp = CRC_Calculate(tx_buff, 6);
            tx_buff[6] = (byte) (crc_temp >> 8);
            tx_buff[7] = (byte) (crc_temp & 0xff);
            byte[] rx_buff = new byte[rx_packet_len];
            try {
                FTDIWrite(tx_buff);
                Thread.sleep(100);
                rx_buff = FTDIRead(rx_packet_len);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (rx_buff != null) {
                crc_temp = CRC_Calculate(rx_buff, rx_packet_len - 2);
                int crc_check = ((rx_buff[rx_packet_len - 2] & 0xff) << 8);
                crc_check = crc_check | ((rx_buff[rx_packet_len - 1]) & 0xFF);
                if (crc_temp == crc_check) {
                    byte[] outdata = new byte[rx_buff[2]];
                    for (int i = 0; i < rx_buff[2]; i++)
                        outdata[i] = rx_buff[3 + i];
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return outdata;
                }
            }
        }
        return null;
    }

    private void Login(String password) {
        assert password != null;
        if (!password.isEmpty()) SetPassword(password);
        if (password.equals("Factory")) WriteModbus(0x5A, 2, this.password);
        else WriteModbus(0x54, 2, this.password);
    }


    //data
    private int CRC_Calculate(byte[] buf, int len) {
        int crc;
        short thisbyte;
        short shift;
        byte lastbit, i;
        crc = -1;
        for (i = 0; i < len; i++) {
            thisbyte = buf[i];
            crc = (int) (crc ^ (thisbyte & 0xFF));
            for (shift = 1; shift <= 8; shift++) {
                lastbit = (byte) (crc & 1);
                crc = (int) ((crc >> 1) & 0x7fff);
                if (lastbit == 1) {
                    crc = (int) (crc ^ 0xA001);
                }
            }
        }
        crc = (int) (((crc << 8) & 0xff00) | (crc >> 8));
        return crc;
    }

    public static float bin2float(int bits) {
        return Float.intBitsToFloat(bits);
    }

    private byte[] GetBytes(long value) {

        byte[] a = {
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF),
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF)
        };

        return a;
    }

    private byte[] GetBytes(float value) {
        return GetBytes(Float.floatToIntBits(value));

    }

    public static byte[] Convert_Persian_Encode(String PersianText) {
        byte[] tmp = new byte[PersianText.length()];

        for (int i = 0; i < PersianText.length(); i++) {
            switch (PersianText.charAt(i)) {
                case 'ا':
                    tmp[i] = (byte) 199;
                    break;
                case 'آ':
                    tmp[i] = (byte) 194;
                    break;
                case 'ب':
                    tmp[i] = (byte) 200;
                    break;
                case 'پ':
                    tmp[i] = (byte) 129;
                    break;
                case 'ت':
                    tmp[i] = (byte) 202;
                    break;
                case 'ث':
                    tmp[i] = (byte) 203;
                    break;
                case 'ج':
                    tmp[i] = (byte) 204;
                    break;
                case 'چ':
                    tmp[i] = (byte) 141;
                    break;
                case 'ح':
                    tmp[i] = (byte) 205;
                    break;
                case 'خ':
                    tmp[i] = (byte) 206;
                    break;
                case 'د':
                    tmp[i] = (byte) 207;
                    break;
                case 'ذ':
                    tmp[i] = (byte) 208;
                    break;
                case 'ر':
                    tmp[i] = (byte) 209;
                    break;
                case 'ز':
                    tmp[i] = (byte) 210;
                    break;
                case 'ژ':
                    tmp[i] = (byte) 142;
                    break;
                case 'س':
                    tmp[i] = (byte) 211;
                    break;
                case 'ش':
                    tmp[i] = (byte) 212;
                    break;
                case 'ص':
                    tmp[i] = (byte) 213;
                    break;
                case 'ض':
                    tmp[i] = (byte) 214;
                    break;
                case 'ط':
                    tmp[i] = (byte) 216;
                    break;
                case 'ظ':
                    tmp[i] = (byte) 217;
                    break;
                case 'ع':
                    tmp[i] = (byte) 218;
                    break;
                case 'غ':
                    tmp[i] = (byte) 219;
                    break;
                case 'ف':
                    tmp[i] = (byte) 221;
                    break;
                case 'ق':
                    tmp[i] = (byte) 222;
                    break;
                case 'ک':
                    tmp[i] = (byte) 223;
                    break;
                case 'گ':
                    tmp[i] = (byte) 144;
                    break;
                case 'ل':
                    tmp[i] = (byte) 225;
                    break;
                case 'م':
                    tmp[i] = (byte) 227;
                    break;
                case 'ن':
                    tmp[i] = (byte) 228;
                    break;
                case 'و':
                    tmp[i] = (byte) 230;
                    break;
                case 'ه':
                    tmp[i] = (byte) 229;
                    break;
                case 'ی':
                    tmp[i] = (byte) 236;
                    break;

                default:
                    tmp[i] = (byte) (PersianText.charAt(i));
                    break;
            }
        }

        return tmp;
    }

    public static String Convert_Persian_Decode(byte[] Bytes) {
        String tmp = "";

        for (int i = 0; i < Bytes.length; i++) {
            switch ((int) Bytes[i]) {
                case 0:
                    break;
                case 199:
                    tmp += 'ا';
                    break;
                case 194:
                    tmp += 'آ';
                    break;
                case 200:
                    tmp += 'ب';
                    break;
                case 129:
                    tmp += 'پ';
                    break;
                case 202:
                    tmp += 'ت';
                    break;
                case 203:
                    tmp += 'ث';
                    break;
                case 204:
                    tmp += 'ج';
                    break;
                case 141:
                    tmp += 'چ';
                    break;
                case 205:
                    tmp += 'ح';
                    break;
                case 206:
                    tmp += 'خ';
                    break;
                case 207:
                    tmp += 'د';
                    break;
                case 208:
                    tmp += 'ذ';
                    break;
                case 209:
                    tmp += 'ر';
                    break;
                case 210:
                    tmp += 'ز';
                    break;
                case 142:
                    tmp += 'ژ';
                    break;
                case 211:
                    tmp += 'س';
                    break;
                case 212:
                    tmp += 'ش';
                    break;
                case 213:
                    tmp += 'ص';
                    break;
                case 214:
                    tmp += 'ض';
                    break;
                case 216:
                    tmp += 'ط';
                    break;
                case 217:
                    tmp += 'ظ';
                    break;
                case 218:
                    tmp += 'ع';
                    break;
                case 219:
                    tmp += 'غ';
                    break;
                case 221:
                    tmp += 'ف';
                    break;
                case 222:
                    tmp += 'ق';
                    break;
                case 223:
                    tmp += 'ک';
                    break;
                case 144:
                    tmp += 'گ';
                    break;
                case 225:
                    tmp += 'ل';
                    break;
                case 227:
                    tmp += 'م';
                    break;
                case 228:
                    tmp += 'ن';
                    break;
                case 230:
                    tmp += 'و';
                    break;
                case 229:
                    tmp += 'ه';
                    break;
                case 236:
                case 237:
                    tmp += 'ی';
                    break;
                default:
                    tmp += (char) (Bytes[i]);
                    break;
            }
        }

        return tmp;
    }

    public static byte Compare_Time(int Time1_Year, byte Time1_Month, byte Time1_Day, byte Time1_Hour, byte Time1_Minute, byte Time1_Secound,
                                    int Time2_Year, byte Time2_Month, byte Time2_Day, byte Time2_Hour, byte Time2_Minute, byte Time2_Secound)    //  Time1 == Time2 => 0 , Time1 > Time2 => 1 , Time1 < Time2 => 2
    {
        if (Time1_Year == Time2_Year) {
            if (Time1_Month == Time2_Month) {
                if (Time1_Day == Time2_Day) {
                    if (Time1_Hour == Time2_Hour) {
                        if (Time1_Minute == Time2_Minute) {
                            if (Time1_Secound == Time2_Secound) {
                                return 0;
                            } else if (Time1_Secound > Time2_Secound)
                                return 1;
                            else
                                return 2;
                        } else if (Time1_Minute > Time2_Minute)
                            return 1;
                        else
                            return 2;
                    } else if (Time1_Hour > Time2_Hour)
                        return 1;
                    else
                        return 2;
                } else if (Time1_Day > Time2_Day)
                    return 1;
                else
                    return 2;
            } else if (Time1_Month > Time2_Month)
                return 1;
            else
                return 2;
        } else if (Time1_Year > Time2_Year)
            return 1;
        else
            return 2;
    }

    public String getMeterSerialNumber() {
        return MeterSerialNumber;
    }


}

