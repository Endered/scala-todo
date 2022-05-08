package controller

import usecase.TodoUsecase
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

case class TodoCreateJson(content: String) {
  def toTodo: Either[ControllerError, domain.Todo] =
    Right(domain.Todo(0, content, false))
}

case class TodoDetailJson(id: String, content: String, completed: Boolean)
case class TodoUpdateJson(id: String, content: String, completed: Boolean) {
  def toTodo: Either[ControllerError, domain.Todo] = id.toLongOption
    .toRight { InvalidDataError }
    .map(id => domain.Todo(id, content, completed))
}

sealed trait TodoError
object TodoError {
  case object NotFound extends TodoError
  case object DBError extends TodoError
  case object InvalidData extends TodoError
}

trait TodoController {
  def GetTodoByID: Route
  def CreateTodo: Route
  def UpdateTodo: Route
  def ToggleCompletedTodo: Route
}

class TodoControllerImpl(todoUsecase: TodoUsecase) extends TodoController {
  import TodoStuff.RichTodo
  import implicits.RichTodoError

  implicit val todoCreateJson =
    jsonFormat(TodoCreateJson.apply _, "content")
  implicit val todoDetailJson = jsonFormat(
    TodoDetailJson.apply _,
    "id",
    "content",
    "completed"
  )
  implicit val todoUpdateJson = jsonFormat(
    TodoUpdateJson.apply _,
    "id",
    "content",
    "completed"
  )

  def GetTodoByID: Route = {
    pathPrefix(Segment) { id =>
      (for {
        id <- id.toLongOption.toRight { InvalidDataError }
        todoO <- todoUsecase.GetTodoByID(id).left.map {
          case usecase.DatabaseError => InternalServerError
          case usecase.NotFoundError => NotFoundError
        }
        todo <- todoO.toRight { NotFoundError }
      } yield todo.toTodoDetail) match {
        case Right(res) => complete(res)
        case Left(err)  => complete(err.toStatusCode)
      }
    }
  }

  def CreateTodo: Route = {
    post {
      entity(as[TodoCreateJson]) { todoCreate =>
        (for {
          todo <- todoCreate.toTodo
          inserted <- todoUsecase.CreateTodo(todo).left.map {
            case usecase.DatabaseError => InternalServerError
            case usecase.NotFoundError => NotFoundError
          }
        } yield inserted.toTodoDetail) match {
          case Right(todo) => complete(todo)
          case Left(err)   => complete(err.toStatusCode)
        }
      }
    }
  }

  def UpdateTodo: Route = {
    pathPrefix(Segment) { id =>
      entity(as[controller.TodoUpdateJson]) { todoUpdate =>
        (for {
          idL <- id.toLongOption match {
            case Some(idL) if id == todoUpdate.id => Right(idL)
            case _                                => Left(InvalidDataError)
          }
          changeTo = domain.Todo(
            id = idL,
            content = todoUpdate.content,
            completed = todoUpdate.completed
          )
          changed <- todoUsecase.UpdateTodo(changeTo).left.map {
            case usecase.DatabaseError => InternalServerError
            case usecase.NotFoundError => NotFoundError
          }
        } yield changed.toTodoDetail) match {
          case Right(todo) => complete(todo)
          case Left(err)   => complete(err.toStatusCode)
        }
      }
    }
  }
  def ToggleCompletedTodo: Route = {
    pathPrefix(Segment) { id =>
      (for {
        id <- id.toLongOption.toRight { InvalidDataError }
        todo <- todoUsecase.ToggleCompletedTodo(id).left.map {
          case usecase.DatabaseError => InternalServerError
          case usecase.NotFoundError => NotFoundError
        }
      } yield todo.toTodoDetail) match {
        case Right(res) => complete(res)
        case Left(err)  => complete(err.toStatusCode)
      }
    }
  }
}

object TodoControllerImpl {
  def apply(todoUsecase: TodoUsecase): TodoControllerImpl =
    new TodoControllerImpl(todoUsecase)
}

object TodoStuff {
  implicit class RichTodo(todo: domain.Todo) {
    def toTodoDetail: TodoDetailJson =
      TodoDetailJson(todo.id.toString(), todo.content, todo.completed)
  }
}

object implicits {
  implicit class RichTodoError(error: ControllerError) {
    def toStatusCode: StatusCode = error match {
      case InvalidDataError    => StatusCodes.BadRequest
      case InternalServerError => StatusCodes.InternalServerError
      case NotFoundError       => StatusCodes.NotFound
    }
  }
}
