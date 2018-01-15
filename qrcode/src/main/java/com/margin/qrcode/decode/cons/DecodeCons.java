package com.margin.qrcode.decode.cons;

/**
 * Created by margin on 2017/12/2.
 */

public class DecodeCons {
    public static class Type {
        public static final int DECODE = 0x0000;
        public static final int QUIT = 0x0001;
    }
    public static class Status {
        public static final int SUCCESS = 0xff00;
        public static final int FAIL = 0xff01;
        public static final int RESTART_PREVIEW = 0xff02;
        public static final int RETURN_SCAN_RESULT = 0xff03;
        public static final int LAUNCH_PRODUCT_QUERY = 0xff04;
    }
}
