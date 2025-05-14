# syntax=docker/dockerfile:1
FROM rust:slim-bookworm as builder
WORKDIR /app
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
WORKDIR /app
# Install postgresql-client, build-essential, and curl
RUN apt-get update && apt-get install -y postgresql-client build-essential curl
# Install sqlx-cli
SHELL ["/bin/bash", "-c"]
RUN curl https://sh.rustup.rs -sSf | bash -s -- -y \
    && source $HOME/.cargo/env \
    && cargo install sqlx-cli --no-default-features --features postgres
COPY --from=builder /app/target/release/backend /app/backend
COPY .env .env
COPY migrations ./migrations
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENV PATH="/root/.cargo/bin:${PATH}"
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"] 