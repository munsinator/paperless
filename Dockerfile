FROM node:24

# paperlessWebUI
WORKDIR /webUI

COPY package.json package-lock.json ./

RUN ["npm", "i"]

COPY . .

RUN ["npm", "run", "build"]

FROM nginx:stable-alpine

#output folder still needs to be specified
COPY --from=0 dist/web /usr/share/nginx/html
COPY ./nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
