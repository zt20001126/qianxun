ARG DOCKER_REGISTRY_PREFIX=docker.io
FROM ${DOCKER_REGISTRY_PREFIX}/library/node:22-alpine AS build
WORKDIR /workspace
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM ${DOCKER_REGISTRY_PREFIX}/library/nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
