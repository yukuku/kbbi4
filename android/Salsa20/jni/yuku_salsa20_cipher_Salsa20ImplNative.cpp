/*
 * Class:     yuku.salsa20.cipher.Salsa20ImplNative
 */

#include <jni.h>
#include <string.h>
#include "yuku_salsa20_cipher_Salsa20ImplNative.h"
#include <android/log.h>

u32 Salsa20ImplNative::tau[] = {
	0x61707865, 0x3120646e, 0x79622d36, 0x6b206574,
};

Salsa20ImplNative::Salsa20ImplNative(u8 *key, u8 *nonce, int rounds) {
	this->posBlock = 0;
	this->posRemainder = 0;
	this->doubleRounds = rounds >> 1;

	__android_log_print(ANDROID_LOG_DEBUG, "sn-init", "Native build %s %s", __DATE__, __TIME__);

	u32* constants = tau;

	// setup from key
	input[1] = U8TO32_LITTLE(key + 0);
	input[2] = U8TO32_LITTLE(key + 4);
	input[3] = U8TO32_LITTLE(key + 8);
	input[4] = U8TO32_LITTLE(key + 12);

	// only 128 bit key supported here
	constants = tau;

	// setup from key
	input[11] = U8TO32_LITTLE(key + 0);
	input[12] = U8TO32_LITTLE(key + 4);
	input[13] = U8TO32_LITTLE(key + 8);
	input[14] = U8TO32_LITTLE(key + 12);

	// setup from constants
	input[0] = constants[0];
	input[5] = constants[1];
	input[10] = constants[2];
	input[15] = constants[3];

	// setup from nonce
	input[6] = U8TO32_LITTLE(nonce + 0);
	input[7] = U8TO32_LITTLE(nonce + 4);
	input[8] = 0; // data offset is 0
	input[9] = 0; // data offset is 0

#if SALSA20_DEBUG

	for (int i = 0; i < 4; i++) {
		__android_log_print(ANDROID_LOG_DEBUG, "sn-key", "%2d: %08x", i, ((u32*)key)[i]);
	}

	for (int i = 0; i < 2; i++) {
		__android_log_print(ANDROID_LOG_DEBUG, "sn-nonce", "%2d: %08x", i, ((u32*)nonce)[i]);
	}

	for (int i = 0; i < 16; i++) {
		__android_log_print(ANDROID_LOG_DEBUG, "sn-input", "%2d: %08x", i, input[i]);
	}

#endif

	calcEncryptionOutputFromInput();
}

	// /////////////////////////////////////////////////////////////////////////

void Salsa20ImplNative::calcEncryptionOutputFromInput() {
	u32 *x = this->tmp1;
	u32 *_input = this->input;
	u8 *_output = this->output;

	memcpy(x, _input, 64);

#if SALSA20_DEBUG

	for (int i = 0; i < 16; i++) {
		__android_log_print(ANDROID_LOG_DEBUG, "sn-x", "%2d: %08x", i, x[i]);
	}

#endif

	int s;
	for (int i = doubleRounds; i > 0; i--) {
		x[ 4] ^= rol32((x[ 0] + x[12]),  7);
		x[ 8] ^= rol32((x[ 4] + x[ 0]),  9);
		x[12] ^= rol32((x[ 8] + x[ 4]), 13);
		x[ 0] ^= rol32((x[12] + x[ 8]), 18);
		x[ 9] ^= rol32((x[ 5] + x[ 1]),  7);
		x[13] ^= rol32((x[ 9] + x[ 5]),  9);
		x[ 1] ^= rol32((x[13] + x[ 9]), 13);
		x[ 5] ^= rol32((x[ 1] + x[13]), 18);
		x[14] ^= rol32((x[10] + x[ 6]),  7);
		x[ 2] ^= rol32((x[14] + x[10]),  9);
		x[ 6] ^= rol32((x[ 2] + x[14]), 13);
		x[10] ^= rol32((x[ 6] + x[ 2]), 18);
		x[ 3] ^= rol32((x[15] + x[11]),  7);
		x[ 7] ^= rol32((x[ 3] + x[15]),  9);
		x[11] ^= rol32((x[ 7] + x[ 3]), 13);
		x[15] ^= rol32((x[11] + x[ 7]), 18);
		x[ 1] ^= rol32((x[ 0] + x[ 3]),  7);
		x[ 2] ^= rol32((x[ 1] + x[ 0]),  9);
		x[ 3] ^= rol32((x[ 2] + x[ 1]), 13);
		x[ 0] ^= rol32((x[ 3] + x[ 2]), 18);
		x[ 6] ^= rol32((x[ 5] + x[ 4]),  7);
		x[ 7] ^= rol32((x[ 6] + x[ 5]),  9);
		x[ 4] ^= rol32((x[ 7] + x[ 6]), 13);
		x[ 5] ^= rol32((x[ 4] + x[ 7]), 18);
		x[11] ^= rol32((x[10] + x[ 9]),  7);
		x[ 8] ^= rol32((x[11] + x[10]),  9);
		x[ 9] ^= rol32((x[ 8] + x[11]), 13);
		x[10] ^= rol32((x[ 9] + x[ 8]), 18);
		x[12] ^= rol32((x[15] + x[14]),  7);
		x[13] ^= rol32((x[12] + x[15]),  9);
		x[14] ^= rol32((x[13] + x[12]), 13);
		x[15] ^= rol32((x[14] + x[13]), 18);
	}

	for (int i = 0; i < 16; i++){
		u32 value = x[i] + _input[i];
		u32 nOfs = i << 2;
		U32TO8_LITTLE(_output + nOfs, value);
	}
}

