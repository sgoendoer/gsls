# gsls
Global Social Lookup System (SONIC)

## api

- GET /
- GET /:gid
- POST /:gid
- PUT /:gid

## install

- build via 
```
mvn clean
mvn install
docker build -t sonic/gsls:0.2.3 .
```

- run via 
```
docker run -d -p 4001:4001/tcp -p 4001:4001/udp -p 4002:4002/tcp --restart=always sonic/gsls:0.2.3
```
