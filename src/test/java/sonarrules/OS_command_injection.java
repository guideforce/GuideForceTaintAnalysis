/**
 * Rule: OS commands should not be vulnerable to command injection attacks.
 * https://rules.sonarsource.com/java/type/Vulnerability/RSPEC-2076
 */

package sonarrules;

import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/** 
 *  @servlet description="moderately complex test of derived strings" 
 *  @servlet vuln_count = "3" 
 *  */
public class OS_command_injection extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String cmd = req.getParameter("command");
        String arg = req.getParameter("arg");

        Runtime.getRuntime().exec(cmd+" "+arg); // Noncompliant
    }
    
    public String getDescription() {
        return "OS commands should not be vulnerable to command injection attacks";
    }
    
    public int getVulnerabilityCount() {
        return 3;
    }
}