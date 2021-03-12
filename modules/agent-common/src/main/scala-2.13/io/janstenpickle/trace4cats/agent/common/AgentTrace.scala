package io.janstenpickle.trace4cats.agent.common

import java.net.InetAddress
import cats.effect.{Concurrent, Resource, Sync, Timer}
import fs2.{Chunk, Pipe}
import io.janstenpickle.trace4cats.kernel.{SpanExporter, SpanSampler}
import io.janstenpickle.trace4cats.meta.{PipeTracer, TracedSpanExporter}
import io.janstenpickle.trace4cats.model.{AttributeValue, CompletedSpan, TraceProcess}
import io.janstenpickle.trace4cats.rate.sampling.RateSpanSampler

object AgentTrace {
  def apply[F[_]: Concurrent: Timer](
    exporterName: String,
    exporterAttributes: Map[String, AttributeValue],
    listenerPort: Int,
    sampleRate: Option[Double],
    bufferSize: Int,
    exporter: SpanExporter[F, Chunk]
  ): Resource[F, (Pipe[F, CompletedSpan, CompletedSpan], SpanExporter[F, Chunk])] = for {
    hostname <- Resource.liftF(Sync[F].delay(InetAddress.getLocalHost.getHostName))
    process = TraceProcess("trace4cats-agent", Map("hostname" -> hostname))

    sampler <- sampleRate.fold(Resource.pure[F, SpanSampler[F]](SpanSampler.always[F]))(rate =>
      RateSpanSampler.create[F](bufferSize, rate)
    )

    pipe = PipeTracer[F](
      Map("listen.protocol" -> "udp", "listen.format" -> "avro", "listen.port" -> listenerPort),
      process,
      sampler
    )
  } yield pipe -> TracedSpanExporter[F](exporterName, exporterAttributes, process, sampler, exporter)
}
