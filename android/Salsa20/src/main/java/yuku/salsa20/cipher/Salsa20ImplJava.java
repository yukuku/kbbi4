package yuku.salsa20.cipher;

class Salsa20ImplJava implements Salsa20 {
	private final static int[] tau = {
		0x61707865, 0x3120646e, 0x79622d36, 0x6b206574,
	};
	
	// encryption states
	final int[] input = new int[16];
	final byte[] output = new byte[64];
	final int[] tmp1 = new int[16]; // not used outside

	// parameter
	final int doubleRounds; // salsa20/20 is 10 double-rounds
	
	// positions
	long posBlock = 0; // 64-byte block number
	int posRemainder = 0; // 0..63
	
	// /////////////////////////////////////////////////////////////////////////
	
	/**
	 * Salsa20 with 20 rounds.
	 * @param key 16 byte (128 bit)
	 * @param nonce 8 byte (64 bit)
	 */
	public Salsa20ImplJava(byte[] key, byte[] nonce) {
		this(key, nonce, 20);
	}
	
	/**
	 * @param key 16 byte (128 bit)
	 * @param nonce 8 byte (64 bit)
	 * @param rounds Must be even. 20 is the full Salsa20 with 20 rounds (or 10 double-rounds) 
	 */
	public Salsa20ImplJava(byte[] key, byte[] nonce, int rounds) {
		if (key == null || key.length != 16) {
			throw new IllegalArgumentException("key is not 16 bytes");
		}
		if (nonce == null || nonce.length != 8) {
			throw new IllegalArgumentException("nonce is not 8 bytes");
		}
		
		doubleRounds = rounds >> 1;
		if (doubleRounds + doubleRounds != rounds) {
			throw new IllegalArgumentException("rounds must be even");
		}
		
		setup(key, nonce);
	}
	
	// /////////////////////////////////////////////////////////////////////////

	private void calcEncryptionOutputFromInput() {
		int[] x = this.tmp1;
		int[] _input = this.input;
		byte[] _output = this.output;
		
		System.arraycopy(_input, 0, x, 0, x.length);

		int s;
		for (int i = doubleRounds; i > 0; i--) {
			s = x[ 0] + x[12]; x[ 4] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[ 4] + x[ 0]; x[ 8] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[ 8] + x[ 4]; x[12] ^= (s << 13) | (s >>> (32 - 13));
			s = x[12] + x[ 8]; x[ 0] ^= (s << 18) | (s >>> (32 - 18));
			s = x[ 5] + x[ 1]; x[ 9] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[ 9] + x[ 5]; x[13] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[13] + x[ 9]; x[ 1] ^= (s << 13) | (s >>> (32 - 13));
			s = x[ 1] + x[13]; x[ 5] ^= (s << 18) | (s >>> (32 - 18));
			s = x[10] + x[ 6]; x[14] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[14] + x[10]; x[ 2] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[ 2] + x[14]; x[ 6] ^= (s << 13) | (s >>> (32 - 13));
			s = x[ 6] + x[ 2]; x[10] ^= (s << 18) | (s >>> (32 - 18));
			s = x[15] + x[11]; x[ 3] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[ 3] + x[15]; x[ 7] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[ 7] + x[ 3]; x[11] ^= (s << 13) | (s >>> (32 - 13));
			s = x[11] + x[ 7]; x[15] ^= (s << 18) | (s >>> (32 - 18));
			s = x[ 0] + x[ 3]; x[ 1] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[ 1] + x[ 0]; x[ 2] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[ 2] + x[ 1]; x[ 3] ^= (s << 13) | (s >>> (32 - 13));
			s = x[ 3] + x[ 2]; x[ 0] ^= (s << 18) | (s >>> (32 - 18));
			s = x[ 5] + x[ 4]; x[ 6] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[ 6] + x[ 5]; x[ 7] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[ 7] + x[ 6]; x[ 4] ^= (s << 13) | (s >>> (32 - 13));
			s = x[ 4] + x[ 7]; x[ 5] ^= (s << 18) | (s >>> (32 - 18));
			s = x[10] + x[ 9]; x[11] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[11] + x[10]; x[ 8] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[ 8] + x[11]; x[ 9] ^= (s << 13) | (s >>> (32 - 13));
			s = x[ 9] + x[ 8]; x[10] ^= (s << 18) | (s >>> (32 - 18));
			s = x[15] + x[14]; x[12] ^= (s <<  7) | (s >>> (32 -  7));
			s = x[12] + x[15]; x[13] ^= (s <<  9) | (s >>> (32 -  9));
			s = x[13] + x[12]; x[14] ^= (s << 13) | (s >>> (32 - 13));
			s = x[14] + x[13]; x[15] ^= (s << 18) | (s >>> (32 - 18));
		}

		for (int i = 0; i < 16; i++){
			int value = x[i] + _input[i];
			int nOfs = i << 2;
			_output[nOfs] = (byte) (value);
			_output[nOfs + 1] = (byte) (value >>> 8);
			_output[nOfs + 2] = (byte) (value >>> 16);
			_output[nOfs + 3] = (byte) (value >>> 24);
		}
	}

