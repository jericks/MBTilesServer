package org.cugos.mbtilesserver

import org.apache.commons.io.FileUtils
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.web.context.WebApplicationContext

import static org.junit.Assert.assertTrue
import static org.hamcrest.Matchers.*
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

@RunWith(SpringJUnit4ClassRunner)
@SpringBootTest(classes = App)
@TestPropertySource(locations = "classpath:readWrite.properties")
@WebAppConfiguration
class RestReadWriteTest {

    private MockMvc mockMvc

    @BeforeClass
    static void preSetup() {
        File source = new File("src/test/resources/countries.mbtiles")
        File destination = new File("src/test/resources/countries2.mbtiles")
        FileUtils.copyFile(source, destination)
    }

    @AfterClass
    static void tearDown() {
        File file = new File("src/test/resources/countries2.mbtiles")
        file.delete()
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    void setup() {
        mockMvc = webAppContextSetup(webApplicationContext).build()
    }

    @Test
    void home() {
        mockMvc.perform(get("/")).andExpect(status().isOk())
    }

    @Test
    void raster() {
        MvcResult result = mockMvc.perform(get("/raster/400/400/-156.533203,3.688855,-50.712891,56.800878"))
                .andExpect(status().isOk()).andReturn()
        assertTrue result.response.contentAsByteArray.length > 0
    }

    @Test
    void rasterAroundPoint() {
        MvcResult result = mockMvc.perform(get("/raster/4/-122.387/47.581/EPSG:4326/400/400"))
                .andExpect(status().isOk()).andReturn()
        assertTrue result.response.contentAsByteArray.length > 0
    }

    @Test
    void tile() {
        MvcResult result = mockMvc.perform(get("/tile/0/0/0"))
                .andExpect(status().isOk()).andReturn()
        assertTrue result.response.contentAsByteArray.length > 0
    }

    @Test
    void createTile() {
       byte[] bytes = getClass().getClassLoader().getResource("tile.png").bytes
       mockMvc.perform(fileUpload("/tile/6/0/0").file("file", bytes))
               .andExpect(status().isOk())
               .andExpect(content().bytes(bytes))
    }

    @Test
    void deleteTile() {
        byte[] bytes = getClass().getClassLoader().getResource("tile.png").bytes
        mockMvc.perform(fileUpload("/tile/6/0/0").file("file", bytes))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
        mockMvc.perform(delete("/tile/6/0/0"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
        mockMvc.perform(get("/tile/6/0/0")).andExpect(status().is4xxClientError())
    }

    @Test
    void tileCounts() {
        mockMvc.perform(get("/tile/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$', hasSize(4)))
                .andExpect(jsonPath('$[0].zoom', is(0)))
                .andExpect(jsonPath('$[0].tiles', is(1)))
                .andExpect(jsonPath('$[0].total', is(1)))
                .andExpect(jsonPath('$[0].percent', is(1.0 as Double)))
                .andExpect(jsonPath('$[1].zoom', is(1)))
                .andExpect(jsonPath('$[1].tiles', is(4)))
                .andExpect(jsonPath('$[1].total', is(4)))
                .andExpect(jsonPath('$[1].percent', is(1.0 as Double)))
                .andExpect(jsonPath('$[2].zoom', is(2)))
                .andExpect(jsonPath('$[2].tiles', is(16)))
                .andExpect(jsonPath('$[2].total', is(16)))
                .andExpect(jsonPath('$[2].percent', is(1.0 as Double)))
                .andExpect(jsonPath('$[3].zoom', is(3)))
                .andExpect(jsonPath('$[3].tiles', is(64)))
                .andExpect(jsonPath('$[3].total', is(64)))
                .andExpect(jsonPath('$[3].percent', is(1.0 as Double)))
    }

    @Test
    void pyramid() {
        mockMvc.perform(get("/pyramid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.proj', is('EPSG:3857')))
                .andExpect(jsonPath('$.bounds.minX', closeTo(-2.0036395147881314E7 as Double, 0.001)))
                .andExpect(jsonPath('$.bounds.minY', closeTo(-2.0037471205137067E7 as Double, 0.001)))
                .andExpect(jsonPath('$.bounds.maxX', closeTo(2.0036395147881314E7 as Double, 0.001)))
                .andExpect(jsonPath('$.bounds.maxY', closeTo(2.003747120513706E7 as Double, 0.001)))
                .andExpect(jsonPath('$.origin', is('BOTTOM_LEFT')))
                .andExpect(jsonPath('$.tileSize.width', is(256)))
                .andExpect(jsonPath('$.tileSize.height', is(256)))
                .andExpect(jsonPath('$.grids', hasSize(20)))
                .andExpect(jsonPath('$.grids[0].z', is(0)))
                .andExpect(jsonPath('$.grids[0].width', is(1)))
                .andExpect(jsonPath('$.grids[0].height', is(1)))
                .andExpect(jsonPath('$.grids[0].xres', is(156412.0 as Double)))
                .andExpect(jsonPath('$.grids[0].yres', is(156412.0 as Double)))
                .andExpect(jsonPath('$.grids[19].z', is(19)))
                .andExpect(jsonPath('$.grids[19].width', is(524288)))
                .andExpect(jsonPath('$.grids[19].height', is(524288)))
                .andExpect(jsonPath('$.grids[19].xres', closeTo(0.29833221435546875 as Double, 0.00001)))
                .andExpect(jsonPath('$.grids[19].yres', closeTo(0.29833221435546875 as Double, 0.00001)))
    }

    @Test
    void metadata() {
        mockMvc.perform(get("/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.type', is('base_layer')))
                .andExpect(jsonPath('$.name', is('countries')))
                .andExpect(jsonPath('$.description', is('countries')))
                .andExpect(jsonPath('$.format', is('png')))
                .andExpect(jsonPath('$.version', is('1.0')))
                .andExpect(jsonPath('$.attribution', is('Created with GeoScript')))
                .andExpect(jsonPath('$.bounds', is('-179.99,-85.0511,179.99,85.0511')))
    }

    @Test
    void gdal() {
        mockMvc.perform(get("/gdal"))
                .andExpect(status().isOk())
                .andExpect(xpath("/GDAL_WMS/Service/@name").string("TMS"))
                .andExpect(xpath("/GDAL_WMS/Service/ServerUrl").string(startsWith('http://')))
                .andExpect(xpath("/GDAL_WMS/Service/ServerUrl").string(containsString('tile/${z}/${x}/${y}')))
                .andExpect(xpath("/GDAL_WMS/DataWindow/UpperLeftX").number(-20037508.34))
                .andExpect(xpath("/GDAL_WMS/DataWindow/UpperLeftY").number(20037508.34))
                .andExpect(xpath("/GDAL_WMS/DataWindow/LowerRightX").number(20037508.34))
                .andExpect(xpath("/GDAL_WMS/DataWindow/LowerRightY").number(-20037508.34))
                .andExpect(xpath("/GDAL_WMS/DataWindow/TileLevel").number(3))
                .andExpect(xpath("/GDAL_WMS/DataWindow/TileCountX").number(1))
                .andExpect(xpath("/GDAL_WMS/DataWindow/TileCountY").number(1))
                .andExpect(xpath("/GDAL_WMS/DataWindow/YOrigin").string('bottom'))
                .andExpect(xpath("/GDAL_WMS/Projection").string('EPSG:3857'))
                .andExpect(xpath("/GDAL_WMS/BlockSizeX").string('256'))
                .andExpect(xpath("/GDAL_WMS/BlockSizeY").string('256'))
                .andExpect(xpath("/GDAL_WMS/BandsCount").string('3'))
    }


}
