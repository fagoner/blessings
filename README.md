## build
```
docker build -t bless:dev .
```
## deploy
```
docker run -ti --rm -p 8080:8080 --link mysql_5726_mysql_1:mysql --net="mysql_5726_default" bless:dev
```