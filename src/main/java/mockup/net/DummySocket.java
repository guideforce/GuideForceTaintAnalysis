package mockup.net;

import mockup.Replaces;
import mockup.servlet.DummyServletInputStream;
import ourlib.nonapp.TaintAPI;

import java.io.IOException;
import java.io.InputStream;

@Replaces("java.net.Socket")
public class DummySocket implements java.io.Closeable{
    @Override
    public void close() throws IOException {

    }

    public InputStream getInputStream() throws IOException {
        return new DummyServletInputStream(TaintAPI.getTaintedString());
    }
}
