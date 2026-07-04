FROM mcr.microsoft.com/devcontainers/java:17-bookworm

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        git \
        jq \
        unzip \
        netcat-openbsd \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER vscode
