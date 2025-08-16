/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 1.8 ("Mode Select Cluster")
*/

library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "mode Select Control Cluster 0x0050 Tools",
        name: "modeSelectClusterMethods0x0050",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import hubitat.helper.HexUtils

// Following functions implement Matter Spec 1.8.7 "ChangeToMode" command.
void changeToMode( Map params = [:] ) {
    try { 
        Map inputs = [ep: null , mode: null] << params
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.mode instanceof Integer
                
        String hexMode = HexUtils.integerToHexString((Integer) inputs.mode, 1)
        List<Map<String, String>> fields = []
        fields.add(matter.cmdField(DataType.UINT8, 0, hexMode)) // Mode
        String cmd = matter.invoke(inputs.ep, 0x0050, 0x00, fields) // ChangeToMode
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing changeToMode with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

Map getModeSelectOptions(Integer ep){
    getStoredAttributeData(endpointInt:ep, clusterInt:0x0050, attrInt:0x0002)?.collectEntries({[ (it[1]), (it[0]) ] }) 
}

Integer getModeSelectCurrentMode(Integer ep){
    return getStoredAttributeData(endpointInt:ep, clusterInt:0x0050, attrInt:0x0003)
}

String getModeSelectLabel(Integer ep){
    log.debug "Mode Select Data model is: " + getDataValue("model")
    switch (getDataValue("model")) {
        case "VTM36":
        	if (ep in [3, 4, 5, 6, 7, 8]) return "<pre><i>Not Used for this device</i></prev>"
        	break;
        case "VTM31-SN":
        case "VTM35-SN":
        	if (ep in [7, 8]) return "<pre><i>Not Used for this device</i></prev>"
        	break;
    }
    "<b>${getStoredAttributeData(endpointInt:ep, clusterInt:0x0050, attrInt:0x0000) ?: "Initialize Device then Refresh Browser"}</b>"
}

void handleModeSelectClusterUpdate(decodedDescriptionMap){
    switch(decodedDescriptionMap.attrInt){
        case 0x0003: // Current Mode
            String settingsName = "ModeSelect_${decodedDescriptionMap.endpointInt}"
            String newValue = "${decodedDescriptionMap.decodedValue}" // Hubitat oddity - all device.settings keys are strings, so newValue must be a number in string format, even though the index to menuItems was originally an Intger number
            device.updateSetting(settingsName, [value:newValue, type:"enum"])
            break
        }
}