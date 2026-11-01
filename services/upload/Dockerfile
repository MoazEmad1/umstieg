FROM python:3.13.18-slim
RUN pip install --no-cache-dir huggingface_hub==2.4.0 \
    && useradd --uid 1000 --create-home app
COPY upload.sh dataset-card.md /opt/upload/
USER app
ENV HF_HOME=/tmp/hf HF_HUB_DISABLE_TELEMETRY=1
ENTRYPOINT ["sh", "/opt/upload/upload.sh"]
