metadata {
   definition (name: "Interface Test", namespace: "interfaceTest", author: "jvm33") { 
        command "menuTest1", [[name:"choices", type:"ENUM", constraints:[0:"Red", 1:"Blue", 2:"Green"] ]]    
        command "resetSettings"
        command "resetSettings2"
        command "getSettings"

    }
   preferences {
        input( name:"menuTest2", type:"enum", options:[0:"Green", 1:"Yellow", 2:"Purple"])
        input(name:"turnOffAfter", type:"number", title:"<B>Automatically Turn off After (seconds):</b>", description:"Number of Seconds for Automatic Turn-Off, blank to disable automatic off")
   }
}

    
void updated(){
   // def value = settings.menuTest2
    log.info settings
    log.info "Turn off after settting is ${settings.turnOffAfter } of class ${settings?.turnOffAfter?.class}"
        def value = device.getSetting("menuTest2")
    log.info "Preference data is ${value } of class ${value.class}"
}
    
void resetSettings(){
    // device.updateSetting("menuTest2",[value:"0", type:"text"])
    settings.menuTest2="0"
}

void resetSettings2(){
    log.info "Attempting to update setting to value of Yellow"
    // device.updateSetting("menuTest2", "2")
    device.updateSetting("menuTest2", [value:"1", type:"enum"])
}
void getSettings(){
    def thing = settings
    log.info thing.class
    log.info settings
}