	/* this.input is int32[16] (from key int32[4], offset int32[2], nonce int32[2]):
	 * [0] = constants[0]
	 * [1] = key[0]
	 * [2] = key[1]
	 * [3] = key[2]
	 * [4] = key[3]
	 * [5] = constants[1]
	 * [6] = nonce[0]
	 * [7] = nonce[1]
	 * [8] = offset[0]
	 * [9] = offset[1]
	 * [10] = constants[2]
	 * [11] = key[0]
	 * [12] = key[1]
	 * [13] = key[2]
	 * [14] = key[3]
	 * [15] = constants[3]
	 */
	void setup(byte[] key, byte[] nonce) {
		int[] constants;

		// setup from key
		this.input[1] = readInt32LE(key, 0);
		this.input[2] = readInt32LE(key, 4);
		this.input[3] = readInt32LE(key, 8);
		this.input[4] = readInt32LE(key, 12);

		if (key.length == 16) { // only 128 bit key supported here
			constants = tau; 
		} else {
			throw new RuntimeException("key not 128 bit");
		}

		// setup from key
		this.input[11] = readInt32LE(key, 0);
		this.input[12] = readInt32LE(key, 4);
		this.input[13] = readInt32LE(key, 8);
		this.input[14] = readInt32LE(key, 12);

		// setup from constants
		this.input[0] = constants[0];
		this.input[5] = constants[1];
		this.input[10] = constants[2];
		this.input[15] = constants[3];

		// setup from nonce
		this.input[6] = readInt32LE(nonce, 0);
		this.input[7] = readInt32LE(nonce, 4);
		this.input[8] = 0; // data offset is 0
		this.input[9] = 0; // data offset is 0
		
		calcEncryptionOutputFromInput();
	}
	
	private static int readInt32LE(byte[] data, int nOfs) {
		return (data[nOfs + 3] << 24) |
		((data[nOfs + 2] & 0xff) << 16) |
		((data[nOfs + 1] & 0xff) << 8) |
		(data[nOfs] & 0xff);
	}

	/* (non-Javadoc)
	 * @see yuku.salsa20.cipher.Salsa20#getPosition()
	 */
	@Override public long getPosition() {
		return (this.posBlock << 6) | this.posRemainder;
	}
	
	/* (non-Javadoc)
	 * @see yuku.salsa20.cipher.Salsa20#setPosition(long)
	 */
	@Override public void setPosition(long pos) {
		setBlockPosition(pos >> 6);
		this.posRemainder = (int) (pos & 0x3f);
	}

	private void setBlockPosition(long block) {
		// only recalculate when it changes
		if (this.posBlock != block) {
			this.posBlock = block;
			this.input[8] = (int) (block & 0x00000000ffffffffL);
			this.input[9] = (int) (block >>> 32);
			calcEncryptionOutputFromInput();
		}
	}
	
	private void increaseBlockPosition() {
		this.posBlock++;
		long block = this.posBlock;
		this.input[8] = (int) (block & 0x00000000ffffffffL);
		this.input[9] = (int) (block >>> 32);
		calcEncryptionOutputFromInput();
	}

	/* (non-Javadoc)
	 * @see yuku.salsa20.cipher.Salsa20#crypt(byte[], int, byte[], int, int)
	 */
	@Override public void crypt(byte[] in, int inOffset, byte[] out, int outOffset, int len) {
		int _posRemainder = this.posRemainder;
		byte[] _output = this.output;
		
		for (int i = 0; i < len; i++) {
			out[outOffset++] = (byte) (in[inOffset++] ^ _output[_posRemainder]);
			
			// increase our position
			_posRemainder++;
			if (_posRemainder == 64) {
				_posRemainder = 0;
				increaseBlockPosition();
			}
		}
		
		this.posRemainder = _posRemainder;
	}

	@Override public String getImplementationName() {
		return "java";
	}
}
