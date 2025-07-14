/**
 MIT License

Copyright (c) 2010 Anthony Whitford

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package org.projectlombok.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;
// http://code.google.com/p/projectlombok/issues/detail?id=146
//import lombok.NonNull;

@Data
public class AnotherDataExample implements Useful {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnotherDataExample.class);

    //@NonNull
    private final DataExample dataExample;

    @NotNull
    private final String moreInformation;

    @Override
	public void doSomething() {
        LOGGER.debug("Doing something useful...");
    }

    // Example related to http://code.google.com/p/projectlombok/issues/detail?id=269
    @XmlType
    @XmlEnum(Integer.class)
    public enum Coin { 
        @XmlEnumValue("1") PENNY(1),
        @XmlEnumValue("5") NICKEL(5),
        @XmlEnumValue("10") DIME(10),
        @XmlEnumValue("25") QUARTER(25);

        private final int cents;

        Coin (final int cents) {
            this.cents = cents;
        }
        
        public int getCents () {
        	return cents;
        }
    }
}
