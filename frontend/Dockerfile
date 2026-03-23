FROM node:20-alpine

LABEL maintainer="AgroConecta Team – UVG CC3090 2026"
LABEL description="Frontend de pruebas AgroConecta"

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 3000

CMD ["npm", "start"]
