#include <linux/types.h>

typedef __u8 u8;
typedef __u32 u32;
typedef __u64 u64;


/**
 * rol32 - rotate a 32-bit value left
 * @word: value to rotate
 * @shift: bits to roll
 */
static inline u32 rol32(u32 word, unsigned int shift) {
	return (word << shift) | (word >> (32 - shift));
}

#define U32TO8_LITTLE(p, v) \
        { (p)[0] = (v >>  0) & 0xff; (p)[1] = (v >>  8) & 0xff; \
          (p)[2] = (v >> 16) & 0xff; (p)[3] = (v >> 24) & 0xff; }

#define U8TO32_LITTLE(p)   \
        (((u32)((p)[0])      ) | ((u32)((p)[1]) <<  8) | \
         ((u32)((p)[2]) << 16) | ((u32)((p)[3]) << 24)   )

//////////////////////////////////

class Salsa20ImplNative {
	static u32 sigma[4];
	static u32 tau[4];

	// encryption states
	u32 input[16];
	u8 output[64];
	u32 tmp1[16];

	// parameter
	u32 doubleRounds; // salsa20/20 is 10 double-rounds

	// positions
	u64 posBlock; // 64-byte block number
	int posRemainder; // 0..63

public:
	Salsa20ImplNative(u8 *key, int kbits, u8 *nonce, int rounds);
	void setPosition(u64 pos);
	u64 getPosition();
	void crypt(u8 *in, int inOffset, u8 *out, int outOffset, int len);

private:
	void calcEncryptionOutputFromInput();
	void setBlockPosition(u64 block);
	void increaseBlockPosition();
};
