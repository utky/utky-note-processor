package ilyaletre.evernote.tasks

import scala.util.Try
import java.time.{ZonedDateTime, Instant}
import java.time.temporal.ChronoUnit
import ilyaletre.evernote.tasks.GetTemplate._
import cats._
import cats.implicits._
import com.typesafe.scalalogging.Logger

object WeeklyReview {
  val logger = Logger("WeeklyReview")
  def dateFormat = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
  def daysOfWeek = 7
  def targetNotebook = "log"
  def apply[F[_]
    : GetTemplate
    : CreateNote
    : FindNotes
    : GetNote
    : Monad]
    (date: ZonedDateTime, templateTitle: String): F[Unit] = {
    val oneWeekAgo = date.minus(daysOfWeek, ChronoUnit.DAYS)
    val title = dateFormat.format(date)
    val context = Map(
      "logs" -> List[String](),
      "todo" -> List[String]()
    )
    val predicates = Seq(
      Notebook(targetNotebook),
      Created(DaysBefore(daysOfWeek))
    )
    logger.info(s"oneWeekAgo: ${oneWeekAgo}")
    logger.info(s"title: ${title}")
    for {
      postsThisWeek <- FindNotes[F].findNotes(predicates)
      previousReview <- GetNote[F].getNoteByTitle(targetNotebook, dateFormat.format(oneWeekAgo))
      template <- GetTemplate[F].getTemplate(templateTitle)
      content = template.render(context)
      result <- CreateNote[F].createNote(CreateNote.CreateReq(
        title,
        content,
        targetNotebook,
        Seq("review")
      ))
    }
    yield Try { result }
  }
}