u64 Salsa20ImplNative::getPosition() {
	return (this->posBlock << 6) | this->posRemainder;
}

void Salsa20ImplNative::setPosition(u64 pos) {
	setBlockPosition(pos >> 6);
	this->posRemainder = (int) (pos & 0x3f);
}

void Salsa20ImplNative::setBlockPosition(u64 block) {
	// only recalculate when it changes
	if (this->posBlock != block) {
		this->posBlock = block;
		this->input[8] = (u32) (block & 0x00000000ffffffffLL);
		this->input[9] = (u32) (block >> 32);
		calcEncryptionOutputFromInput();
	}
}

void Salsa20ImplNative::increaseBlockPosition() {
	this->posBlock++;
	u64 block = this->posBlock;
	this->input[8] = (u32) (block & 0x00000000ffffffffLL);
	this->input[9] = (u32) (block >> 32);
	calcEncryptionOutputFromInput();
}

void Salsa20ImplNative::crypt(u8 *in, int inOffset, u8 *out, int outOffset, int len) {
	int _posRemainder = this->posRemainder;
	u8 *_output = this->output;

	for (int i = 0; i < len; i++) {
		out[outOffset++] = (u8) (in[inOffset++] ^ _output[_posRemainder]);

		// increase our position
		_posRemainder++;
		if (_posRemainder == 64) {
			_posRemainder = 0;
			increaseBlockPosition();
		}
	}

	this->posRemainder = _posRemainder;
}


//////////////////////////////////// JNI INTERFACES /////////////////////////////////////////

extern "C" {
	JNIEXPORT jlong Java_yuku_salsa20_cipher_Salsa20ImplNative_nativeSetup(JNIEnv *env, jobject thiz, jbyteArray _key, jbyteArray _nonce, jint rounds) {
		jbyte *key = env->GetByteArrayElements(_key, NULL);
		jbyte *nonce = env->GetByteArrayElements(_nonce, NULL);

		Salsa20ImplNative *s = new Salsa20ImplNative((u8*)key, (u8*)nonce, rounds);

		env->ReleaseByteArrayElements(_nonce, nonce, JNI_ABORT);
		env->ReleaseByteArrayElements(_key, key, JNI_ABORT);

		return (jlong) s;
	}

	JNIEXPORT void Java_yuku_salsa20_cipher_Salsa20ImplNative_nativeSetPosition(JNIEnv *env, jobject thiz, jlong obj, jlong pos) {
		Salsa20ImplNative *s = (Salsa20ImplNative*) obj;
		s->setPosition((u64) pos);
	}

	JNIEXPORT jlong Java_yuku_salsa20_cipher_Salsa20ImplNative_nativeGetPosition(JNIEnv *env, jobject thiz, jlong obj) {
		Salsa20ImplNative *s = (Salsa20ImplNative*) obj;
		return (jlong) s->getPosition();
	}

	JNIEXPORT void Java_yuku_salsa20_cipher_Salsa20ImplNative_nativeCrypt(JNIEnv *env, jobject thiz, jlong obj, jbyteArray _in, jint inOffset, jbyteArray _out, jint outOffset, jint len) {
		jbyte *in = env->GetByteArrayElements(_in, NULL);
		jbyte *out = env->GetByteArrayElements(_out, NULL);

		Salsa20ImplNative *s = (Salsa20ImplNative*) obj;
		s->crypt((u8*) in, (int) inOffset, (u8*) out, (int) outOffset, (int) len);

		env->ReleaseByteArrayElements(_out, out, 0);
		env->ReleaseByteArrayElements(_in, in, JNI_ABORT);
	}
}
