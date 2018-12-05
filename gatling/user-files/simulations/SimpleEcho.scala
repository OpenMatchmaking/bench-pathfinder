package benchmark.Echo

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


class SimpleEcho extends Simulation {
  val server = System.getProperty("REMOTE_SERVER", "ws://pathfinder:9000/")
  val count = Integer.getInteger("users", 100)
  val rampPeriod = Integer.getInteger("ramp", 60)

  val message = """
  |{
  |  "url": "benchmark\/api\/echo",
  |  "content": {
  |     "text": "value"
  |  },
  |  "event-name": "simple-echo-benchmark"
  |}""".stripMargin

  val scn = scenario("Receive valid response from microservice-echo without token check.")
    .exec(ws("Gateway").connect(server))
    .pause(1)
    .exec(
      ws("Gateway")
        .sendText(message)
        .await(30 seconds)(
          ws.checkTextMessage("EchoResponseIsValid")
            .check(jsonPath("$.error").notExists)
            .check(jsonPath("$.event-name").ofType[String].is("simple-echo-benchmark"))
            .check(jsonPath("$.content").ofType[String].is("""{"text":"value"}"""))
        )
    )
    .pause(1)
    .exec(ws("Gateway").close)

  setUp(scn.inject(rampUsers(count) during (rampPeriod seconds)))
}
