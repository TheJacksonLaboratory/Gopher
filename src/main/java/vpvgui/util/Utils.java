package vpvgui.util;

import java.util.List;
import java.util.StringJoiner;

public class Utils {



    public static String join(List<String> lst, String delim) {
        StringJoiner stringJoiner = new StringJoiner(delim);
        for (String item:lst) {
            stringJoiner.add(item);
        }
        return  stringJoiner.toString();
    }


}
