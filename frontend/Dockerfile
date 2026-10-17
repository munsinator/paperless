FROM node:24

# paperlessWebUI
WORKDIR /ui

COPY package.json package-lock.json ./

RUN ["npm", "ci"]

COPY . .

RUN ["npm", "run", "build"]

FROM nginx:stable-alpine

COPY --from=0 /ui/dist/frontend/browser/ /usr/share/nginx/html/
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
