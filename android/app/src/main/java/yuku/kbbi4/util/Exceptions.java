package yuku.kbbi4.util;

public class Exceptions {
	public interface Producer<T> {
		T invoke() throws Throwable;
	}

	public interface ThrowingRunnable {
		void run() throws Throwable;
	}

	public static <T> T mustNotFail(Producer<T> thing) {
		try {
			return thing.invoke();
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	public static void mustNotFail(ThrowingRunnable r) {
		try {
			r.run();
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}
}
