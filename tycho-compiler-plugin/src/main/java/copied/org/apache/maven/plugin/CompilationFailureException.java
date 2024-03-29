/*
 * 	Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package copied.org.apache.maven.plugin;

import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.compiler.CompilerMessage;

public class CompilationFailureException extends MojoFailureException {
    /**
     * 
     */
    private static final long serialVersionUID = 6116801730028853785L;
    private static final String LS = System.lineSeparator();

    public CompilationFailureException(List<CompilerMessage> messages) {
        super(null, "Compilation failure", longMessage(messages));
    }

    public static String longMessage(List<CompilerMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (CompilerMessage compilerMessage : messages) {
            sb.append(compilerMessage).append(LS);
        }
        return sb.toString();
    }
}
