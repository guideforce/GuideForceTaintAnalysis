package mockup.misc;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

import java.io.IOException;

@Replaces("java.io.File")
public class File {

    private String path;

    public File (String pathname) {
        path = pathname;
    }

    public boolean createNewFile() throws IOException {
        TaintAPI.outputString(path);
        return true;
    }
}
