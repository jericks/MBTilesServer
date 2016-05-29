package org.cugos.mbtileserver

import org.apache.commons.io.FileUtils
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup

@RunWith(SpringJUnit4ClassRunner)
@SpringApplicationConfiguration(classes = App)
@TestPropertySource(locations = "classpath:test.properties")
@WebAppConfiguration
class RestTest {

    private MockMvc mockMvc

    @BeforeClass
    static void preSetup() {
        File source = new File("src/test/resources/countries.mbtiles")
        File destination = new File("src/test/resources/countries2.mbtiles")
        FileUtils.copyFile(source, destination)
    }

    @AfterClass
    static void tearDown() {
        File file = new File("/src/test/resources/countries2.mbtiles")
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
        mockMvc.perform(get("/raster/400/400/-156.533203,3.688855,-50.712891,56.800878")).andExpect(status().isOk())
    }

    @Test
    void tile() {
        mockMvc.perform(get("/tile/0/0/0")).andExpect(status().isOk())
    }

    @Test
    void createTile() {
       byte[] bytes = getClass().getClassLoader().getResource("tile.png").bytes
       mockMvc.perform(fileUpload("/tile/6/0/0").file("file", bytes))
               .andExpect(status().isOk())
               .andExpect(content().bytes(bytes))
    }

    @Test
    void updateTile() {
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
        mockMvc.perform(get("/tile/counts")).andExpect(status().isOk())
    }

    @Test
    void pyramid() {
        mockMvc.perform(get("/pyramid")).andExpect(status().isOk())
    }

    @Test
    void metadata() {
        mockMvc.perform(get("/metadata")).andExpect(status().isOk())
    }

    @Test
    void gdal() {
        mockMvc.perform(get("/gdal")).andExpect(status().isOk())
    }

}
