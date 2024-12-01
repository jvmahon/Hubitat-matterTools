/* 
Reference: Matter Application Cluster Specification Version 1.2 ("Matter Cluster Spec"), Section 5.2 ("Door Lock Cluster")
*/

library (
        base: "driver",
        author: "jvm33",
        category: "matter",
        description: "Door Lock Cluster 0x0101 Tools",
        name: "doorLockClusterMethods0x0101",
        namespace: "matterTools",
        documentationLink: "https://github.com/jvmahon/Hubitat-Matter",
		version: "0.0.1"
)
import hubitat.helper.HexUtils

// Following functions implement Matter Spec 5.2.10 "lockDoor" command.
void lock( Map params = [:] ) {
    try { 
        Map inputs = [ep: null , mode: null] << params
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.PINCode instanceof String
                
        List<Map<String, String>> fields = []
        if (inputs.PINCode) {
            log.warn "probably need to reverse the pin code if one is used!"
            fields.add(matter.cmdField(DataType.STRING_OCTET1, 0, inputs.PINCode)) // PIN Code
        }
        String cmd = matter.invoke(inputs.ep, 0x0101, 0x00, fields) // Lock Door
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing lockDoor with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

void unlock( Map params = [:] ) {
    try { 
        Map inputs = [ep: null , mode: null] << params
        assert inputs.ep instanceof Integer  // Check that endpoint is an integer
        assert inputs.PINCode instanceof String
                
        List<Map<String, String>> fields = []
        if (inputs.PINCode) {
            log.warn "probably need to reverse the pin code if one is used!"
            fields.add(matter.cmdField(DataType.STRING_OCTET1, 0, inputs.PINCode)) // PIN Code
        }
        String cmd = matter.invoke(inputs.ep, 0x0101, 0x01, fields) // Unlock Door
        sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.MATTER))     
    } catch (AssertionError e) {
        log.error "<pre>${e}<br><br>Stack trace:<br>${getStackTrace(e) }"
    } catch(e){
        log.error "<pre>${e}<br><br>when processing unlockDoor with inputs ${inputs}<br><br>Stack trace:<br>${getStackTrace(e) }"
    }
}

void deleteCode(codeposition){
}

void getCodes() {
}
void setCode(){
}
void setCodeLength(pincodelength){
}