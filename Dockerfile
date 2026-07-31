# =============================================================================
# CausalRepair — reproducible runtime image
#
# Self-contained environment for the CausalRepair LLM-based APR framework:
#   - JDK 8 + Defects4J 2.0.1 (project repos fetched at build time)
#   - Python 3.12 + project dependencies
#   - The CausalRepair code, dataset metadata, and correct patches
#
# Build:  docker build -t causalrepair:latest .
# Run:    docker run --rm -e SILICONFLOW_API_KEY=sk-... \
#             -v "$PWD/results:/results" causalrepair:latest run
#
# You must supply your own LLM API key at run time (see README).
# =============================================================================
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

# --- System dependencies (Defects4J: JDK8, perl, cpanm, git, svn; build tools) ---
# Optional apt mirror for regions where archive.ubuntu.com is slow/unreliable.
# Default uses the official archive; override e.g.:
#   docker build --build-arg APT_MIRROR=mirrors.ustc.edu.cn -t causalrepair:latest .
ARG APT_MIRROR=""
RUN if [ -n "$APT_MIRROR" ]; then \
        sed -i "s@//archive.ubuntu.com@//${APT_MIRROR}@g; s@//security.ubuntu.com@//${APT_MIRROR}@g" /etc/apt/sources.list; \
    fi \
    && apt-get -o Acquire::Retries=5 update \
    && apt-get -o Acquire::Retries=5 install -y --no-install-recommends \
        openjdk-8-jdk \
        perl \
        cpanminus \
        git \
        subversion \
        build-essential \
        unzip \
        zip \
        wget \
        curl \
        ca-certificates \
        software-properties-common \
        locales \
    && locale-gen C.UTF-8 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# --- Defects4J 2.0.1 (pinned; line numbers in the dataset are tied to this snapshot) ---
RUN git clone https://github.com/rjust/defects4j.git /opt/defects4j \
    && cd /opt/defects4j \
    && git checkout v2.0.1 \
    && cpanm --installdeps . \
    && ./init.sh

ENV PATH="/opt/defects4j/framework/bin:${PATH}"

# --- Python 3.12 (deadsnakes) + isolated venv with project dependencies ---
RUN apt-get -o Acquire::Retries=5 update \
    && apt-get -o Acquire::Retries=5 install -y --no-install-recommends gnupg dirmngr \
    && add-apt-repository -y ppa:deadsnakes/ppa \
    && apt-get -o Acquire::Retries=5 update \
    && apt-get -o Acquire::Retries=5 install -y --no-install-recommends \
        python3.12 \
        python3.12-venv \
        python3.12-dev \
    && rm -rf /var/lib/apt/lists/* \
    && python3.12 -m venv /opt/venv

ENV PATH="/opt/venv/bin:${PATH}"

COPY requirements.txt /opt/causalrepair/requirements.txt
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r /opt/causalrepair/requirements.txt

# Pre-cache the tiktoken cl100k_base vocabulary at build time (avoids a runtime download).
ENV TIKTOKEN_CACHE_DIR=/opt/tiktoken_cache
RUN mkdir -p /opt/tiktoken_cache \
    && python -c "import tiktoken; tiktoken.get_encoding('cl100k_base')"

# --- Application code, data, scripts ---
WORKDIR /opt/causalrepair
COPY utils/            /opt/causalrepair/utils/
COPY Defects4J/        /opt/causalrepair/Defects4J/
COPY Results/          /opt/causalrepair/Results/
COPY iterative_repair.py collect_plausible_patches.py augment_patches.py /opt/causalrepair/
COPY scripts/          /opt/causalrepair/scripts/
RUN chmod +x /opt/causalrepair/scripts/*.sh

# --- Runtime configuration ---
# Provide your API key at run time, e.g. -e SILICONFLOW_API_KEY=sk-...
ENV CAUSALREPAIR_DATA=/opt/causalrepair/Defects4J \
    DEFECTS4J_CMD=/opt/defects4j/framework/bin/defects4j \
    CAUSALREPAIR_PROVIDER=siliconflow \
    CAUSALREPAIR_MODEL=deepseek-ai/DeepSeek-V3 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/causalrepair/scripts:${PATH}"

ENTRYPOINT ["/opt/causalrepair/scripts/entrypoint.sh"]
CMD ["help"]
