/*
* deletes all successful jobs
*
* run with 'ucc run-groovy -f killall.groovy'
* 
*/

import eu.unicore.client.*
import eu.unicore.client.core.*
import eu.unicore.client.lookup.CoreEndpointLister

def kill(job, statuscode){
  if (job.status==statuscode){
     println("Deleting "+job.endpoint.url)
     job.delete()
  }
}

// iterate over sites and delete all jobs that are "SUCCESSFUL"

def endpoints = new CoreEndpointLister(registry, configurationProvider, auth)

endpoints.each {
   it.jobsList.each {
      def jc = new JobClient(new Endpoint(it), configurationProvider.getClientConfiguration(it), auth)
      kill(jc, JobClient.Status.SUCCESSFUL)
   }
}
