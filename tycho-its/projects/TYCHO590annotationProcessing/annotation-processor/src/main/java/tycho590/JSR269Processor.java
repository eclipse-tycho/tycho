/*******************************************************************************
 * Copyright (c) 2011 BSB and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Demande (BSB group) - initial API and implementation
 *******************************************************************************/
package tycho590;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("*")
public class JSR269Processor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (!roundEnv.processingOver()) {

			final Filer filer = processingEnv.getFiler();
			try {
				final JavaFileObject sourceFile = filer.createSourceFile("tycho590.processed.Processor");
				Writer sourceWriter = sourceFile.openWriter();
				sourceWriter.append("package tycho590.processed;");
				sourceWriter.append(String.format("%n"));
				sourceWriter.append("public class Processor {}");
				sourceWriter.close();				
			} catch (IOException ex) {
				//That will happen on round 2 because source file already exists
				System.out.println("[APT] " + ex.getMessage());				
			}
		}
		return false;

	}
}
