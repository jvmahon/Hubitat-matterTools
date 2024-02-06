metadata {
   definition (name: "Temporary - Parse Any", namespace: "matterTools", author: "My Name") { 
   }

    command "test"
    
   preferences {
      // None for now
   }
}


#include matterTools.createMatterEvents
import hubitat.helper.ColorUtils
import hubitat.helper.HexUtils

void test(){
    
    
    log.info HexHue2Name("EF")



log.info getHubitatEvents(clusterInt:0x0400, attrInt:0x0000, value:"8FFF")
log.info  getHubitatEvents(clusterInt:0x0400, attrInt:0x0000, value:"00FF")
log.info  getHubitatEvents(clusterInt:0x0300, attrInt:0x0000, value:"8F")    


}

