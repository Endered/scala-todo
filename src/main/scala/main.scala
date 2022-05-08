import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import scala.io.StdIn
import scalikejdbc.ConnectionPool
import scalikejdbc.DB
import scalikejdbc._
import repository.TodoRepositoryImpl
import repository.TodoRepository
import usecase.TodoUsecase
import usecase.TodoUsecaseImpl
import controller.TodoControllerImpl
import controller.TodoController

object Main {
  import implicits._

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "my-system")
    implicit val executionContext = system.executionContext

    val url = "jdbc:mysql://127.0.0.1:3306/test-db"
    val user = "test"
    val password = "password"
    ConnectionPool.singleton(url, user, password)
    val conn = ConnectionPool.borrow()
    using(DB(ConnectionPool.borrow())) { db =>
      db.autoClose(false)
      db.localTx { implicit session =>
        sql"""
CREATE TABLE IF NOT EXISTS todos(
id BIGINT PRIMARY KEY AUTO_INCREMENT,
content TEXT NOT NULL,
completed BOOLEAN NOT NULL
)
""".update.apply()
      }
      val todoRepository: TodoRepository = TodoRepositoryImpl()
      val todoUsecase: TodoUsecase = TodoUsecaseImpl(db, todoRepository)
      val todoController: TodoController = TodoControllerImpl(todoUsecase)

      val route = pathPrefix("todo") {
        get {
          todoController.GetTodoByID
        } ~ patch {
          pathPrefix("id") {
            todoController.UpdateTodo
          } ~ pathPrefix("toggle") {
            todoController.ToggleCompletedTodo
          }
        } ~ post {
          todoController.CreateTodo
        }
      }
      val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

      println(
        s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop..."
      )
      StdIn.readLine()
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }
  }
}

object implicits {
  implicit class RichTodoError(error: controller.TodoError) {
    def toStatusCode: StatusCode = error match {
      case controller.TodoError.NotFound    => StatusCodes.NotFound
      case controller.TodoError.InvalidData => StatusCodes.BadRequest
      case controller.TodoError.DBError     => StatusCodes.InternalServerError
    }
  }
}
