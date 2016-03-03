package autolift.cats

import cats.{FlatMap, Functor}
import autolift.LiftFlatMap


trait CatsLiftFlatMap[Obj, Fn] extends LiftFlatMap[Obj, Fn]

object CatsLiftFlatMap extends LowPriorityCatsLiftFlatMap {
  def apply[Obj, Fn](implicit lift: CatsLiftFlatMap[Obj, Fn]): Aux[Obj, Fn, lift.Out] = lift

  implicit def base[M[_], A, C >: A, B](implicit flatMap: FlatMap[M]): Aux[M[A], C => M[B], M[B]] =
    new CatsLiftFlatMap[M[A], C => M[B]]{
      type Out = M[B]

      def apply(ma: M[A], f: C => M[B]) = flatMap.flatMap(ma)(f)
    }
}

trait LowPriorityCatsLiftFlatMap extends LowPriorityCatsLiftFlatMap1{
  implicit def unbase[MA, A, C >: A, B](implicit unapply: Un.Apply[FlatMap, MA, A]): Aux[MA, C => unapply.M[B], unapply.M[B]] =
    new CatsLiftFlatMap[MA, C => unapply.M[B]]{
      type Out = unapply.M[B]

      //Is this too coupled a type signature?
      def apply(ma: MA, f: C => unapply.M[B]) = unapply.TC.flatMap(unapply.subst(ma))(f)
    }
}

trait LowPriorityCatsLiftFlatMap1 extends LowPriorityCatsLiftFlatMap2{
  implicit def recur[F[_], G, Fn](implicit functor: Functor[F], lift: LiftFlatMap[G, Fn]): Aux[F[G], Fn, F[lift.Out]] =
    new CatsLiftFlatMap[F[G], Fn]{
      type Out = F[lift.Out]

      def apply(fg: F[G], f: Fn) = functor.map(fg){ g: G => lift(g, f) }
    }
}

trait LowPriorityCatsLiftFlatMap2{
  type Aux[Obj, Fn, Out0] = CatsLiftFlatMap[Obj, Fn]{ type Out = Out0 }

  implicit def unrecur[FG, F[_], G, Fn](implicit unapply: Un.Aux[Functor, FG, F, G], lift: LiftFlatMap[G, Fn]): Aux[FG, Fn, F[lift.Out]] =
    new CatsLiftFlatMap[FG, Fn]{
      type Out = F[lift.Out]

      def apply(fg: FG, f: Fn) = unapply.TC.map(unapply.subst(fg)){ g: G => lift(g, f) }
    }
}

final class LiftedFlatMap[A, B, M[_]](protected val f: A => M[B])(implicit flatMap: FlatMap[M]){
  def andThen[C >: B, D](that: LiftedFlatMap[C, D, M]) = new LiftedFlatMap({ x: A => flatMap.flatMap(f(x))(that.f) })

  def compose[C, D <: A](that: LiftedFlatMap[C, D, M]) = that andThen this

  def map[C](g: B => C): LiftedFlatMap[A, C, M] = new LiftedFlatMap({ x: A => flatMap.map(f(x))(g) })

  def apply[That](that: That)(implicit lift: LiftFlatMap[That, A => M[B]]): lift.Out = lift(that, f)
}

trait LiftedFlatMapImplicits{
  implicit def liftedFlatMapFunctor[A, M[_]] = new Functor[LiftedFlatMap[A, ?, M]]{
    def map[B, C](lb: LiftedFlatMap[A, B, M])(f: B => C) = lb map f
  }
}

trait LiftFlatMapContext{
  def liftFlatMap[A, B, M[_]](f: A => M[B])(implicit flatMap: FlatMap[M]) = new LiftedFlatMap(f)
}

