#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdint.h>

void obf_xor(const unsigned char *input, size_t len,
             unsigned char *output, uint8_t key) {
    for (size_t i = 0; i < len; i++) {
        output[i] = input[i] ^ key;
    }
}

void obf_xor_multi(const unsigned char *input, size_t len,
                   unsigned char *output, const uint8_t *key, size_t key_len) {
    for (size_t i = 0; i < len; i++) {
        output[i] = input[i] ^ key[i % key_len];
    }
}

// Auto-detect XOR key by scoring printable character ratio
int obf_xor_detect_key(const unsigned char *data, size_t len, int *best_key) {
    int best_score = 0;
    *best_key = 0;

    for (int k = 0; k < 256; k++) {
        int score = 0;
        for (size_t i = 0; i < len; i++) {
            unsigned char c = data[i] ^ k;
            if ((c >= 0x20 && c <= 0x7E) || c == '\n' || c == '\r' || c == '\t') {
                score++;
            }
        }
        if (score > best_score) {
            best_score = score;
            *best_key = k;
        }
    }

    // Require at least 70% printable
    return (best_score > len * 0.7) ? best_score : 0;
}

// Calculate Shannon entropy (0.0 = uniform, 8.0 = max entropy)
double obf_entropy(const unsigned char *data, size_t len) {
    if (len == 0) return 0.0;

    uint64_t freq[256];
    memset(freq, 0, sizeof(freq));
    for (size_t i = 0; i < len; i++) {
        freq[data[i]]++;
    }

    double entropy = 0.0;
    for (int i = 0; i < 256; i++) {
        if (freq[i] > 0) {
            double p = (double)freq[i] / len;
            entropy -= p * log2(p);
        }
    }
    return entropy;
}

// Byte frequency analysis
void obf_byte_freq(const unsigned char *data, size_t len,
                   uint64_t freq[256]) {
    memset(freq, 0, sizeof(uint64_t) * 256);
    for (size_t i = 0; i < len; i++) {
        freq[data[i]]++;
    }
}

// Detect common packers/obfuscators by section names/entropy
int obf_detect_packer(const unsigned char *data, size_t len) {
    if (len < 4) return 0;

    // Check for UPX
    if (memmem(data, len, "UPX!", 4) || memmem(data, len, "UPX0", 4) ||
        memmem(data, len, "UPX1", 4)) return 1;

    // Check for ASPack
    if (memmem(data, len, ".aspack", 7)) return 2;

    // Check for Themida
    if (memmem(data, len, ".themida", 8)) return 3;

    // High entropy in .text section might indicate encryption
    double ent = obf_entropy(data, len > 0x10000 ? 0x10000 : len);
    if (ent > 7.5) return 4; // Likely encrypted/packed

    return 0;
}

const char *obf_packer_name(int packer_id) {
    switch (packer_id) {
        case 1: return "UPX";
        case 2: return "ASPack";
        case 3: return "Themida";
        case 4: return "High entropy (likely encrypted)";
        default: return "Unknown";
    }
}
