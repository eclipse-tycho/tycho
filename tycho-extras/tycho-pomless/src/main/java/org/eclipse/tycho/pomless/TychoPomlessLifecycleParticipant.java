/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 *  
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;

@Named("TychoPomlessLifecycleParticipant")
@Singleton
public class TychoPomlessLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Inject
    protected Logger logger;

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
//        File moduleProjectDirectory = session.getRequest().getMultiModuleProjectDirectory();
//        if (moduleProjectDirectory != null) {
//            File extensionsFile = new File(moduleProjectDirectory, ".mvn/extensions.xml");
//            try (InputStream is = new FileInputStream(extensionsFile)) {
//                List<CoreExtension> extensions = new CoreExtensionsXpp3Reader().read(is).getExtensions();
//                for (CoreExtension coreExtension : extensions) {
//                    if ("org.eclipse.tycho.extras".equals(coreExtension.getGroupId())
//                            && "tycho-pomless".equals(coreExtension.getArtifactId())) {
//                        logger.warn(
//                                "org.eclipse.tycho.extras:tycho-pomless build extension will be replaced in a future version of Tycho by the new org.eclipse.tycho:tycho-build extension.");
//                        logger.warn(
//                                "You can simply change your .mvn/extensions.xml to reference the new extension right now:");
//                        logger.warn("<extension>");
//                        logger.warn("   <groupId>org.eclipse.tycho</groupId>");
//                        logger.warn("   <artifactId>tycho-build</artifactId>");
//                        logger.warn("   <version>" + coreExtension.getVersion() + "</version>");
//                        logger.warn("</extension>");
//                        break;
//                    }
//                }
//
//            } catch (IOException | XmlPullParserException e) {
//                // //don't care, we just wan't to inform the user...
//            }
//        }
    }
}
