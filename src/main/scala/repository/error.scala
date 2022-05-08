package repository

sealed trait RepositoryError
case object DatabaseError extends RepositoryError
