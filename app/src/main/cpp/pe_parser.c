#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

typedef struct {
    int is_64;
    uint64_t image_base;
    uint32_t entry_rva;
    uint32_t section_alignment;
    uint32_t file_alignment;
    uint16_t num_sections;
    uint32_t timestamp;
    char name[256];
} PEInfo;

typedef struct {
    char name[9];
    uint32_t virtual_size;
    uint32_t virtual_addr;
    uint32_t raw_size;
    uint32_t raw_offset;
    uint32_t characteristics;
} PESection;

static uint16_t pe_read16(const unsigned char *p) {
    return p[0] | (p[1] << 8);
}

static uint32_t pe_read32(const unsigned char *p) {
    return p[0] | (p[1] << 8) | (p[2] << 16) | (p[3] << 24);
}

static uint64_t pe_read64(const unsigned char *p) {
    uint32_t lo = pe_read32(p);
    uint32_t hi = pe_read32(p + 4);
    return ((uint64_t)hi << 32) | lo;
}

int pe_validate(const unsigned char *data, size_t len) {
    if (len < 2) return 0;
    if (data[0] != 'M' || data[1] != 'Z') return 0;
    if (len < 64) return 0;
    uint32_t pe_offset = pe_read32(data + 60);
    if (pe_offset + 4 > len) return 0;
    return data[pe_offset] == 'P' && data[pe_offset + 1] == 'E';
}

int pe_parse_info(const unsigned char *data, size_t len, PEInfo *info) {
    if (!pe_validate(data, len)) return -1;
    memset(info, 0, sizeof(PEInfo));

    uint32_t pe_offset = pe_read32(data + 60);
    const unsigned char *coff = data + pe_offset + 4;

    uint16_t machine = pe_read16(coff);
    info->is_64 = (machine == 0x8664);
    info->num_sections = pe_read16(coff + 2);
    info->timestamp = pe_read32(coff + 4);

    const unsigned char *opt = coff + 20;
    if (info->is_64) {
        if (pe_offset + 24 + 112 > len) return -1;
        info->entry_rva = pe_read32(opt + 16);
        info->image_base = pe_read64(opt + 24);
        info->section_alignment = pe_read32(opt + 32);
        info->file_alignment = pe_read32(opt + 36);
    } else {
        if (pe_offset + 24 + 96 > len) return -1;
        info->entry_rva = pe_read32(opt + 16);
        info->image_base = pe_read32(opt + 28);
        info->section_alignment = pe_read32(opt + 32);
        info->file_alignment = pe_read32(opt + 36);
    }

    return 0;
}

int pe_parse_sections(const unsigned char *data, size_t len,
                      PESection *out, int max_sections) {
    PEInfo info;
    if (pe_parse_info(data, len, &info) < 0) return -1;

    uint32_t pe_offset = pe_read32(data + 60);
    int opt_size = pe_read16(data + pe_offset + 20);
    const unsigned char *sec_start = data + pe_offset + 24 + opt_size;

    int count = 0;
    for (int i = 0; i < info.num_sections && count < max_sections; i++) {
        const unsigned char *s = sec_start + i * 40;
        if ((size_t)(sec_start - data + (i + 1) * 40) > len) break;

        PESection *sec = &out[count];
        memset(sec, 0, sizeof(PESection));
        memcpy(sec->name, s, 8);
        sec->name[8] = '\0';
        sec->virtual_size = pe_read32(s + 8);
        sec->virtual_addr = pe_read32(s + 12);
        sec->raw_size = pe_read32(s + 16);
        sec->raw_offset = pe_read32(s + 20);
        sec->characteristics = pe_read32(s + 36);
        count++;
    }
    return count;
}
