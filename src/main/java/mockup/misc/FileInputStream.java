package mockup.misc;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

import java.io.FileNotFoundException;

@Replaces("java.io.FileInputStream")
public class FileInputStream {
    public FileInputStream(String name) throws FileNotFoundException {
        TaintAPI.outputString(name);
    }
}
