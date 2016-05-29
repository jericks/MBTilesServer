package org.cugos.mbtileserver

import geoscript.layer.MBTiles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class Config implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

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

    public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
        port = event.getEmbeddedServletContainer().getPort()
        hostName = InetAddress.localHost.hostName
    }
}
