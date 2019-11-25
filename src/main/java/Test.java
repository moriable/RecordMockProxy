import org.apache.commons.codec.net.URLCodec;

import java.io.UnsupportedEncodingException;

public class Test {
    public static void main(String[] args) {
        String a = "0123456789";
        System.out.println(a.substring(3));
        System.out.println(a.substring(0, 10));

        try {
            System.out.println(new URLCodec().encode("|^[]{}!<>'*@", "UTF-8"));
        } catch (UnsupportedEncodingException e) { }
    }
}
