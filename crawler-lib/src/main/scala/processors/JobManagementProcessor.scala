// package org.blikk.crawler.processors

// import org.blikk.crawler._
// import org.blikk.crawler.channels.JobChannelInput
// import org.blikk.crawler.channels.JobChannelInput.Actions
// import scala.collection.JavaConversions._


// object JobManagementProcessor {
//   def terminateWhen(name: String)(func: (ResponseProcessorInput => Boolean)) = {
//     new JobManagementProcessor(name, Actions.Stop)(func)
//   }
// }

// class JobManagementProcessor(val name: String, action: JobChannelInput.JobAction)
//   (func: (ResponseProcessorInput => Boolean)) extends ResponseProcessor {

//   def process(in: ResponseProcessorInput) : Map[String, ProcessorOutput] = {
//     func(in) match {
//       case true => Map(name -> JobChannelInput(action))
//       case false => Map.empty
//     }
//   }
// }