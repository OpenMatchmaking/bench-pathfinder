package benchmark.Echo

import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.math.{round}
import scala.util.Random


class EchoWithJwt extends Simulation {
  val server = System.getProperty("REMOTE_SERVER", "ws://pathfinder:9000")
  val usersCount = Integer.getInteger("users", 100)
  val testDuration = Integer.getInteger("duration", 60)
  val stages = Integer.getInteger("stages", 5)
  val rampLasts = Integer.getInteger("ramp", 10)

  val usersAtStart: Int       = round(usersCount.toFloat * 0.20f)
  val usersIncrementStep: Int = round((usersCount.toFloat * 0.80f) / stages)
  val levelDuration: Int      = round(testDuration.toFloat / stages)

  def getRandomUserData() = {
    val random = new Random()
    val timestamp = LocalDateTime.now
    val username = "user_%d_%d".format(timestamp.hashCode(), random.nextLong())
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(username.getBytes())
    val bigInt = new BigInteger(1, digest)
    val hashedString = bigInt.toString(16)
    Map("user"-> hashedString)
  }
  val usersFeeder = Iterator.continually(getRandomUserData)

  val registerUserTemplate = """
  |{
  |  "url": "auth\/api\/users\/register",
  |  "content": {
  |     "username": "${user}",
  |     "password": "123456",
  |     "confirm_password": "123456"
  |  },
  |  "event-name": "echo-with-jwt-benchmark-step-1"
  |}""".stripMargin

  val registerNewUser = ws("Register user")
    .sendText(registerUserTemplate)
    .await(300 seconds)(
      ws.checkTextMessage("UserHasBeenRegisteredSuccessfully")
        .check(jsonPath("$.error").notExists)
        .check(jsonPath("$.event-name").ofType[String].is("echo-with-jwt-benchmark-step-1"))
        .check(jsonPath("$.content").exists)
    )

  val generateTokenTemplate = """
  |{
  |  "url": "auth\/api\/token\/new",
  |  "content": {
  |     "username": "${user}",
  |     "password": "123456"
  |  },
  |  "event-name": "echo-with-jwt-benchmark-step-2"
  |}""".stripMargin

  val generateTokenForUser = ws("Generate JSON Web Token")
    .sendText(generateTokenTemplate)
    .await(300 seconds)(
      ws.checkTextMessage("UserReceivedNewJsonWebToken")
        .check(jsonPath("$.error").notExists)
        .check(jsonPath("$.event-name").ofType[String].is("echo-with-jwt-benchmark-step-2"))
        .check(jsonPath("$.content").exists)
        .check(jsonPath("$.content.access_token").ofType[String].saveAs("token"))
    )

  val echoMessageTemplate = """
  |{
  |  "url": "benchmark\/api\/echo\/jwt",
  |  "content": {
  |     "text": "value"
  |  },
  |  "event-name": "echo-with-jwt-benchmark-step-3",
  |  "token": "${token}"
  |}""".stripMargin

  val sendEchoRequest = ws("Send request")
    .sendText(echoMessageTemplate)
    .await(300 seconds)(
      ws.checkTextMessage("EchoResponseIsValid")
        .check(jsonPath("$.error").notExists)
        .check(jsonPath("$.event-name").ofType[String].is("echo-with-jwt-benchmark-step-3"))
        .check(jsonPath("$.content").ofType[String].is("""{"text":"value"}"""))
    )

  val httpProtocol = http.wsBaseUrl(server)
    .wsReconnect
    .wsMaxReconnects(5)

  val scn = scenario("Receive valid response from microservice-echo with JSON Web Token check.")
    .feed(usersFeeder)
    .exec(ws("Connect").connect("/"))
    .pause(1)
    .exec(registerNewUser)
    .pause(1)
    .exec(generateTokenForUser)
    .pause(1)
    .exec(sendEchoRequest)
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
