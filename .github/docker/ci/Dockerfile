# CI image for vpnhide-zygisk.
#
# Bakes in everything build-zip.sh needs so each CI run skips ~3-5
# minutes of NDK download + apt installs + cargo-install. Rebuilt by
# the ci-image.yml workflow when this Dockerfile changes (and once a
# month on schedule).

FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates curl unzip zip xz-utils \
        build-essential pkg-config \
        cmake ninja-build \
        openjdk-17-jdk-headless \
        git && \
    rm -rf /var/lib/apt/lists/*

ENV RUSTUP_HOME=/usr/local/rustup \
    CARGO_HOME=/usr/local/cargo \
    PATH=/usr/local/cargo/bin:$PATH

RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | \
        sh -s -- -y --default-toolchain stable --profile minimal --no-modify-path && \
    rustup target add aarch64-linux-android && \
    cargo install cargo-ndk --locked && \
    chmod -R a+w /usr/local/rustup /usr/local/cargo

ENV ANDROID_NDK_VERSION=r28b
ENV ANDROID_NDK_HOME=/opt/android-ndk

RUN curl -fsSL -o /tmp/ndk.zip \
        "https://dl.google.com/android/repository/android-ndk-${ANDROID_NDK_VERSION}-linux.zip" && \
    unzip -q /tmp/ndk.zip -d /opt && \
    mv "/opt/android-ndk-${ANDROID_NDK_VERSION}" "${ANDROID_NDK_HOME}" && \
    rm /tmp/ndk.zip

RUN chmod -R a+rx "${ANDROID_NDK_HOME}"
