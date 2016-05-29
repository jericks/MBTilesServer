MBTiles Server
--------------
Server your MBTiles Map on the internet.

Build
-----

```bash
./gradlew build
```

Run
---
```bash
java -jar mbtileserver-0.1.0.jar --file=countries.mbtiles --readOnly=true
```

or if you are building the project:

```bash
java -jar build/libs/mbtileserver-0.1.0.jar --file=countries.mbtiles --readOnly=true
```

and browse to http://localhost:8080:

![alt tag](docs/webapp.png)

Examples
--------

**Inspect metadata**

```bash
curl http://localhost:8080/metadata
```

```bash
curl http://localhost:8080/pyramid
```

```bash
curl http://localhost:8080/tile/counts
```

**Create QGIS/GDAL Layer File**

```bash
curl -o layer.xml http://localhost:8080/gdal && qgis layer.xml
```

![alt tag](docs/qgis.png)

**Download Tiles**

```bash
curl -o tile_0_0_0.png http://localhost:8080/tile/0/0/0
```

```bash
curl -o tile_1_0_0.png http://localhost:8080/tile/1/0/0
```

**Download Raster**
```bash
curl -o us.png http://localhost:8080//raster/400/400/-156.533203,3.688855,-50.712891,56.800878
```

**Add/Update a Tile**
```bash
curl -F "file=@tile.png" http://localhost:8080/tile/5/0/0
```

```bash
curl -o tile_5_0_0.png http://localhost:8080/tile/5/0/0
```

**Delete a Tile**
```bash
curl -X DELETE http://localhost:8080/tile/5/0/0
```

```bash
curl http://localhost:8080/tile/5/0/0
```