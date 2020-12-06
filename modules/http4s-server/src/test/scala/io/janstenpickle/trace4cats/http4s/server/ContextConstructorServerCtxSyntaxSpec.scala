package io.janstenpickle.trace4cats.http4s.server

import cats.data.Kleisli
import cats.effect.IO
import cats.{~>, Id}
import io.janstenpickle.trace4cats.http4s.common.TraceContext
import io.janstenpickle.trace4cats.http4s.server.syntax._

import scala.concurrent.ExecutionContext

class ContextConstructorServerCtxSyntaxSpec
    extends BaseServerTracerSpec[IO, Kleisli[IO, TraceContext[IO], *]](
      9183,
      λ[IO ~> Id](_.unsafeRunSync()),
      λ[Kleisli[IO, TraceContext[IO], *] ~> IO](ga => TraceContext.empty[IO].flatMap(ga.run)),
      (routes, filter, ep) =>
        routes.tracedContext(
          Http4sResourceReaders
            .fromHeadersContext(TraceContext.make[IO], requestFilter = filter)(ep.toReader)
        ),
      (app, filter, ep) =>
        app.tracedContext(
          Http4sResourceReaders
            .fromHeadersContext(TraceContext.make[IO], requestFilter = filter)(ep.toReader)
        ),
      IO.timer(ExecutionContext.global)
    )
