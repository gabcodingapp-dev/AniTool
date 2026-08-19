#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

// ARM64 instruction helpers
// NOP = 0xD503201F
#define ARM64_NOP 0xD503201F
// RET = 0xD65F03C0
#define ARM64_RET 0xD65F03C0
// MOV X0, #0 = 0xD2800000
#define ARM64_MOV_X0_0 0xD2800000
// B (unconditional) base: 0x14000000
#define ARM64_B_BASE 0x14000000

// Write 32-bit LE
static void write32(unsigned char *p, uint32_t v) {
    p[0] = v & 0xFF;
    p[1] = (v >> 8) & 0xFF;
    p[2] = (v >> 16) & 0xFF;
    p[3] = (v >> 24) & 0xFF;
}

static uint32_t read32(const unsigned char *p) {
    return p[0] | (p[1] << 8) | (p[2] << 16) | (p[3] << 24);
}

// Patch to NOP (ARM64)
int patch_nop(unsigned char *data, size_t len, uint64_t offset) {
    if (offset + 4 > len) return -1;
    write32(data + offset, ARM64_NOP);
    return 0;
}

// Patch to RET (ARM64)
int patch_ret(unsigned char *data, size_t len, uint64_t offset) {
    if (offset + 4 > len) return -1;
    write32(data + offset, ARM64_RET);
    return 0;
}

// Patch to MOV X0, #0 + RET (return 0)
int patch_ret_zero(unsigned char *data, size_t len, uint64_t offset) {
    if (offset + 8 > len) return -1;
    write32(data + offset, ARM64_MOV_X0_0);
    write32(data + offset + 4, ARM64_RET);
    return 0;
}

// Patch unconditional branch (B) at offset to target
int patch_branch(unsigned char *data, size_t len,
                 uint64_t offset, int64_t target_offset) {
    if (offset + 4 > len) return -1;

    int64_t imm = target_offset - offset;
    // B range: ±128MB (26-bit signed * 4)
    if (imm < -134217728 || imm > 134217723) return -2;

    uint32_t imm26 = (uint32_t)((imm >> 2) & 0x03FFFFFF);
    uint32_t insn = ARM64_B_BASE | imm26;
    write32(data + offset, insn);
    return 0;
}

// Patch conditional branch to unconditional
int patch_cond_to_uncond(unsigned char *data, size_t len, uint64_t offset) {
    if (offset + 4 > len) return -1;

    uint32_t insn = read32(data + offset);
    uint32_t top = (insn >> 24) & 0xFF;

    // B.cond (0x54): change to B (0x14)
    if ((top & 0xFE) == 0x54) {
        int64_t imm = (int64_t)(int32_t)((insn & 0x00FFFFE0) << 11) >> 9;
        return patch_branch(data, len, offset, offset + imm);
    }

    // CBZ/CBNZ (0x34/0x35): change to B
    if ((top & 0xFE) == 0x34) {
        int64_t imm = (int64_t)(int32_t)((insn & 0x00FFFFE0) << 8) >> 6;
        return patch_branch(data, len, offset, offset + imm);
    }

    // TBZ/TBNZ (0x36/0x37): change to B
    if ((top & 0xFE) == 0x36) {
        int64_t imm = (int64_t)(int32_t)((insn & 0x00FFFFE0) << 8) >> 6;
        return patch_branch(data, len, offset, offset + imm);
    }

    return -2; // Not a recognized conditional branch
}

// Patch bytes at offset
int patch_bytes(unsigned char *data, size_t len,
                uint64_t offset, const unsigned char *patch, size_t patch_len) {
    if (offset + patch_len > len) return -1;
    memcpy(data + offset, patch, patch_len);
    return 0;
}

// Search for byte pattern
int64_t search_pattern(const unsigned char *data, size_t len,
                       const unsigned char *pattern, size_t pat_len,
                       size_t start) {
    if (start >= len || pat_len == 0) return -1;
    for (size_t i = start; i <= len - pat_len; i++) {
        if (memcmp(data + i, pattern, pat_len) == 0) {
            return (int64_t)i;
        }
    }
    return -1;
}

// Search for string
int64_t search_string(const unsigned char *data, size_t len,
                      const char *str, size_t start) {
    size_t slen = strlen(str);
    return search_pattern(data, len, (const unsigned char *)str, slen, start);
}
