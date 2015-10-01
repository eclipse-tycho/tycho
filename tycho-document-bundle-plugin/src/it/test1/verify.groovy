/*******************************************************************************
 * Copyright (c) 2014 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBH SYSTEMS GmbH - initial API and implementation
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

// check if the encoding is set
 checkFileContains ( "docbundle1/target/javadoc.options.txt", "-encoding UTF-8" );

// check if the doclet artifacts are passed as path
 checkFileContains ( "docbundle1/target/javadoc.options.txt", "-docletpath" );
 
 // check if the transitive dependencies are also resolved
 checkFileContains ( "docbundle1/target/javadoc.options.txt", "tycho-maven-plugin" );
 
 // check if the custom doclet is used
 checkFileContains ( "docbundle1/target/javadoc.options.txt", "-doclet CustomDoclet" );
 
 // check if the expected java doc files are generated 
 checkFile ( "docbundle1/target/gen-doc/toc/javadoc.xml", "Missing expected toc file" ); 
 checkFile ( "docbundle1/target/gen-doc/reference/api/package-list", "Missing package list file" );
 checkFile ( "docbundle1/target/gen-doc/reference/api/bundle1/SampleClass1.html", "Missing doc file" );
 
 // check if the toc file contains the custom label and custom summary file
 checkFileContains ( "docbundle1/target/gen-doc/toc/javadoc.xml", "reference/api/custom-overview.html" );
 checkFileContains ( "docbundle1/target/gen-doc/toc/javadoc.xml", "The custom main label" );
  
 return true;