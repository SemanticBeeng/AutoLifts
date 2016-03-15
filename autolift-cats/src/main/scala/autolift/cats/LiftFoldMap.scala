package autolift.cats

import cats.{Functor, Monoid, Foldable}
import autolift.LiftFoldMap

trait CatsLiftFoldMap[FA, Fn] extends LiftFoldMap[FA, Fn]

object CatsLiftFoldMap extends LowPriorityCatsLiftFoldMap{
  def apply[FA, Fn](implicit lift: CatsLiftFoldMap[FA, Fn]): Aux[FA, Fn, lift.Out] = lift

  implicit def base[F[_], A, C >: A, B](implicit fold: Foldable[F], ev: Monoid[B]): Aux[F[A], C => B, B] =
    new CatsLiftFoldMap[F[A], C => B]{
      type Out = B

      def apply(fa: F[A], f: C => B) = fold.foldMap(fa)(f)
    }
}

trait LowPriorityCatsLiftFoldMap extends LowPriorityCatsLiftFoldMap1{
  implicit def unbase[FA, A, C >: A, B](implicit unapply: Un.Apply[Foldable, FA, A], ev: Monoid[B]): Aux[FA, C => B, B] =
    new CatsLiftFoldMap[FA, C => B]{
      type Out = B

      def apply(fa: FA, f: C => B) = unapply.TC.foldMap(unapply.subst(fa))(f)
    }
}

trait LowPriorityCatsLiftFoldMap1 extends LowPriorityCatsLiftFoldMap2{
  implicit def recur[F[_], G, Fn](implicit functor: Functor[F], lift: LiftFoldMap[G, Fn]): Aux[F[G], Fn, F[lift.Out]] =
    new CatsLiftFoldMap[F[G], Fn]{
      type Out = F[lift.Out]

      def apply(fg: F[G], f: Fn) = functor.map(fg){ g: G => lift(g, f) }
    }
}

trait LowPriorityCatsLiftFoldMap2{
  type Aux[FA, Fn, Out0] = CatsLiftFoldMap[FA, Fn]{ type Out = Out0 }

  implicit def unrecur[FG, G, Fn](implicit unapply: Un.Apply[Functor, FG, G], lift: LiftFoldMap[G, Fn]): Aux[FG, Fn, unapply.M[lift.Out]] =
    new CatsLiftFoldMap[FG, Fn]{
      type Out = unapply.M[lift.Out]

      def apply(fg: FG, f: Fn) = unapply.TC.map(unapply.subst(fg)){ g: G => lift(g, f) }
    }
}

final class LiftedFoldMap[A, B : Monoid](f: A => B){
  def andThen[C >: B, D : Monoid](that: LiftedFoldMap[C, D]) = that compose this

  def compose[C, D <: A](that: LiftedFoldMap[C, D]) = that map f

  def map[C : Monoid](g: B => C): LiftedFoldMap[A, C] = new LiftedFoldMap(f andThen g)

  def apply[That](that: That)(implicit lift: LiftFoldMap[That, A => B]): lift.Out = lift(that, f)
}

trait LiftFoldMapContext{
  def liftFoldMap[A, B : Monoid](f: A => B) = new LiftedFoldMap(f)
}

