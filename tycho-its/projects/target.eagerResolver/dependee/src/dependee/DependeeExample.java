/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
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
