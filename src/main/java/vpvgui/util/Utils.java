package vpvgui.util;

import java.util.List;

public class Utils {



    public static String join(List<String> lst, String delim) {
        if (lst==null || lst.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append(lst.get(0));
        for (int i=1;i<lst.size();i++) {
            sb.append(String.format("%s%s",delim,lst.get(i)));
        }
        return sb.toString();
    }


}
