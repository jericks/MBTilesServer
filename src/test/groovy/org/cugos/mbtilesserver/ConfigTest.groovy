package org.cugos.mbtilesserver

import geoscript.layer.MBTiles
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class ConfigTest {

    @Test
    void create() {
        Config config = new Config(
                port: 8080,
                hostName: "localhost",
                mbtiles: new MBTiles(new File(getClass().getClassLoader().getResource("countries.mbtiles").toURI()))
        )
        assertEquals 8080, config.port
        assertEquals "localhost", config.hostName
        assertNotNull config.mbtiles
    }

}
