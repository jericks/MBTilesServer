package org.cugos.mbtilesserver

import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import static org.junit.Assert.*

@RunWith(SpringJUnit4ClassRunner)
@SpringBootTest(classes = App, webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "server.port=8080")
@TestPropertySource(locations = "classpath:readWrite.properties")
class AppTest {

    private WebDriver browser

    private static File dir = new File("build/screenshots")

    @BeforeClass
    static void beforeClass() {
        // Copy mbtiles file to keep the original unchanged
        File source = new File("src/e2e/resources/countries.mbtiles")
        File destination = new File("src/e2e/resources/countries2.mbtiles")
        FileUtils.copyFile(source, destination)
        // Prepare screenshots directory
        FileUtils.deleteDirectory(dir)
        dir.mkdirs()
    }

    @AfterClass
    static void afterClass() {
        File file = new File("src/e2e/resources/countries2.mbtiles")
        file.delete()
    }

    @Before
    void before() {
        browser = new FirefoxDriver()
    }

    @After
    void after() {
        browser.quit()
    }

    private void captureScreenShot(String name) {
        File scrFile = ((TakesScreenshot)browser).getScreenshotAs(OutputType.FILE)
        FileUtils.copyFile(scrFile, new File(dir, name + ".png"))
    }

    @Test
    void url() {
        browser.get("http://localhost:8080/")
        assertEquals("http://localhost:8080/", browser.getCurrentUrl())
        assertEquals("MBTiles Web Services", browser.getTitle())
        captureScreenShot("web")
    }

    @Test
    void header() {
        browser.get("http://localhost:8080/")
        String header = browser.findElement(By.tagName("h1")).getText()
        assertEquals("MBTiles Web Services", header)
    }

    @Test
    void metadata() {
        browser.get("http://localhost:8080/")
        WebElement e = browser.findElement(By.className("metadata"))
        List<WebElement> divs = e.findElements(By.tagName("div"))
        assertEquals("Name = countries", divs[0].getText())
        assertEquals("Description = countries", divs[1].getText())
        assertEquals("Type = base_layer", divs[2].getText())
        assertEquals("Format = png", divs[3].getText())
        assertEquals("Version = 1.0", divs[4].getText())
    }

    @Test
    void tileStats() {
        browser.get("http://localhost:8080/")
        WebElement e = browser.findElement(By.className("tilestats"))
        List<WebElement> rows = e.findElement(By.tagName("tbody")).findElements(By.tagName("tr"))
        Closure checkRow = { WebElement row, int zoom, int total, int tiles, String percent  ->
            List<WebElement> cells = row.findElements(By.tagName("td"))
            assertEquals(zoom, cells.get(0).getText() as int)
            assertEquals(total, cells.get(1).getText() as int)
            assertEquals(tiles, cells.get(2).getText() as int)
            assertEquals(percent, cells.get(3).getText())
        }
        rows.eachWithIndex { WebElement row, int index ->
            checkRow(row, index, Math.pow(4, index) as int, Math.pow(4, index) as int, "100%")
        }
    }

    @Test
    void map() {
        browser.get("http://localhost:8080/")
        WebElement map = browser.findElement(By.id("map"))
        WebElement zoomIn = map.findElement(By.className("ol-zoom-in"))
        zoomIn.click()
        Thread.sleep(250)
        zoomIn.click()
        Thread.sleep(250)
        captureScreenShot("map_zoom_in")
        WebElement zoomOut = map.findElement(By.className("ol-zoom-out"))
        zoomOut.click()
        Thread.sleep(250)
        zoomOut.click()
        Thread.sleep(250)
        captureScreenShot("map_zoom_out")
    }

    @Test
    void pyramidWebService() {
        browser.get("http://localhost:8080/")
        browser.findElement(By.linkText("Pyramid")).click()
        assertEquals("http://localhost:8080/pyramid", browser.getCurrentUrl())
        captureScreenShot("pyramid_service")
    }

    @Test
    void tileWebService() {
        browser.get("http://localhost:8080/")
        browser.findElement(By.linkText("Tiles")).click()
        assertEquals("http://localhost:8080/tile/0/0/0", browser.getCurrentUrl())
        captureScreenShot("tile_service")
    }


    @Test
    void rasterWebService() {
        browser.get("http://localhost:8080/")
        browser.findElement(By.linkText("Raster")).click()
        assertEquals("http://localhost:8080/raster/400/400/-156.533203,3.688855,-50.712891,56.800878", browser.getCurrentUrl())
        captureScreenShot("raster_service")
    }

    @Test
    void rasterAroundPointWebService() {
        browser.get("http://localhost:8080/")
        browser.findElement(By.linkText("Raster around Point")).click()
        assertEquals("http://localhost:8080/raster/4/-122.387/47.581/EPSG:4326/400/400", browser.getCurrentUrl())
        captureScreenShot("raster_around_point_service")
    }

    @Test
    void metadataWebService() {
        browser.get("http://localhost:8080/")
        browser.findElement(By.linkText("Metadata")).click()
        assertEquals("http://localhost:8080/metadata", browser.getCurrentUrl())
        captureScreenShot("metadata_service")
    }

    @Test
    void tileStatsWebService() {
        browser.get("http://localhost:8080/")
        browser.findElement(By.linkText("Tile Counts")).click()
        assertEquals("http://localhost:8080/tile/counts", browser.getCurrentUrl())
        captureScreenShot("tilecounts_service")
    }

    @Test
    void qgisLayerWebService() {
        browser.get("http://localhost:8080/")
        browser.findElement(By.linkText("QGIS Layer")).click()
        assertEquals("http://localhost:8080/gdal", browser.getCurrentUrl())
        captureScreenShot("gdal_service")
    }
}
