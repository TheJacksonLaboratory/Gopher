package gopher.io;

import com.google.common.collect.ImmutableList;
import gopher.service.model.RestrictionEnzyme;

import java.io.*;
import java.util.List;

public class RestrictionEnzymeParser {

    private final File pathToRestrictionEnzymeFile;


    public RestrictionEnzymeParser(File pathToRestrictionEnzymeFile) {
        this.pathToRestrictionEnzymeFile = pathToRestrictionEnzymeFile;
    }


    public static List<RestrictionEnzyme> getEnzymes(InputStream inputStream) throws IOException {
        ImmutableList.Builder<RestrictionEnzyme> builder = new ImmutableList.Builder<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue; /* skip header*/
                String[] a = line.split("\\s+");
                RestrictionEnzyme re = new RestrictionEnzyme(a[0], a[1]);
                builder.add(re);
            }
        }
        return builder.build();
    }


    public List<RestrictionEnzyme> getEnzymes() throws IOException {
        return getEnzymes(new FileInputStream(pathToRestrictionEnzymeFile));
    }

}
