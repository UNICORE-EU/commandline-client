/*
* for testing the Groovy script facility
*/
println "*** Groovy test script."
println ""
println "The following variables are available:"
println ""
binding.variables.each {
  println it.key +": " + it.value.getClass().name
}
println ""

// check that the context for Groovy scripts is setup as expected
assert auth!=null
assert configurationProvider!=null
assert registry!=null
assert registryURL!=null
assert properties!=null
assert options!=null
assert commandLine!=null

// example from UCC manual

// list core service endpoints from registry
registry.listEntries("CoreServices").each {
    console.info("Core API endpoint at: {}", it.url)
}
println "*** END of Groovy test"