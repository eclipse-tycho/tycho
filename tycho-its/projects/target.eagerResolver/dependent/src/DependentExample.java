/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
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
