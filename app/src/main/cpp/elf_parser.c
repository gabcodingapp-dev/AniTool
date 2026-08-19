#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <elf.h>

typedef struct {
    int is_64;
    int is_le;
    uint64_t entry;
    uint64_t phoff;
    uint64_t shoff;
    uint32_t phnum;
    uint32_t shnum;
    uint32_t shstrndx;
    uint64_t file_size;
} ElfInfo;

typedef struct {
    char name[64];
    uint64_t offset;
    uint64_t size;
    uint32_t type;
    uint64_t addr;
    uint32_t flags;
} ElfSectionInfo;

typedef struct {
    char name[64];
    uint64_t value;
    uint64_t size;
    uint32_t info;
    uint32_t shndx;
} ElfSymbolInfo;

typedef struct {
    uint64_t offset;
    uint64_t info;
    uint64_t addend;
    char sym_name[64];
} ElfRelocInfo;

static uint16_t read16(const unsigned char *p, int le) {
    return le ? (p[0] | (p[1] << 8)) : ((p[0] << 8) | p[1]);
}

static uint32_t read32(const unsigned char *p, int le) {
    if (le) return p[0] | (p[1] << 8) | (p[2] << 16) | (p[3] << 24);
    return (p[0] << 24) | (p[1] << 16) | (p[2] << 8) | p[3];
}

static uint64_t read64(const unsigned char *p, int le) {
    if (le) {
        uint32_t lo = read32(p, le);
        uint32_t hi = read32(p + 4, le);
        return ((uint64_t)hi << 32) | lo;
    }
    uint32_t hi = read32(p, le);
    uint32_t lo = read32(p + 4, le);
    return ((uint64_t)hi << 32) | lo;
}

int elf_validate(const unsigned char *data, size_t len) {
    if (len < 4) return 0;
    return data[0] == 0x7F && data[1] == 'E' && data[2] == 'L' && data[3] == 'F';
}

int elf_is_64(const unsigned char *data) {
    return data[4] == 2;
}

int elf_is_le(const unsigned char *data) {
    return data[5] == 1;
}

int elf_parse_info(const unsigned char *data, size_t len, ElfInfo *info) {
    if (!elf_validate(data, len)) return -1;
    memset(info, 0, sizeof(ElfInfo));
    info->is_64 = data[4] == 2;
    info->is_le = data[5] == 1;
    info->file_size = len;

    if (info->is_64) {
        if (len < 64) return -1;
        info->entry = read64(data + 24, info->is_le);
        info->phoff = read64(data + 32, info->is_le);
        info->shoff = read64(data + 40, info->is_le);
        info->phnum = read16(data + 56, info->is_le);
        info->shnum = read16(data + 60, info->is_le);
        info->shstrndx = read16(data + 62, info->is_le);
    } else {
        if (len < 52) return -1;
        info->entry = read32(data + 24, info->is_le);
        info->phoff = read32(data + 28, info->is_le);
        info->shoff = read32(data + 32, info->is_le);
        info->phnum = read16(data + 44, info->is_le);
        info->shnum = read16(data + 48, info->is_le);
        info->shstrndx = read16(data + 50, info->is_le);
    }
    return 0;
}

int elf_parse_sections(const unsigned char *data, size_t len,
                       ElfSectionInfo *out, int max_sections) {
    ElfInfo info;
    if (elf_parse_info(data, len, &info) < 0) return -1;

    int count = 0;
    int shentsize = info.is_64 ? 64 : 40;

    // Get section string table
    const char *shstrtab = NULL;
    if (info.shstrndx < info.shnum && info.shoff > 0) {
        unsigned char *shdr = (unsigned char *)(data + info.shoff + info.shstrndx * shentsize);
        uint64_t stroff = info.is_64 ? read64(shdr + 24, info.is_le) : read32(shdr + 16, info.is_le);
        uint64_t strsize = info.is_64 ? read64(shdr + 32, info.is_le) : read32(shdr + 20, info.is_le);
        if (stroff + strsize <= len) {
            shstrtab = (const char *)(data + stroff);
        }
    }

    for (int i = 0; i < info.shnum && count < max_sections; i++) {
        unsigned char *shdr = (unsigned char *)(data + info.shoff + i * shentsize);
        if ((size_t)(info.shoff + (i + 1) * shentsize) > len) break;

        ElfSectionInfo *sec = &out[count];
        memset(sec, 0, sizeof(ElfSectionInfo));

        uint32_t name_idx = read32(shdr, info.is_le);
        sec->type = read32(shdr + 4, info.is_le);
        sec->flags = info.is_64 ? read64(shdr + 8, info.is_le) : read32(shdr + 8, info.is_le);
        sec->addr = info.is_64 ? read64(shdr + 16, info.is_le) : read32(shdr + 12, info.is_le);
        sec->offset = info.is_64 ? read64(shdr + 24, info.is_le) : read32(shdr + 16, info.is_le);
        sec->size = info.is_64 ? read64(shdr + 32, info.is_le) : read32(shdr + 20, info.is_le);

        if (shstrtab && name_idx < len) {
            const char *name = shstrtab + name_idx;
            strncpy(sec->name, name, 63);
            sec->name[63] = '\0';
        }

        count++;
    }
    return count;
}

const char *elf_section_type_str(uint32_t type) {
    switch (type) {
        case SHT_NULL: return "NULL";
        case SHT_PROGBITS: return "PROGBITS";
        case SHT_SYMTAB: return "SYMTAB";
        case SHT_STRTAB: return "STRTAB";
        case SHT_RELA: return "RELA";
        case SHT_HASH: return "HASH";
        case SHT_DYNAMIC: return "DYNAMIC";
        case SHT_NOTE: return "NOTE";
        case SHT_NOBITS: return "NOBITS";
        case SHT_REL: return "REL";
        case SHT_DYNSYM: return "DYNSYM";
        case SHT_INIT_ARRAY: return "INIT_ARRAY";
        case SHT_FINI_ARRAY: return "FINI_ARRAY";
        case SHT_PREINIT_ARRAY: return "PREINIT_ARRAY";
        default: return "UNKNOWN";
    }
}
