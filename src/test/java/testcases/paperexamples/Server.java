package testcases.paperexamples;

public class Server {

    void serve() {
        while(hasQuery()) {
            String query = nextQuery();
            boolean authorized = verifyAuthorization(); // emits 'authcheck';
            if(authorized) {
                readSensitiveData(); // emits 'access';
            }
        }
        logAccess(); // emits 'log';
    }

    boolean cond;

    boolean hasQuery() {
        return cond;
    }

    String nextQuery() {
        return "";
    }

    boolean verifyAuthorization() {
        return cond;
    }

    void readSensitiveData() { }

    void logAccess() { }
}
