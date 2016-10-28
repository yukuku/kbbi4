package yuku.salsa20.cipher;

import java.io.IOException;
import java.io.OutputStream;

public class Salsa20OutputStream extends OutputStream {
	public static final String TAG = Salsa20OutputStream.class.getSimpleName();

	private final Salsa20 s; 
	private final OutputStream out;
	private static final int BUFFER_SIZE = 64000;
	private final byte[] buf;
	private final byte[] oneByteBuf;
	
	public Salsa20OutputStream(OutputStream out, byte[] key, byte[] nonce) {
		this(out, key, nonce, 20);
	}
	
	public Salsa20OutputStream(OutputStream out, byte[] key, byte[] nonce, int rounds) {
		this.out = out;
		s = new Salsa20.Factory().newInstance(key, nonce, rounds);
		buf = new byte[BUFFER_SIZE];
		oneByteBuf = new byte[1];
	}
	
	@Override public void write(int oneByte) throws IOException {
		oneByteBuf[0] = (byte) oneByte;
		s.crypt(oneByteBuf, 0, this.buf, 0, 1);
		out.write(this.buf, 0, 1);
	}

	@Override public void write(byte[] buffer, int offset, int count) throws IOException {
		if (count < BUFFER_SIZE) {
			s.crypt(buffer, offset, this.buf, 0, count);
			out.write(this.buf, 0, count);
		} else {
			for (int i = offset, max = offset + count; i < max; i += BUFFER_SIZE) {
				int c;
				if (i + BUFFER_SIZE > max) {
					c = max - i;
				} else {
					c = BUFFER_SIZE;
				}
				s.crypt(buffer, i, this.buf, 0, c);
				out.write(this.buf, 0, c);
			}
		}
	};
}
