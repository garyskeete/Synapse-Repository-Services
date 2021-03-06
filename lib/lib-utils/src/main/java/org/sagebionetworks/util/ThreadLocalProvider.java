package org.sagebionetworks.util;

import java.util.Map;

import com.google.common.collect.Maps;

public class ThreadLocalProvider {
	static Map<String, ThreadLocal<?>> threadLocals = Maps.newHashMap();

	/**
	 * Call this method from static initialization of your class to get a static threadlocal instance
	 * 
	 * <pre>
	 * class Test {
	 * 	private static final ThreadLocal&lt;Long&gt; id = ThreadLocalProvider.getInstance(&quot;my.special.key&quot;);
	 * }
	 * </pre>
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	public static synchronized <T> ThreadLocal<T> getInstance(String key, Class<T> clazz) {
		@SuppressWarnings("unchecked")
		ThreadLocal<T> threadLocal = (ThreadLocal<T>) threadLocals.get(key);
		if (threadLocal == null) {
			threadLocal = new ThreadLocal<T>();
			threadLocals.put(key, threadLocal);
		}
		return threadLocal;
	}
}
