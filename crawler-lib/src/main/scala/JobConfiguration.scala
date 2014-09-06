package org.blikk.crawler

import com.typesafe.config.{Config, ConfigFactory}

object JobConfiguration {
  def empty(jobId: String) = JobConfiguration(jobId, Seq.empty[WrappedHttpRequest], Nil, ConfigFactory.empty)
}

case class JobConfiguration(jobId: String, seeds: Seq[WrappedHttpRequest], 
  processors: List[ResponseProcessor], settings: Config)