
package com.stegmannsystems.debugtychosurefire.bundle;

import javax.xml.validation.SchemaFactory;

/**
 * @author schulz
 */
public class HelloWorld
{

    public String test()
    {
        final SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$
        if (sf == null) {
            // fail
        }
        return "Hello World";
    }

}
