/*
* for testing the Groovy script facility
*/

println "This is a test Groovy script"

//check that the context for Groovy scripts is setup correctly
assert(registry!=null)
assert(configurationProvider!=null)
assert(registryURL!=null)
assert(properties!=null)
assert(options!=null)
assert(commandLine!=null)

// example from UCC manual

// list storages from registry
registry.listEntries("StorageManagement").each { 
    messageWriter.message("Storage at "+it.getUrl())			    
}
