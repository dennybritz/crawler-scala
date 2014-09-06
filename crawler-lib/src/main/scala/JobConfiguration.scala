package org.blikk.crawler

import com.typesafe.config.{Config, ConfigFactory}

object JobConfiguration {
  def empty(jobId: String) = new JobConfiguration(jobId, Nil, Nil, ConfigFactory.empty)
}

case class JobConfiguration(
  jobId: String, 
  seeds: List[WrappedHttpRequest], 
  processors: List[ResponseProcessor], 
  settings: Config) extends Serializable