package gopher.io;

import com.google.common.collect.ImmutableList;
import gopher.model.RestrictionEnzyme;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RestrictionEnzymeParser {

    private final String pathToRestrictionEnzymeFile;

    public RestrictionEnzymeParser(String path) {
        pathToRestrictionEnzymeFile=path;
    }


    public List<RestrictionEnzyme> getEnzymes() {
        ImmutableList.Builder<RestrictionEnzyme> builder = new ImmutableList.Builder<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(pathToRestrictionEnzymeFile));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue; /* skip header*/
                String a[] = line.split("\\s+");
                RestrictionEnzyme re = new RestrictionEnzyme(a[0], a[1]);
                builder.add(re);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.build();
    }

}
