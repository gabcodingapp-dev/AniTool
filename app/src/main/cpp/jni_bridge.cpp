#include <jni.h>
#include <string>
#include <cstring>
#include <cmath>

// Capstone
#include <capstone/capstone.h>

// C functions
extern "C" {
#include "elf_parser.c"
#include "pe_parser.c"
#include "dex_parser.c"
#include "obfuscate.c"
#include "patch_utils.c"
}

// Helper: get byte array from Java
static jbyteArray get_bytes(JNIEnv *env, jbyteArray arr) {
    return arr;
}

// ======== ELF ========
extern "C" JNIEXPORT jboolean JNICALL
Java_com_oprek_tool_core_NativeLib_elfValidate(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = elf_validate((const unsigned char *)bytes, len);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return result ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_oprek_tool_core_NativeLib_elfGetInfo(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    ElfInfo info;
    int ret = elf_parse_info((const unsigned char *)bytes, len, &info);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    if (ret < 0) return env->NewStringUTF("Invalid ELF");

    char buf[512];
    snprintf(buf, sizeof(buf),
        "Arch: %s %s\n"
        "Entry: 0x%016llX\n"
        "Program Headers: %u @ 0x%llX\n"
        "Section Headers: %u @ 0x%llX\n"
        "Section StrTab idx: %u\n"
        "File size: %llu bytes",
        info.is_64 ? "ELF64" : "ELF32",
        info.is_le ? "Little Endian" : "Big Endian",
        info.entry, info.phnum, info.phoff,
        info.shnum, info.shoff, info.shstrndx, info.file_size);
    return env->NewStringUTF(buf);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_oprek_tool_core_NativeLib_elfGetSections(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);

    ElfSectionInfo sections[256];
    int count = elf_parse_sections((const unsigned char *)bytes, len, sections, 256);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    if (count <= 0) {
        jclass strClass = env->FindClass("java/lang/String");
        jobjectArray arr = env->NewObjectArray(0, strClass, nullptr);
        return arr;
    }

    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(count, strClass, nullptr);

    for (int i = 0; i < count; i++) {
        char buf[512];
        snprintf(buf, sizeof(buf), "%s|%s|0x%llX|%llu|0x%llX|%u",
            sections[i].name,
            elf_section_type_str(sections[i].type),
            sections[i].offset,
            sections[i].size,
            sections[i].addr,
            sections[i].flags);
        env->SetObjectArrayElement(result, i, env->NewStringUTF(buf));
    }

    return result;
}

// ======== DISASSEMBLER (CAPSTONE) ========
extern "C" JNIEXPORT jstring JNICALL
Java_com_oprek_tool_core_NativeLib_disassemble(JNIEnv *env, jclass,
        jbyteArray code, jlong offset, jint arch, jint mode, jint count) {

    jsize len = env->GetArrayLength(code);
    jbyte *bytes = env->GetByteArrayElements(code, nullptr);

    csh handle;
    cs_insn *insn;
    cs_arch cs_arch_val;
    cs_mode cs_mode_val;

    switch (arch) {
        case 0: cs_arch_val = CS_ARCH_ARM; break;
        case 1: cs_arch_val = CS_ARCH_ARM64; break;
        case 2: cs_arch_val = CS_ARCH_X86; break;
        default: cs_arch_val = CS_ARCH_ARM64; break;
    }

    switch (mode) {
        case 0: cs_mode_val = CS_MODE_ARM; break;
        case 1: cs_mode_val = CS_MODE_THUMB; break;
        case 2: cs_mode_val = CS_MODE_ARM64; break;
        case 3: cs_mode_val = CS_MODE_64; break;
        case 4: cs_mode_val = CS_MODE_32; break;
        default: cs_mode_val = CS_MODE_ARM64; break;
    }

    std::string result;
    if (cs_open(cs_arch_val, cs_mode_val, &handle) != CS_ERR_OK) {
        env->ReleaseByteArrayElements(code, bytes, JNI_ABORT);
        return env->NewStringUTF("Failed to initialize Capstone");
    }

    insn = cs_malloc(handle);
    const uint8_t *code_ptr = (const uint8_t *)bytes;
    size_t code_size = len;
    uint64_t addr = (uint64_t)offset;
    int printed = 0;

    while (cs_disasm_iter(handle, &code_ptr, &code_size, &addr, insn) && printed < count) {
        char line[256];
        snprintf(line, sizeof(line), "0x%016llX:  %-8s %s %s\n",
            insn->address, insn->mnemonic, insn->op_str, "");
        result += line;
        printed++;
    }

    cs_free(insn, 1);
    cs_close(&handle);
    env->ReleaseByteArrayElements(code, bytes, JNI_ABORT);

    return env->NewStringUTF(result.c_str());
}

// ======== OBFUSCATE ========
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_oprek_tool_core_NativeLib_xorEncrypt(JNIEnv *env, jclass,
        jbyteArray data, jbyte key) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);

    jbyteArray result = env->NewByteArray(len);
    jbyte *out = env->GetByteArrayElements(result, nullptr);

    obf_xor((const unsigned char *)bytes, len,
            (unsigned char *)out, (uint8_t)key);

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    env->ReleaseByteArrayElements(result, out, 0);
    return result;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_oprek_tool_core_NativeLib_entropy(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    double ent = obf_entropy((const unsigned char *)bytes, len);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return ent;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_oprek_tool_core_NativeLib_detectPacker(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = obf_detect_packer((const unsigned char *)bytes, len);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return result;
}

// ======== PATCH ========
extern "C" JNIEXPORT jint JNICALL
Java_com_oprek_tool_core_NativeLib_patchNop(JNIEnv *env, jclass,
        jbyteArray data, jlong offset) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = patch_nop((unsigned char *)bytes, len, offset);
    env->ReleaseByteArrayElements(data, bytes, 0);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_oprek_tool_core_NativeLib_patchRet(JNIEnv *env, jclass,
        jbyteArray data, jlong offset) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = patch_ret((unsigned char *)bytes, len, offset);
    env->ReleaseByteArrayElements(data, bytes, 0);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_oprek_tool_core_NativeLib_patchRetZero(JNIEnv *env, jclass,
        jbyteArray data, jlong offset) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = patch_ret_zero((unsigned char *)bytes, len, offset);
    env->ReleaseByteArrayElements(data, bytes, 0);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_oprek_tool_core_NativeLib_patchBranchUncond(JNIEnv *env, jclass,
        jbyteArray data, jlong offset, jlong target) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = patch_branch((unsigned char *)bytes, len, offset, target - offset);
    env->ReleaseByteArrayElements(data, bytes, 0);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_oprek_tool_core_NativeLib_patchCondToUncond(JNIEnv *env, jclass,
        jbyteArray data, jlong offset) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = patch_cond_to_uncond((unsigned char *)bytes, len, offset);
    env->ReleaseByteArrayElements(data, bytes, 0);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_oprek_tool_core_NativeLib_searchPattern(JNIEnv *env, jclass,
        jbyteArray data, jbyteArray pattern, jlong start) {
    jsize dlen = env->GetArrayLength(data);
    jsize plen = env->GetArrayLength(pattern);
    jbyte *dbytes = env->GetByteArrayElements(data, nullptr);
    jbyte *pbytes = env->GetByteArrayElements(pattern, nullptr);

    int64_t result = search_pattern((const unsigned char *)dbytes, dlen,
        (const unsigned char *)pbytes, plen, (size_t)start);

    env->ReleaseByteArrayElements(data, dbytes, JNI_ABORT);
    env->ReleaseByteArrayElements(pattern, pbytes, JNI_ABORT);
    return (jlong)result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_oprek_tool_core_NativeLib_packerName(JNIEnv *env, jclass, jint id) {
    return env->NewStringUTF(obf_packer_name(id));
}

// ======== PE ========
extern "C" JNIEXPORT jboolean JNICALL
Java_com_oprek_tool_core_NativeLib_peValidate(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = pe_validate((const unsigned char *)bytes, len);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return result ? JNI_TRUE : JNI_FALSE;
}

// ======== DEX ========
extern "C" JNIEXPORT jboolean JNICALL
Java_com_oprek_tool_core_NativeLib_dexValidate(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    int result = dex_validate((const unsigned char *)bytes, len);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return result ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_oprek_tool_core_NativeLib_dexGetInfo(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    DexHeader header;
    int ret = dex_parse_header((const unsigned char *)bytes, len, &header);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    if (ret < 0) return env->NewStringUTF("Invalid DEX");

    char buf[1024];
    snprintf(buf, sizeof(buf),
        "DEX Version: %s\n"
        "File Size: %u bytes\n"
        "Header Size: %u bytes\n"
        "Endian Tag: 0x%08X\n"
        "String IDs: %u @ 0x%X\n"
        "Type IDs: %u @ 0x%X\n"
        "Proto IDs: %u @ 0x%X\n"
        "Field IDs: %u @ 0x%X\n"
        "Method IDs: %u @ 0x%X\n"
        "Class Defs: %u @ 0x%X\n"
        "Data: %u @ 0x%X",
        header.version, header.file_size, header.header_size,
        header.endian_tag, header.string_ids_size, header.string_ids_off,
        header.type_ids_size, header.type_ids_off,
        header.proto_ids_size, header.proto_ids_off,
        header.field_ids_size, header.field_ids_off,
        header.method_ids_size, header.method_ids_off,
        header.class_defs_size, header.class_defs_off,
        header.data_size, header.data_off);
    return env->NewStringUTF(buf);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_oprek_tool_core_NativeLib_dexGetClasses(JNIEnv *env, jclass, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);

    DexClass classes[1024];
    int count = dex_parse_classes((const unsigned char *)bytes, len, classes, 1024);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(count, strClass, nullptr);

    for (int i = 0; i < count; i++) {
        char buf[512];
        snprintf(buf, sizeof(buf), "%s|0x%X", classes[i].name, classes[i].access_flags);
        env->SetObjectArrayElement(result, i, env->NewStringUTF(buf));
    }
    return result;
}
