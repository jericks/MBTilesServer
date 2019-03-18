package org.cugos.mbtilesserver

import geoscript.layer.MBTiles
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class Config implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    int port

    String hostName

    @Value('${readOnly:true}')
    boolean readOnly

    MBTiles mbtiles

    @Value('${file}')
    private String fileName

    @PostConstruct
    private void mbtiles() {
        mbtiles = new MBTiles(new File(fileName).canonicalFile)
    }

    @Override
    void customize(ConfigurableServletWebServerFactory factory) {
        port = factory.port
        hostName = InetAddress.localHost.hostName
    }
}
