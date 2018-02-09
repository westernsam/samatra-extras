package com.springer.samatra.extras.mustache.viewmodel

import com.springer.samatra.extras.Logger

trait ViewModelBuilder[C] {
  def model(context: C): Either[Exception, Map[String, Any]]
}

final class AggregateViewModelBuilder[C](builders: Seq[ViewModelBuilder[_ >: C]]) extends ViewModelBuilder[C] with Logger {
  private val emptyViewModel: Either[Exception, Map[String, Any]] = Right(Map[String, Any]())

  def ++(vmb: Seq[ViewModelBuilder[_ >: C]]): AggregateViewModelBuilder[C] =
    new AggregateViewModelBuilder[C](this +: vmb)

  def +(vmb: ViewModelBuilder[_ >: C]): AggregateViewModelBuilder[C] =
    new AggregateViewModelBuilder[C](Seq(this, vmb))

  def model(context: C): Either[Exception, Map[String, Any]] =
    builders.foldLeft(emptyViewModel) {
      case (Right(viewModel), builder) =>
        builder.model(context).map { m =>

          val clashingKeys = m.keySet.intersect(viewModel.keySet)
          if (clashingKeys.nonEmpty)
            log.error(
              s"Keys clash when aggregating: $clashingKeys. The clashing entries in the individual model will be ignored.\n" +
                s"Individual model: $m\n" +
                s"Accumulated model: $viewModel"
            )

          m ++ viewModel
        }
      case (fail: Left[_, _], _) => fail
    }
}