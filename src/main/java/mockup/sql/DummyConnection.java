package mockup.sql;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Replaces("java.sql.Connection")
public abstract class DummyConnection implements Connection {

    public PreparedStatement prepareStatement(String sql)
            throws SQLException {
        TaintAPI.outputString(sql);
        return null;
    }
}
