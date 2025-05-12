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
COPY ./migrations ./migrations
COPY .env .env
ENV PATH="/root/.cargo/bin:${PATH}"
EXPOSE 8080
CMD bash -c "until pg_isready -h db -p 5432; do sleep 1; done && DATABASE_URL=$DATABASE_URL sqlx migrate run --source ./migrations && /app/backend" 