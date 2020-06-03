package yuku.kbbi.util;

public class Exceptions {
	public interface Producer<T> {
		T invoke() throws Throwable;
	}

	public static <T> T mustNotFail(Producer<T> thing) {
		try {
			return thing.invoke();
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}
}
