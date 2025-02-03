import org.apache.commons.lang3.StringUtils;
import dependee.DependeeExample;

public class DependentExample {

    public static void main(String[] args) {
    	DependeeExample dependeeEx = new DependeeExample();
    	String s = " ";
        boolean sIsBlank = StringUtils.isBlank(s);

        System.out.println("s is blank:  " + sIsBlank);
        System.out.println("bitfield is: " + dependeeEx.getBitField());
    }
}
