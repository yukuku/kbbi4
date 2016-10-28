package yuku.salsa20.cipher;


class Salsa20ImplNative implements Salsa20 {
	private long nativeObj = 0;
	
	static {
		System.loadLibrary("salsa20");
	}

	/**
	 * @param key 16 byte (128 bit)
	 * @param nonce 8 byte (64 bit)
	 * @param rounds Must be even. 20 is the full Salsa20 with 20 rounds (or 10 double-rounds) 
	 */
	public Salsa20ImplNative(byte[] key, byte[] nonce, int rounds) {
		if (key == null || key.length != 16) {
			throw new IllegalArgumentException("key is not 16 bytes");
		}
		if (nonce == null || nonce.length != 8) {
			throw new IllegalArgumentException("nonce is not 8 bytes");
		}
		
		int doubleRounds = rounds >> 1;
		if (doubleRounds + doubleRounds != rounds) {
			throw new IllegalArgumentException("rounds must be even");
		}
		
		nativeObj = nativeSetup(key, nonce, rounds);
	}

	@Override public long getPosition() {
		return nativeGetPosition(nativeObj);
	}

	@Override public void setPosition(long pos) {
		nativeSetPosition(nativeObj, pos);
	}

	@Override public void crypt(byte[] in, int inOffset, byte[] out, int outOffset, int len) {
		nativeCrypt(nativeObj, in, inOffset, out, outOffset, len);
	}
	
	@Override public String getImplementationName() {
		return "native";
	}
	
	private static native long nativeSetup(byte[] key, byte[] nonce, int rounds);
	private static native void nativeSetPosition(long obj, long pos);
	private static native long nativeGetPosition(long obj);
	private static native void nativeCrypt(long obj, byte[] in, int inOffset, byte[] out, int outOffset, int len);
}
