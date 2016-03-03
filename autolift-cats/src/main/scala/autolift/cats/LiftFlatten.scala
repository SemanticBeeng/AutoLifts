package autolift.cats

import cats.{FlatMap, Functor}
import autolift.LiftFlatten

trait CatsLiftFlatten[M[_], Obj] extends LiftFlatten[M, Obj]

object CatsLiftFlatten extends LowPriorityCatsLiftFlatten{
  def apply[M[_], Obj](implicit lift: CatsLiftFlatten[M, Obj]): Aux[M, Obj, lift.Out] = lift

  implicit def base[M[_], A](implicit flatMap: FlatMap[M]): Aux[M, M[M[A]], M[A]] =
    new CatsLiftFlatten[M, M[M[A]]]{
      type Out = M[A]

      def apply(mma: M[M[A]]) = flatMap.flatMap(mma){ ma: M[A] => ma }
    }
}

trait LowPriorityCatsLiftFlatten extends LowPriorityCatsLiftFlatten1{
  implicit def recur[M[_], F[_], G](implicit functor: Functor[F], lift: LiftFlatten[M, G]): Aux[M, F[G], F[lift.Out]] =
    new CatsLiftFlatten[M, F[G]]{
      type Out = F[lift.Out]

      def apply(fg: F[G]) = functor.map(fg){ g: G => lift(g) }
    }
}

trait LowPriorityCatsLiftFlatten1{
  type Aux[M[_], Obj, Out0] = CatsLiftFlatten[M, Obj]{ type Out = Out0 }

  implicit def unrecur[M[_], FG, F[_], G](implicit unapply: Un.Aux[Functor, FG, F, G], lift: LiftFlatten[M, G]): Aux[M, FG, F[lift.Out]] =
    new CatsLiftFlatten[M, FG]{
      type Out = F[lift.Out]

      def apply(fg: FG) = unapply.TC.map(unapply.subst(fg)){ g: G => lift(g) }
    }
}

