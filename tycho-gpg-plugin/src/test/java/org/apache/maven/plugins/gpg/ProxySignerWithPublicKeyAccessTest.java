/*******************************************************************************
 * Copyright (c) 2022 Konrad Windszus.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.apache.maven.plugins.gpg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class ProxySignerWithPublicKeyAccessTest {

    @Test
    void testExtractFingerprint() throws IOException {
        String exampleOutput = "sec:-:4096:1:8CC10D97B7FE12A7:1423233945:::f:::scESC:::+:::23::0:\n"
                + "fpr:::::::::1AFBC35D645EB43A91E382BF8CC10D97B7FE12A7:\n"
                + "grp:::::::::D523117FB662FF9ED4230C39E20C21D84956D8FB:\n"
                + "uid:-::::1423233945::0140A26DEEE45B2C1B0BF2446A6EDAB996E2544B::Konrad Windszus (Senior Solutions Architect at Netcentric) <konrad.windszus@netcentric.biz>::::::::::0:\n"
                + "ssb:-:4096:1:DCFCC1CEE161D45D:1423233945::::::e:::+:::23:\n"
                + "fpr:::::::::502FCC921A57F6CE71E3E2A4DCFCC1CEE161D45D:\n"
                + "grp:::::::::434D24AFD1C23C7A1D965417754497C7DF04977D:\n"
                + "sec:-:2048:1:D7742D58455ECC7C:1453392224:::f:::scESC:::+:::23::0:\n"
                + "fpr:::::::::B91AB7D2121DC6B0A61AA182D7742D58455ECC7C:\n"
                + "grp:::::::::BA5BE58EB5C86385CB69CB416EC648483168B017:\n"
                + "uid:-::::1453392224::227820B35E2F998546D17962E5EDD8DA2F0336D9::Konrad Windszus <kwin@apache.org>::::::::::0:\n"
                + "ssb:-:2048:1:97F326B2226BCE00:1453392224::::::e:::+:::23:\n"
                + "fpr:::::::::773B0A7C5A291C08C7B4312997F326B2226BCE00:\n"
                + "grp:::::::::F7B254B3824FE5FF0DA93A8E36A669E7B45249B4:";
        assertEquals("1AFBC35D645EB43A91E382BF8CC10D97B7FE12A7",
                ProxySignerWithPublicKeyAccess.extractFingerprint(exampleOutput));
    }

}
