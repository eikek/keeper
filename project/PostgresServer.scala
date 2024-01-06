import scala.sys.process._

object PostgresServer {
  // When using a local postgres for development, use this env variable
  // to not start a postgres server via docker for the tests
  val skipServer = sys.env.contains("KEEPER_NO_POSTGRES")

  val containerName = "keeper-test-postgres"
  val startCmd =
    s"docker run --rm --name $containerName -e POSTGRES_PASSWORD=dev -e POSTGRES_USER=dev -e POSTGRES_DB=keeper_test -p 5432:5432 -d postgres"
  val stopCmd = s"docker stop -t5 $containerName"
  val isReadyCmd = s"docker exec $containerName pg_isready"

  def start(): Unit =
    if (skipServer) println(s"Not starting postgres via docker")
    else {
      println("Starting latest PostgreSQL docker for testing")
      startCmd.!!
      var rc = 1
      while (rc != 0) {
        Thread.sleep(500)
        rc = isReadyCmd.!
      }
    }

  def stop(): Unit =
    if (!skipServer) {
      println("Stopping PostgreSQL container")
      stopCmd.!!
    }
}
