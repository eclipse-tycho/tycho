/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
import java.io.*;

File marker = new File(basedir, "target/.run.ok"); 
if (!marker.isFile()) {
  throw new Exception( "Missing expected marker file " + marker );
}

return true;
