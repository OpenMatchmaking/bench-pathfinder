package benchmark.SimpleEcho

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


class SimpleEcho extends Simulation {
  val message = """
  |{
  |  "url": "benchmark\/api\/test",
  |  "content": {
  |     "text": "value"
  |  },
  |  "event-name": "simple-echo-benchmark"
  |}""".stripMargin

  val scn = scenario("Receive echo response from a microservice.")
    .exec(ws("Gateway").connect("ws://pathfinder:9000/"))
    .pause(1)
    .exec(
      ws("Gateway")
        .sendText(message)
        .await(10 seconds)(
          ws.checkTextMessage("CheckEchoResponse")
            .check(jsonPath("$.error").is(null))
            .check(jsonPath("$.content").ofType[String].is(message))
        )
    )
    .pause(1)
    .exec(ws("Gateway").close)

  setUp(scn.inject(rampUsers(100) during (30 seconds)))
}
