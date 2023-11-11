/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * an implementation of the {@link IQueryable} interface that has a backing list
 * of delegates that are queried to have the final result.
 *
 * @param <T>
 */
public final class ListQueryable<T> implements IQueryable<T> {

	private final List<IQueryable<T>> queryList = new ArrayList<>();

	@Override
	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, queryList.size());
		List<IQueryResult<T>> results = new ArrayList<>();
		for (IQueryable<T> queryable : queryList) {
			results.add(queryable.query(query, subMonitor.split(1)));
		}
		return new ListQueryResult<>(results);
	}

	public void add(IQueryable<T> child) {
		queryList.add(child);
	}

	private static final class ListQueryResult<R> implements IQueryResult<R> {

		private List<IQueryResult<R>> resultList;

		public ListQueryResult(List<IQueryResult<R>> resultList) {
			this.resultList = resultList;
		}

		@Override
		public IQueryResult<R> query(IQuery<R> query, IProgressMonitor monitor) {
			return query.perform(iterator());
		}

		@Override
		public boolean isEmpty() {
			return resultList.isEmpty() || resultList.stream().allMatch(IQueryResult::isEmpty);
		}

		@Override
		public Iterator<R> iterator() {
			return stream().iterator();
		}

		@Override
		public Stream<R> stream() {
			return resultList.stream().flatMap(r -> StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(r.iterator(), Spliterator.ORDERED), false));
		}

		@SuppressWarnings("unchecked")
		@Override
		public R[] toArray(Class<R> clazz) {
			return stream().toArray(s -> (R[]) Array.newInstance(clazz, s));
		}

		@Override
		public Set<R> toSet() {
			return stream().collect(Collectors.toCollection(LinkedHashSet::new));
		}

		@Override
		public Set<R> toUnmodifiableSet() {
			return stream().collect(Collectors.toUnmodifiableSet());
		}

	}

}