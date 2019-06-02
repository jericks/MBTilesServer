package org.cugos.mbtilesserver

import geoscript.geom.Bounds
import geoscript.geom.Point
import geoscript.layer.ImageTile
import geoscript.layer.io.PyramidWriter
import geoscript.layer.io.JsonPyramidWriter
import geoscript.proj.Projection
import groovy.json.JsonOutput
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.ModelAndView
import springfox.documentation.annotations.ApiIgnore

import javax.imageio.ImageIO
import javax.servlet.http.HttpServletResponse
import java.awt.image.RenderedImage

@Api(value = "MBTiles REST API")
@RestController
class Rest {

    @Autowired
    Config config

    @ApiIgnore
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "text/html")
    public ModelAndView home(Model model) {
        model.addAttribute("name", config.mbtiles.name)
        model.addAttribute("metadata", config.mbtiles.metadata)
        model.addAttribute("stats", config.mbtiles.tileCounts)
        new ModelAndView("home")
    }

    @RequestMapping(value = "raster/{w}/{h}/{bounds}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value="Get a Raster", notes="Stitch together a Raster from a set of tiles")
    HttpEntity<byte[]> raster(
            @PathVariable @ApiParam(value = "Width") int w,
            @PathVariable @ApiParam(value = "Height") int h,
            @PathVariable @ApiParam(value = "Bounds") String bounds
    ) throws IOException {
        String type = config.mbtiles.metadata.get("format","png") ?: "png"
        Bounds b = Bounds.fromString(bounds)
        b.proj = "EPSG:4326"
        RenderedImage image = config.mbtiles.getRaster(b, w, h).image
        byte[] bytes = getBytes(image, type)
        createHttpEntity(bytes, type)
    }

    @RequestMapping(value = "raster/{z}/{x}/{y}/{proj}/{w}/{h}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value="Get a Raster around a Point", notes="Stitch together a Raster from a set of tiles")
    HttpEntity<byte[]> rasterAroundPoint(
            @PathVariable @ApiParam(value = "Zoom Level") long z,
            @PathVariable @ApiParam(value = "X Coordinate") double x,
            @PathVariable @ApiParam(value = "Y Coordinate") double y,
            @PathVariable @ApiParam(value = "Projection") String proj,
            @PathVariable @ApiParam(value = "Width") int w,
            @PathVariable @ApiParam(value = "Height") int h
    ) throws IOException {
        String type = config.mbtiles.metadata.get("format","png") ?: "png"
        Point point = Projection.transform(new Point(x,y), new Projection(proj), config.mbtiles.proj)
        RenderedImage image = config.mbtiles.getRaster(point, z, w, h).image
        byte[] bytes = getBytes(image, type)
        createHttpEntity(bytes, type)
    }

    @RequestMapping(value = "tile/{z}/{x}/{y}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value="Get a Tile", notes="Get a Tile")
    HttpEntity<byte[]> getTile(
      @PathVariable @ApiParam(value = "Zoom Level") int z, 
      @PathVariable @ApiParam(value = "X") int x, 
      @PathVariable @ApiParam(value = "Y") int y
    ) throws IOException {
        ImageTile tile = config.mbtiles.get(z,x,y)
        byte[] bytes = tile.data
        createHttpEntity(bytes, config.mbtiles.metadata.get("format","png")  ?: "png")
    }

    @RequestMapping(value = "tile/{z}/{x}/{y}", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(value="Create a Tile", notes="Create a Tile")
    HttpEntity<byte[]> createTile(
      @PathVariable @ApiParam(value = "Zoom Level") int z, 
      @PathVariable @ApiParam(value = "X") int x, 
      @PathVariable @ApiParam(value = "Y") int y, 
      @RequestParam @ApiParam(value = "File") MultipartFile file
    ) throws IOException {
        if (config.readOnly) {
            new ResponseEntity<byte[]>(HttpStatus.METHOD_NOT_ALLOWED)
        } else {
            ImageTile tile = config.mbtiles.get(z, x, y)
            tile.data = file.bytes
            config.mbtiles.put(tile)
            byte[] bytes = tile.data
            createHttpEntity(bytes, config.mbtiles.metadata.get("format", "png") ?: "png")
        }
    }

    @RequestMapping(value = "tile/{z}/{x}/{y}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value="Delete a Tile", notes="Delete a Tile")
    HttpEntity<byte[]> deleteTile(
      @PathVariable @ApiParam(value = "Zoom Level") int z, 
      @PathVariable @ApiParam(value = "X") int x, 
      @PathVariable @ApiParam(value = "Y") int y
    ) throws IOException {
        if (config.readOnly) {
            new ResponseEntity<byte[]>(HttpStatus.METHOD_NOT_ALLOWED)
        } else {
            ImageTile tile = config.mbtiles.get(z, x, y)
            config.mbtiles.delete(tile)
            byte[] bytes = tile.data
            createHttpEntity(bytes, config.mbtiles.metadata.get("format", "png") ?: "png")
        }
    }

    @RequestMapping(value = "tile/counts", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ApiOperation(value="Count Tiles", notes="Count Tiles")
    String tileCounts() throws IOException {
        JsonOutput.prettyPrint(
            JsonOutput.toJson(config.mbtiles.tileCounts)
        )
    }

    @RequestMapping(value = "pyramid", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ApiOperation(value="Get Pyramid", notes="Get the tile pyramid")
    String pyramid() throws IOException {
        PyramidWriter writer = new JsonPyramidWriter()
        writer.write(config.mbtiles.pyramid)
    }

    @RequestMapping(value = "metadata", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ApiOperation(value="Get Metadata", notes="Get the tile metadata")
    String metadata() throws IOException {
        JsonOutput.prettyPrint(
            JsonOutput.toJson(config.mbtiles.metadata)
        )
    }

    @RequestMapping(value = "gdal", method = RequestMethod.GET, produces = "text/xml")
    @ResponseBody
    @ApiOperation(value="Get GDAL Layer", notes="Get GDAL Layer")
    String gdal() throws IOException {
        """<GDAL_WMS>
    <Service name="TMS">
        <ServerUrl>http://${config.hostName}:${config.port}/tile/\${z}/\${x}/\${y}</ServerUrl>
    </Service>
    <DataWindow>
        <UpperLeftX>-20037508.34</UpperLeftX>
        <UpperLeftY>20037508.34</UpperLeftY>
        <LowerRightX>20037508.34</LowerRightX>
        <LowerRightY>-20037508.34</LowerRightY>
        <TileLevel>${config.mbtiles.tileCounts.last().zoom}</TileLevel>
        <TileCountX>1</TileCountX>
        <TileCountY>1</TileCountY>
        <YOrigin>bottom</YOrigin>
    </DataWindow>
    <Projection>EPSG:3857</Projection>
    <BlockSizeX>256</BlockSizeX>
    <BlockSizeY>256</BlockSizeY>
    <BandsCount>3</BandsCount>
    <Cache/>
</GDAL_WMS>
"""
    }

    @ExceptionHandler(IllegalArgumentException)
    void handleBadRequests(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value())
    }

    protected HttpEntity createHttpEntity(byte[] bytes, String type) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Empty Image!")
        }
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(type.equalsIgnoreCase("png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
        headers.setContentLength(bytes.length)
        new HttpEntity<byte[]>(bytes, headers)
    }

    protected byte[] getBytes(RenderedImage image, String type) {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        ImageIO.write(image, type, out)
        out.close()
        out.toByteArray()
    }
}
