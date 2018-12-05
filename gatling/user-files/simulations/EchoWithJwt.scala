package benchmark.Echo

import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


class EchoWithJwt extends Simulation {
  val server = "ws://pathfinder:9000/"
  val count = 1//Integer.getInteger("users", 100)
  val rampPeriod = 1//Integer.getInteger("ramp", 60)

  def getRandomUserData() = {
    val timestamp = LocalDateTime.now
    val timestamp_hash = BigInteger.valueOf(timestamp.hashCode()).toByteArray()
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(timestamp_hash)
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

  val registerNewUser = ws("Gateway")
    .sendText(registerUserTemplate)
    .await(10 seconds)(
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

  val generateTokenForUser = ws("Gateway")
    .sendText(generateTokenTemplate)
    .await(10 seconds)(
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

  val sendEchoRequest = ws("Gateway")
    .sendText(echoMessageTemplate)
    .await(10 seconds)(
      ws.checkTextMessage("EchoResponseIsValid")
        .check(jsonPath("$.error").notExists)
        .check(jsonPath("$.event-name").ofType[String].is("echo-with-jwt-benchmark-step-3"))
        .check(jsonPath("$.content").ofType[String].is("""{"text":"value"}"""))
    )

  val scn = scenario("Receive valid response from microservice-echo with JSON Web Token check.")
    .feed(usersFeeder)
    .exec(ws("Gateway").connect(server))
    .pause(1)
    .exec(registerNewUser)
    .pause(1)
    .exec(generateTokenForUser)
    .pause(1)
    .exec(sendEchoRequest)
    .pause(1)
    .exec(ws("Gateway").close)

  setUp(scn.inject(rampUsers(count) during (rampPeriod seconds)))
}
