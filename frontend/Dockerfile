# Stage 1 — build
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG NG_APP_API_URL=http://localhost:8080/api
ENV NG_APP_API_URL=$NG_APP_API_URL
RUN npx ng build --configuration production

# Stage 2 — serve
FROM nginx:alpine
COPY --from=build /app/dist/sales-dashboard/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
