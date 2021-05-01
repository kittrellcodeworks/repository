package org.kittrellcodeworks.repository.util

/**
 * Provides implicit evidence that a type is not an option.
 * Some Repository implementations require this evidence in order to derive handlers for domain classes.
 *
 * This scala "hack" can be found described
 * <a href="https://stackoverflow.com/questions/53447357/scala-generics-exclude-type">on stack overflow</a>.
 */
sealed class IsNotOpt[T]

object IsNotOpt {
  private val instance: IsNotOpt[Any] = new IsNotOpt[Any]

  // the fact that we have 2 implicit functions for type Option will fail to resolve for Options.
  implicit def isOpt1[B <: Option[_]]: IsNotOpt[B] = ??? // will never get called.
  implicit def isOpt2[B <: Option[_]]: IsNotOpt[B] = ???

  // resolves for everything else
  implicit def isNotOpt[B]: IsNotOpt[B] = instance.asInstanceOf[IsNotOpt[B]]
}
