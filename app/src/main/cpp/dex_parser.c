#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

typedef struct {
    uint32_t file_size;
    uint32_t header_size;
    uint32_t endian_tag;
    uint32_t link_size;
    uint32_t map_off;
    uint32_t string_ids_size;
    uint32_t string_ids_off;
    uint32_t type_ids_size;
    uint32_t type_ids_off;
    uint32_t proto_ids_size;
    uint32_t proto_ids_off;
    uint32_t field_ids_size;
    uint32_t field_ids_off;
    uint32_t method_ids_size;
    uint32_t method_ids_off;
    uint32_t class_defs_size;
    uint32_t class_defs_off;
    uint32_t data_size;
    uint32_t data_off;
    char version[8];
} DexHeader;

typedef struct {
    char name[256];
    uint32_t type_idx;
    uint32_t access_flags;
    uint32_t superclass_idx;
    uint32_t source_file_idx;
    uint32_t class_data_off;
    uint32_t static_values_off;
} DexClass;

typedef struct {
    char name[256];
    char proto[256];
    uint32_t class_idx;
    uint32_t proto_idx;
    uint32_t name_idx;
    uint16_t access_flags;
} DexMethod;

static uint32_t dex_read32(const unsigned char *p) {
    return p[0] | (p[1] << 8) | (p[2] << 16) | (p[3] << 24);
}

int dex_validate(const unsigned char *data, size_t len) {
    if (len < 32) return 0;
    return memcmp(data, "dex\n", 4) == 0;
}

int dex_parse_header(const unsigned char *data, size_t len, DexHeader *header) {
    if (!dex_validate(data, len)) return -1;
    memset(header, 0, sizeof(DexHeader));

    memcpy(header->version, data + 4, 7);
    header->version[7] = '\0';
    header->file_size = dex_read32(data + 32);
    header->header_size = dex_read32(data + 36);
    header->endian_tag = dex_read32(data + 40);
    header->link_size = dex_read32(data + 44);
    header->map_off = dex_read32(data + 48);
    header->string_ids_size = dex_read32(data + 52);
    header->string_ids_off = dex_read32(data + 56);
    header->type_ids_size = dex_read32(data + 60);
    header->type_ids_off = dex_read32(data + 64);
    header->proto_ids_size = dex_read32(data + 68);
    header->proto_ids_off = dex_read32(data + 72);
    header->field_ids_size = dex_read32(data + 76);
    header->field_ids_off = dex_read32(data + 80);
    header->method_ids_size = dex_read32(data + 84);
    header->method_ids_off = dex_read32(data + 88);
    header->class_defs_size = dex_read32(data + 92);
    header->class_defs_off = dex_read32(data + 96);
    header->data_size = dex_read32(data + 100);
    header->data_off = dex_read32(data + 104);

    return 0;
}

static void dex_get_string(const unsigned char *data, size_t len,
                           uint32_t string_id_off, char *out, size_t out_size) {
    out[0] = '\0';
    if (string_id_off + 4 > len) return;
    uint32_t str_off = dex_read32(data + string_id_off);
    if (str_off >= len) return;

    // ULEB128 length
    const unsigned char *p = data + str_off;
    uint32_t str_len = 0;
    int shift = 0;
    while (p < data + len) {
        uint8_t b = *p++;
        str_len |= (uint32_t)(b & 0x7F) << shift;
        if ((b & 0x80) == 0) break;
        shift += 7;
    }

    // Skip MUTF-8 size, copy string
    // Simple ASCII copy
    size_t i = 0;
    while (i < str_len && i < out_size - 1 && p < data + len) {
        char c = (char)*p++;
        if (c == '\0') break;
        out[i++] = c;
    }
    out[i] = '\0';
}

int dex_parse_classes(const unsigned char *data, size_t len,
                      DexClass *out, int max_classes) {
    DexHeader header;
    if (dex_parse_header(data, len, &header) < 0) return -1;

    int count = 0;
    uint32_t off = header.class_defs_off;

    for (int i = 0; i < header.class_defs_size && count < max_classes; i++) {
        if (off + 32 > len) break;

        DexClass *cls = &out[count];
        memset(cls, 0, sizeof(DexClass));

        cls->access_flags = dex_read32(data + off + 4);
        cls->superclass_idx = dex_read32(data + off + 8);
        cls->source_file_idx = dex_read32(data + off + 28);
        cls->class_data_off = dex_read32(data + off + 32);
        cls->static_values_off = dex_read32(data + off + 36);

        uint32_t name_idx = dex_read32(data + off);
        if (header.string_ids_off + name_idx * 4 + 4 <= len) {
            dex_get_string(data, len, header.string_ids_off + name_idx * 4,
                          cls->name, sizeof(cls->name));
        }

        off += 32;
        count++;
    }
    return count;
}
