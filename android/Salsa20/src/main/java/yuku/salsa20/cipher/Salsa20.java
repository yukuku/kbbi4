package yuku.salsa20.cipher;


public interface Salsa20 {

	/**
	 * @return position of stream in bytes
	 */
	long getPosition();

	/**
	 * @param pos position of stream in bytes
	 */
	void setPosition(long pos);

	/**
	 * Encrypt or decrypt (they are the same) bytes
	 * @param in input buffer
	 * @param inOffset starting offset of input
	 * @param out output buffer
	 * @param outOffset starting offset of output
	 * @param len how many bytes to process
	 */
	void crypt(byte[] in, int inOffset, byte[] out, int outOffset, int len);

	String getImplementationName();
	
	public static class Factory {
		private static int nativeAvailable = 0; // 0=unknown, 1=yes, 2=no
		
		public Salsa20 newInstanceJava(byte[] key, byte[] nonce, int rounds) {
			return new Salsa20ImplJava(key, nonce, rounds);
		}
		
		public Salsa20 newInstanceNative(byte[] key, byte[] nonce, int rounds) {
			return new Salsa20ImplNative(key, nonce, rounds);
		}
		
		public Salsa20 newInstance(byte[] key, byte[] nonce) {
			return newInstance(key, nonce, 20);
		}

		public Salsa20 newInstance(byte[] key, byte[] nonce, int rounds) {
			if (nativeAvailable == 0) {
				try {
					System.loadLibrary("salsa20");
					nativeAvailable = 1;
				} catch (UnsatisfiedLinkError e) {
					nativeAvailable = 2;
				}
			}
			
			if (nativeAvailable == 1) {
				return newInstanceNative(key, nonce, rounds);
			} else if (nativeAvailable == 2) {
				return newInstanceJava(key, nonce, rounds);
			}
			
			return null;
		}
	}
}