package org.cugos.mbtilesserver

import geoscript.geom.Bounds
import geoscript.layer.ImageTile
import geoscript.layer.io.PyramidWriter
import geoscript.layer.io.JsonPyramidWriter
import groovy.json.JsonOutput
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

import javax.imageio.ImageIO
import javax.servlet.http.HttpServletResponse
import java.awt.image.RenderedImage

@RestController
class Rest {

    @Autowired
    Config config

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "text/html")
    public ModelAndView home(Model model) {
        model.addAttribute("name", config.mbtiles.name)
        model.addAttribute("metadata", config.mbtiles.metadata)
        model.addAttribute("stats", config.mbtiles.tileCounts)
        new ModelAndView("home")
    }

    @RequestMapping("raster/{w}/{h}/{bounds}")
    @ResponseBody
    HttpEntity<byte[]> raster(@PathVariable int w, @PathVariable int h, @PathVariable String bounds) throws IOException {
        String type = config.mbtiles.metadata.get("format","png")
        Bounds b = Bounds.fromString(bounds)
        b.proj = "EPSG:4326"
        RenderedImage image = config.mbtiles.getRaster(b, w, h).image
        byte[] bytes = getBytes(image, type)
        createHttpEntity(bytes, type)
    }

    @RequestMapping(value = "tile/{z}/{x}/{y}", method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<byte[]> getTile(@PathVariable int z, @PathVariable int x, @PathVariable int y) throws IOException {
        ImageTile tile = config.mbtiles.get(z,x,y)
        byte[] bytes = tile.data
        createHttpEntity(bytes, config.mbtiles.metadata.get("format","png"))
    }

    @RequestMapping(value = "tile/{z}/{x}/{y}", method = RequestMethod.POST)
    @ResponseBody
    HttpEntity<byte[]> createTile(@PathVariable int z, @PathVariable int x, @PathVariable int y, @RequestParam MultipartFile file) throws IOException {
        if (config.readOnly) {
            new ResponseEntity<byte[]>(HttpStatus.METHOD_NOT_ALLOWED)
        } else {
            ImageTile tile = config.mbtiles.get(z, x, y)
            tile.data = file.bytes
            config.mbtiles.put(tile)
            byte[] bytes = tile.data
            createHttpEntity(bytes, config.mbtiles.metadata.get("format", "png"))
        }
    }

    @RequestMapping(value = "tile/{z}/{x}/{y}", method = RequestMethod.PUT)
    @ResponseBody
    HttpEntity<byte[]> updateTile(@PathVariable int z, @PathVariable int x, @PathVariable int y, @RequestParam MultipartFile file) throws IOException {
        if (config.readOnly) {
            new ResponseEntity<byte[]>(HttpStatus.METHOD_NOT_ALLOWED)
        } else {
            ImageTile tile = config.mbtiles.get(z, x, y)
            tile.data = file.bytes
            config.mbtiles.put(tile)
            byte[] bytes = tile.data
            createHttpEntity(bytes, config.mbtiles.metadata.get("format", "png"))
        }
    }

    @RequestMapping(value = "tile/{z}/{x}/{y}", method = RequestMethod.DELETE)
    @ResponseBody
    HttpEntity<byte[]> deleteTile(@PathVariable int z, @PathVariable int x, @PathVariable int y) throws IOException {
        if (config.readOnly) {
            new ResponseEntity<byte[]>(HttpStatus.METHOD_NOT_ALLOWED)
        } else {
            ImageTile tile = config.mbtiles.get(z, x, y)
            config.mbtiles.delete(tile)
            byte[] bytes = tile.data
            createHttpEntity(bytes, config.mbtiles.metadata.get("format", "png"))
        }
    }

    @RequestMapping(value = "tile/counts", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    String tileCounts() throws IOException {
        JsonOutput.prettyPrint(
            JsonOutput.toJson(config.mbtiles.tileCounts)
        )
    }

    @RequestMapping(value = "pyramid", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    String pyramid() throws IOException {
        PyramidWriter writer = new JsonPyramidWriter()
        writer.write(config.mbtiles.pyramid)
    }

    @RequestMapping(value = "metadata", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    String metadata() throws IOException {
        JsonOutput.prettyPrint(
            JsonOutput.toJson(config.mbtiles.metadata)
        )
    }

    @RequestMapping(value = "gdal", method = RequestMethod.GET, produces = "text/xml")
    @ResponseBody
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