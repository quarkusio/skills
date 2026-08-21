FROM eclipse-temurin:11-jdk-focal

USER root
# install uv and python tooling for smoke tests
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl python3 python3-venv \
    && rm -rf /var/lib/apt/lists/* \
    && curl -LsSf https://astral.sh/uv/install.sh | sh

ENV PATH="/root/.local/bin:$PATH"

# Shared browsers path so Chromium is cached once
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN mkdir -p /ms-playwright && chmod 755 /ms-playwright

# create venv and install
RUN uv venv /opt/venv     && uv pip install --python /opt/venv/bin/python playwright==1.47.0 pytest

ENV PATH="/opt/venv/bin:$PATH"
RUN playwright install --with-deps chromium

# Set working directory
WORKDIR /app

# Copy all the assets
COPY . .

RUN if [ -f gradlew ]; then chmod +x gradlew; fi && if [ -f smoke.py ]; then chmod +x smoke.py; fi && if [ -f test.sh ]; then chmod +x test.sh; fi

CMD [ "./gradlew", "clean", "bootRun" ]
