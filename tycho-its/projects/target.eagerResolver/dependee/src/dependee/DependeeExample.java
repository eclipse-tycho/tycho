package dependee;

import org.apache.commons.lang3.BitField;
import org.apache.commons.lang3.StringUtils;

public class DependeeExample {

    public DependeeExample() { }

    public boolean isStringBlank(String s) {
        return StringUtils.isBlank(s);
    }
    
    public BitField getBitField() {
    	return new BitField(0xFFFFFF);
    }

}
