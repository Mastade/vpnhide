#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# Build the kernel module if .ko is not fresh
KSRC="${KSRC:-$HOME/tmp_zfs/kernel_build/kernel-source}"
CLANG="${CLANG:-$HOME/Android/Sdk/ndk/29.0.14033849/toolchains/llvm/prebuilt/linux-x86_64/bin}"

if [ ! -f vpnhide_kmod.ko ] || [ vpnhide_kmod.c -nt vpnhide_kmod.ko ]; then
    echo "Building kernel module..."
    make -C "$KSRC" M=$(pwd) \
        ARCH=arm64 LLVM=1 LLVM_IAS=1 \
        CC=$CLANG/clang LD=$CLANG/ld.lld \
        AR=$CLANG/llvm-ar NM=$CLANG/llvm-nm \
        OBJCOPY=$CLANG/llvm-objcopy \
        OBJDUMP=$CLANG/llvm-objdump \
        STRIP=$CLANG/llvm-strip \
        CROSS_COMPILE=aarch64-linux-gnu- \
        modules
fi

cp vpnhide_kmod.ko module/vpnhide_kmod.ko

OUT="vpnhide-kmod.zip"
rm -f "$OUT"
(cd module && zip -qr "../$OUT" .)

echo
echo "Built: $OUT"
ls -lh "$OUT"
