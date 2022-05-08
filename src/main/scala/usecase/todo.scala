package usecase

import domain.Todo

import scalikejdbc.DB
import repository.TodoRepository

trait TodoUsecase {
  def GetTodoByID(id: Long): Either[UsecaseError, Option[Todo]]
  def CreateTodo(todo: Todo): Either[UsecaseError, Todo]
  def UpdateTodo(todo: Todo): Either[UsecaseError, Todo]
  def ToggleCompletedTodo(todo: Long): Either[UsecaseError, Todo]
}

class TodoUsecaseImpl(db: DB, todoRepository: TodoRepository)
    extends TodoUsecase {
  def GetTodoByID(id: Long): Either[UsecaseError, Option[Todo]] = {
    db.readOnly { implicit session =>
      todoRepository.GetTodoByID(id).left.map { case repository.DatabaseError =>
        DatabaseError
      }
    }
  }

  def CreateTodo(todo: Todo): Either[UsecaseError, Todo] = {
    db.localTx { implicit session =>
      todoRepository.CreateTodo(todo).left.map {
        case repository.DatabaseError => DatabaseError
      }
    }
  }

  def UpdateTodo(todo: Todo): Either[UsecaseError, Todo] = {
    db.localTx { implicit session =>
      todoRepository.UpdateTodo(todo).left.map {
        case repository.DatabaseError => DatabaseError
      }
    }
  }

  def ToggleCompletedTodo(id: Long): Either[UsecaseError, Todo] = {
    db.localTx { implicit session =>
      for {
        todoO <- todoRepository.GetTodoByID(id).left.map {
          case repository.DatabaseError => DatabaseError
        }
        todo <- todoO.toRight { NotFoundError }
        changeTo = todo.copy(completed = !todo.completed)
        changed <- todoRepository.UpdateTodo(changeTo).left.map {
          case repository.DatabaseError => DatabaseError
        }
      } yield {
        changed
      }
    }
  }
}

object TodoUsecaseImpl {
  def apply(db: DB, todoRepository: TodoRepository): TodoUsecaseImpl =
    new TodoUsecaseImpl(db, todoRepository)
}
