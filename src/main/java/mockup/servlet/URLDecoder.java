package mockup.servlet;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

import java.io.UnsupportedEncodingException;

@Replaces("java.net.URLDecoder")
public class URLDecoder {

    public static String decode(String s) {
        return TaintAPI.getTaintedString();
    }

    public static String decode(String s, String enc)
            throws UnsupportedEncodingException {
        return TaintAPI.getTaintedString();
    }
}
