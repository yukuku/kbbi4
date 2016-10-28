package yuku.salsa20.cipher;

import java.io.IOException;
import java.io.InputStream;

public class Salsa20InputStream extends InputStream {
	public static final String TAG = Salsa20InputStream.class.getSimpleName();

	private final Salsa20 s; 
	private final InputStream in;
	private static final int BUFFER_SIZE = 64000;
	private final byte[] buf;
	private final byte[] oneByteBuf;
	
	public Salsa20InputStream(InputStream in, byte[] key, byte[] nonce) {
		this(in, key, nonce, 20);
	}
	
	public Salsa20InputStream(InputStream in, byte[] key, byte[] nonce, int rounds) {
		this.in = in;
		s = new Salsa20.Factory().newInstance(key, nonce, rounds);
		buf = new byte[BUFFER_SIZE];
		oneByteBuf = new byte[1];
	}
	
	@Override public int read() throws IOException {
		int m = in.read();
		if (m < 0) {
			return m; // EOF
		}
		
		this.buf[0] = (byte) m;
		s.crypt(this.buf, 0, this.oneByteBuf, 0, 1);
		return (0xff & this.oneByteBuf[0]);
	};
	
	@Override public int read(byte[] buffer, int offset, int length) throws IOException {
		if (length > BUFFER_SIZE) {
			length = BUFFER_SIZE;
		}
		
		int read = in.read(this.buf, 0, length);
		if (read <= 0) {
			return read; // EOF or 0 read
		}
		
		s.crypt(this.buf, 0, buffer, offset, read);
		return read;
	}
}
