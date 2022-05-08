package repository

import scalikejdbc._
import scalikejdbc.TxBoundary.Either
import scala.util.Try
import domain.Todo

trait TodoRepository {
  def GetTodoByID(id: Long)(implicit
      session: DBSession
  ): Either[RepositoryError, Option[Todo]]
  def CreateTodo(todo: Todo)(implicit
      session: DBSession
  ): Either[RepositoryError, Todo]
  def UpdateTodo(todo: Todo)(implicit
      session: DBSession
  ): Either[RepositoryError, Todo]
}

class TodoRepositoryImpl extends TodoRepository {
  def GetTodoByID(
      id: Long
  )(implicit session: DBSession): Either[RepositoryError, Option[Todo]] = {
    Try {
      sql"""
SELECT id,content,completed FROM todos where id = ${id}
""".map { rs =>
        domain.Todo(
          id = rs.long("id"),
          content = rs.string("content"),
          completed = rs.boolean("completed")
        )
      }.single()
        .apply()
    }.toEither.left.map { _ => DatabaseError }
  }

  def CreateTodo(todo: domain.Todo)(implicit
      session: DBSession
  ): Either[RepositoryError, Todo] = {
    Try {
      val id = sql"""
INSERT INTO todos (content,completed) VALUES (${todo.content},${todo.completed})
""".updateAndReturnGeneratedKey.apply()
      todo.copy(id = id)
    }.toEither.left.map { _ => DatabaseError }
  }

  def UpdateTodo(todo: domain.Todo)(implicit
      session: DBSession
  ): Either[RepositoryError, Todo] = {
    Try {
      sql"""
UPDATE todos SET content = ${todo.content}, completed = ${todo.completed} WHERE id = ${todo.id}
""".update.apply()
      todo
    }.toEither.left.map { _ => DatabaseError }
  }
}

object TodoRepositoryImpl {
  def apply(): TodoRepositoryImpl = new TodoRepositoryImpl()
}
