package mockup.misc;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

import java.io.IOException;

@Replaces("java.io.FileWriter")
public class FileWriter {
    public FileWriter (String path)  throws IOException {
        TaintAPI.outputString(path);
    }
}
