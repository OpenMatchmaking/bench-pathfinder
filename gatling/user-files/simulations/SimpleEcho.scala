package benchmark.Echo

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.math.{round}


class SimpleEcho extends Simulation {
  val server = System.getProperty("REMOTE_SERVER", "ws://pathfinder:9000")
  val usersCount = Integer.getInteger("users", 100)
  val testDuration = Integer.getInteger("duration", 60)
  val stages = Integer.getInteger("stages", 5)
  val rampLasts = Integer.getInteger("ramp", 10)

  val usersAtStart: Int       = round(usersCount.toFloat * 0.20f)
  val usersIncrementStep: Int = round((usersCount.toFloat * 0.80f) / stages)
  val levelDuration: Int      = round(testDuration.toFloat / stages)

  val message = """
  |{
  |  "url": "benchmark\/api\/echo",
  |  "content": {
  |     "text": "value"
  |  },
  |  "event-name": "simple-echo-benchmark"
  |}""".stripMargin

  val httpProtocol = http.wsBaseUrl(server)
    .wsReconnect
    .wsMaxReconnects(5)

  val scn = scenario("Receive valid response from microservice-echo without token check.")
    .exec(ws("Connect").connect("/"))
    .pause(1)
    .exec(
      ws("Send request")
        .sendText(message)
        .await(30 seconds)(
          ws.checkTextMessage("EchoResponseIsValid")
            .check(jsonPath("$.error").notExists)
            .check(jsonPath("$.event-name").ofType[String].is("simple-echo-benchmark"))
            .check(jsonPath("$.content").ofType[String].is("""{"text":"value"}"""))
        )
    )
    .pause(1)
    .exec(ws("Close").close)

  setUp(scn.inject(
    incrementConcurrentUsers(usersIncrementStep)
      .times(stages)
      .eachLevelLasting(levelDuration seconds)
      .separatedByRampsLasting(rampLasts seconds)
      .startingFrom(usersAtStart)
  )).protocols(httpProtocol)
}
