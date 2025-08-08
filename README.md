Pre-Req: Ensure you have docker installed

RUN docker compose -f docker-compose.yml build --no-cache

RUN docker compose -f docker-compose.yml up -d
