package com.springer.samatra.extras.mustache.viewmodel

import com.springer.samatra.extras.Logger

trait ViewModelBuilder[C, E] {
  def model(context: C): Either[E, Map[String, Any]]
}

trait AggregateViewModelBuilder[C, E] extends ViewModelBuilder[C, E] {
  def ++(vmb: Seq[ViewModelBuilder[_ >: C, E]]): AggregateViewModelBuilder[C, E] =
    new ConcreteAggregateViewModelBuilder[C, E](this +: vmb)

  def +(vmb: ViewModelBuilder[_ >: C, E]): AggregateViewModelBuilder[C, E] =
    new ConcreteAggregateViewModelBuilder[C, E](Seq(this,vmb))
}


final class ConcreteAggregateViewModelBuilder[C, E](builders: Seq[ViewModelBuilder[_ >: C, E]]) extends AggregateViewModelBuilder[C, E] with Logger {
  private val emptyViewModel: Either[E, Map[String, Any]] = Right(Map[String, Any]())

  def model(context: C): Either[E, Map[String, Any]] =
    builders.foldLeft(emptyViewModel) {
      case (Right(viewModel), builder) =>
        builder.model(context).map{ m =>

          val clashingKeys = m.keySet.intersect(viewModel.keySet)
          if(clashingKeys.nonEmpty)
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