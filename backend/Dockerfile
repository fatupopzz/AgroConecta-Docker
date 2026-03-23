FROM node:20-alpine

LABEL maintainer="AgroConecta Team – UVG CC3090 2026"
LABEL description="Backend API REST de AgroConecta"

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

EXPOSE 8080

CMD ["npm", "run", "dev"]
