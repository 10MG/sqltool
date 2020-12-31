package cn.tenmg.sqltool.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集合工具类
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class CollectionUtils {

	@SafeVarargs
	public static <E> Set<E> asSet(E... elements) {
		if (elements == null || elements.length == 0) {
			return Collections.emptySet();
		}

		if (elements.length == 1) {
			return Collections.singleton(elements[0]);
		}

		LinkedHashSet<E> set = new LinkedHashSet<E>(elements.length * 4 / 3 + 1);
		Collections.addAll(set, elements);
		return set;
	}

	/**
	 * Returns {@code true} if the specified {@code Collection} is {@code null} or
	 * {@link Collection#isEmpty empty}, {@code false} otherwise.
	 *
	 * @param c
	 *            the collection to check
	 * @return {@code true} if the specified {@code Collection} is {@code null} or
	 *         {@link Collection#isEmpty empty}, {@code false} otherwise.
	 * @since 1.0
	 */
	public static boolean isEmpty(Collection<?> c) {
		return c == null || c.isEmpty();
	}

	/**
	 * Returns {@code true} if the specified {@code Map} is {@code null} or
	 * {@link Map#isEmpty empty}, {@code false} otherwise.
	 *
	 * @param m
	 *            the {@code Map} to check
	 * @return {@code true} if the specified {@code Map} is {@code null} or
	 *         {@link Map#isEmpty empty}, {@code false} otherwise.
	 */
	public static boolean isEmpty(Map<?, ?> m) {
		return m == null || m.isEmpty();
	}

	/**
	 * Returns the size of the specified collection or {@code 0} if the collection
	 * is {@code null}.
	 *
	 * @param c
	 *            the collection to check
	 * @return the size of the specified collection or {@code 0} if the collection
	 *         is {@code null}.
	 */
	public static int size(Collection<?> c) {
		return c != null ? c.size() : 0;
	}

	/**
	 * Returns the size of the specified map or {@code 0} if the map is
	 * {@code null}.
	 *
	 * @param m
	 *            the map to check
	 * @return the size of the specified map or {@code 0} if the map is
	 *         {@code null}.
	 */
	public static int size(Map<?, ?> m) {
		return m != null ? m.size() : 0;
	}

	@SafeVarargs
	public static <E> List<E> asList(E... elements) {
		if (elements == null || elements.length == 0) {
			return Collections.emptyList();
		}
		// Integer overflow does not occur when a large array is passed in
		// because the list array already exists
		return Arrays.asList(elements);
	}

	static int computeListCapacity(int arraySize) {
		return (int) Math.min(5L + arraySize + (arraySize / 10), Integer.MAX_VALUE);
	}
}
