/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *******************************************************************************/
 
 def checkFile = { fileName, message ->
    File file = new File(basedir, fileName);
    if (!file.isFile()) {
      throw new Exception( message + ": " + file );
    }
 }
 
 def checkFileContains = { fileName, content ->
    File file = new File(basedir, fileName);
    String fileContent = file.text
    if (!fileContent.contains(content)) {
      throw new Exception("Expected content '" + content + "' could not be found in file " + file);
    }
 }

 // check if all the classpath entries are found
 checkFileContains ( "doc.bundle/target/javadoc.options.txt", "/my.bundle/target/classes" );
 checkFileContains ( "doc.bundle/target/javadoc.options.txt", "/my.bundle/lib/osgi.annotation-7.0.0.jar" );
 
 // check if the expected java doc files are generated
 checkFile ( "doc.bundle/target/tocjavadoc.xml", "Missing expected toc file" ); 
 checkFile ( "doc.bundle/target/reference/api/element-list", "Missing element list file" );
 checkFile ( "doc.bundle/target/reference/api/my/bundle/SampleClass1.html", "Missing doc file" );
 
 // check that annotations are represented in the java doc files
 checkFileContains ( "doc.bundle/target/reference/api/my/bundle/package-summary.html", "@Version(\"1.0.0\")" );
 checkFileContains ( "doc.bundle/target/reference/api/my/bundle/SampleClass1.html", "@ProviderType" );
 
 return true;
