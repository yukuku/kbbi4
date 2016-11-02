package yuku.kbbi.dastruk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ValueReader implements Closeable {
	private InputStream input;
	private byte[] buf = new byte[4096];
	private static Charset utf8 = Charset.forName("utf-8");

	public ValueReader(final InputStream input) {
		this.input = input;
	}

	public int readUint8() throws IOException {
		return input.read();
	}

	public int readVarint() throws IOException {
		final int a = input.read();
		if (a < 0xf0) {
			return a;
		}

		if (a == 0xfe) {
			return input.read();
		}

		if (a == 0xfd) {
			return 0x100 | input.read();
		}

		if (a == 0xfc) {
			return input.read() << 8 | input.read();
		}

		if (a == 0xfb) {
			return 0x10000 | input.read() << 8 | input.read();
		}

		if (a == 0xfa) {
			return input.read() << 16 | input.read() << 8 | input.read();
		}

		throw new RuntimeException("a not understood: 0x" + Integer.toHexString(a));
	}

	@NonNull
	public String readString() throws IOException {
		final int length = readVarint();
		return readRawString(length);
	}

	@NonNull
	public String readRawString(final int length) throws IOException {
		if (buf.length < length) {
			buf = new byte[length << 1];
		}

		int read = 0;
		while (true) {
			read += input.read(buf, read, length - read);
			if (read == length) break;
		}

		return new String(buf, 0, length, utf8);
	}

	public void skip(final int length) throws IOException {
		int skipped = 0;
		while (true) {
			skipped += input.skip(length - skipped);
			if (skipped == length) return;
		}
	}

	static final int ARG_null = 1;
	static final int ARG_int = 2;
	static final int ARG_text = 3;

	static final int[] codeArg = new int[256];

	static { // must keep updated with preprocessor
		codeArg[0] = ARG_text; // normal text
		codeArg[1] = ARG_text;
		codeArg[2] = ARG_text;
		codeArg[3] = ARG_text;
		codeArg[4] = ARG_text;
		codeArg[10] = ARG_null;
		codeArg[11] = ARG_null;
		codeArg[12] = ARG_null;
		codeArg[13] = ARG_null;
		codeArg[14] = ARG_null;
		codeArg[15] = ARG_null;
		codeArg[20] = ARG_text;
		codeArg[21] = ARG_text;
		codeArg[22] = ARG_text;
		codeArg[23] = ARG_text;
		codeArg[24] = ARG_text;
		codeArg[25] = ARG_text;
		codeArg[30] = ARG_null;
		codeArg[31] = ARG_null;
		codeArg[32] = ARG_null;
		codeArg[33] = ARG_null;
		codeArg[40] = ARG_int;
		codeArg[41] = ARG_int;
		codeArg[42] = ARG_text;
		codeArg[50] = ARG_text;
		codeArg[60] = ARG_text;
		codeArg[61] = ARG_text;
		codeArg[62] = ARG_text;
		codeArg[63] = ARG_text;
		codeArg[74] = ARG_text;
		codeArg[0xff] = ARG_null; // EOF marker
	}

	public Cav readClv(@Nullable Cav reuse) throws IOException {
		final Cav res = reuse != null ? reuse : new Cav();

		final int code = res.code = input.read();

		final int arg = codeArg[code];
		if (arg == ARG_null) {
			res.string = null;
			res.number = 0;
		} else if (arg == ARG_int) {
			res.string = null;
			res.number = readVarint();
		} else if (arg == ARG_text) {
			res.string = readString();
			res.number = 0;
		} else {
			throw new RuntimeException("Code not understood: 0x" + Integer.toHexString(code));
		}

		return res;
	}

	@Override
	public void close() throws IOException {
		input.close();
	}
}
