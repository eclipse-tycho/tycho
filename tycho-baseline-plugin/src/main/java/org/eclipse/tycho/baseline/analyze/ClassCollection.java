/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.baseline.analyze;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A collection of {@link ClassMethods} indexed by class name, providing lookup
 * and chaining capabilities for class method resolution.
 */
public class ClassCollection implements Function<String, Optional<ClassMethods>>, Consumer<ClassMethods> {

	private Map<String, ClassMethods> classLookupMap = new HashMap<>();

	@Override
	public Optional<ClassMethods> apply(String className) {
		return Optional.ofNullable(classLookupMap.get(className));
	}

	/**
	 * @return a stream of all method signatures provided by all classes in this
	 *         collection
	 */
	public Stream<MethodSignature> provides() {

		return classLookupMap.values().stream().distinct().flatMap(cm -> cm.provides());
	}

	/**
	 * Returns all method signatures provided by the given class name.
	 *
	 * @param className the fully qualified class name to look up
	 * @return the list of provided method signatures, or an empty list
	 */
	public List<MethodSignature> get(String className) {
		return apply(className).stream().flatMap(cm -> cm.provides()).toList();
	}

	/**
	 * Creates a resolver that first consults this collection, then falls back to the
	 * given chained resolver.
	 *
	 * @param chained the fallback resolver
	 * @return a composed resolver function
	 */
	public Function<String, Optional<ClassMethods>> chain(Function<String, Optional<ClassMethods>> chained) {
		return cls -> {
			return apply(cls).or(() -> chained.apply(cls));
		};
	}

	@Override
	public void accept(ClassMethods methods) {
		methods.definitions().forEach(def -> {
			classLookupMap.put(def.name(), methods);
		});
	}

}
